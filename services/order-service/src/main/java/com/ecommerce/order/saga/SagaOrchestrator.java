package com.ecommerce.order.saga;

import com.ecommerce.order.entity.SagaExecution;
import com.ecommerce.order.entity.SagaStatus;
import com.ecommerce.order.repository.SagaExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Saga Orchestrator
 * 
 * <p>Central coordinator for saga execution.
 * Manages the execution of saga steps and handles compensation on failure.</p>
 * 
 * <h2>Orchestrator Responsibilities:</h2>
 * <pre>
 * 1. Execute Steps:
 *    - Execute steps in order
 *    - Persist state after each step
 *    - Enable recovery
 * 
 * 2. Handle Failures:
 *    - Catch exceptions
 *    - Decide retry or compensate
 *    - Execute compensation in reverse order
 * 
 * 3. Manage State:
 *    - Create SagaExecution record
 *    - Update after each step
 *    - Mark as COMPLETED or COMPENSATED
 * 
 * 4. Enable Recovery:
 *    - Load incomplete sagas on startup
 *    - Resume from last completed step
 *    - Continue or compensate
 * </pre>
 * 
 * <h2>Execution Flow:</h2>
 * <pre>
 * public void execute(Saga saga, Context context) {
 *     
 *     1. Create SagaExecution (STARTED)
 *     2. Save to database
 *     
 *     3. For each step:
 *        a. Execute step
 *        b. If success:
 *           - currentStep++
 *           - Save state
 *           - Continue
 *        c. If failure:
 *           - Start compensation
 *           - Break loop
 *     
 *     4. If all steps succeeded:
 *        - Mark COMPLETED
 *        - Return success
 *     
 *     5. If any step failed:
 *        - Execute compensations (reverse order)
 *        - Mark COMPENSATED
 *        - Return failure
 * }
 * </pre>
 * 
 * <h2>Compensation Flow:</h2>
 * <pre>
 * Steps: [1, 2, 3, 4]
 * Completed: [1, 2]
 * Failed: 3
 * 
 * Compensation Order:
 * - Step 2: compensate()
 * - Step 1: compensate()
 * 
 * Note: Don't compensate Step 3 (it didn't complete)
 * 
 * Compensation Loop:
 * for (i = currentStep - 1; i >= 0; i--) {
 *     step = getStep(i);
 *     step.compensate(context);
 * }
 * </pre>
 * 
 * <h2>Retry Strategy:</h2>
 * <pre>
 * Transient Failures (Retry):
 * - Network timeout
 * - Database connection lost
 * - Service temporarily unavailable
 * → Retry with exponential backoff
 * 
 * Business Failures (Don't Retry):
 * - Insufficient inventory
 * - Payment declined
 * - Invalid product
 * → Compensate immediately
 * 
 * Retry Logic:
 * retries = 0
 * while (retries < maxRetries) {
 *     try {
 *         step.execute(context);
 *         break; // Success
 *     } catch (RetriableException e) {
 *         retries++;
 *         sleep(backoff * retries);
 *     } catch (BusinessException e) {
 *         startCompensation();
 *         break;
 *     }
 * }
 * </pre>
 * 
 * <h2>Recovery Process:</h2>
 * <pre>
 * On Application Startup:
 * 
 * 1. Load incomplete sagas:
 *    SELECT * FROM saga_executions 
 *    WHERE status IN ('IN_PROGRESS', 'COMPENSATING')
 * 
 * 2. For each saga:
 *    a. Load saga definition
 *    b. Check last completed step
 *    c. Resume execution:
 *       - If IN_PROGRESS: Continue forward
 *       - If COMPENSATING: Continue compensation
 * 
 * 3. Example:
 *    Saga crashed at Step 3/4
 *    Database shows: currentStep = 2 (Steps 1, 2 completed)
 *    Recovery: Execute Step 3, then Step 4
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component  // Spring bean
public class SagaOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(SagaOrchestrator.class);

    // Repository for persisting saga state
    private final SagaExecutionRepository sagaRepository;

    // Configuration
    @Value("${app.saga.retry.max-attempts:5}")
    private int maxRetries;

    @Value("${app.saga.retry.initial-backoff:1000}")
    private long initialBackoff;

    @Value("${app.saga.retry.multiplier:2}")
    private int backoffMultiplier;

    @Autowired
    public SagaOrchestrator(SagaExecutionRepository sagaRepository) {
        this.sagaRepository = sagaRepository;
    }

    /**
     * Execute Saga
     * 
     * <p>Orchestrates the execution of all saga steps.
     * Handles failures and triggers compensation if needed.</p>
     * 
     * @param saga Saga to execute
     * @param context Saga context
     * @param orderId Order ID being processed
     * @param <T> Context type
     * @return Saga execution result
     */
    @Transactional
    public <T> SagaExecution execute(Saga<T> saga, T context, String orderId) {
        // Generate saga ID
        String sagaId = UUID.randomUUID().toString();
        
        logger.info("Starting saga: {} for order: {}", saga.getSagaName(), orderId);
        
        // Create saga execution record
        SagaExecution execution = SagaExecution.builder()
            .sagaId(sagaId)
            .sagaType(saga.getSagaName())
            .status(SagaStatus.STARTED)
            .currentStep(0)
            .totalSteps(saga.getTotalSteps())
            .orderId(orderId)
            .retryCount(0)
            .build();
        
        // Persist initial state
        execution = sagaRepository.save(execution);
        
        // Update status to IN_PROGRESS
        execution.setStatus(SagaStatus.IN_PROGRESS);
        execution = sagaRepository.save(execution);
        
        try {
            // Execute all steps
            List<SagaStep<T>> steps = saga.getSteps();
            
            for (int i = 0; i < steps.size(); i++) {
                SagaStep<T> step = steps.get(i);
                
                logger.info("Executing step {}/{}: {} for saga: {}", 
                    i + 1, steps.size(), step.getStepName(), sagaId);
                
                try {
                    // Execute step with retry
                    executeStepWithRetry(step, context, execution);
                    
                    // Step succeeded - update state
                    execution.nextStep();
                    execution = sagaRepository.save(execution);
                    
                    logger.info("Step {} completed successfully: {}", 
                        i + 1, step.getStepName());
                    
                } catch (Exception e) {
                    // Step failed - start compensation
                    logger.error("Step {} failed: {}. Starting compensation.", 
                        i + 1, step.getStepName(), e);
                    
                    execution.setErrorMessage(e.getMessage());
                    execution.setStatus(SagaStatus.COMPENSATING);
                    execution = sagaRepository.save(execution);
                    
                    // Compensate all previously completed steps
                    compensate(saga, context, execution);
                    
                    return execution;
                }
            }
            
            // All steps succeeded
            execution.markCompleted();
            execution = sagaRepository.save(execution);
            
            logger.info("Saga completed successfully: {}", sagaId);
            return execution;
            
        } catch (Exception e) {
            // Unexpected error
            logger.error("Saga failed with unexpected error: {}", sagaId, e);
            execution.markFailed(e.getMessage());
            return sagaRepository.save(execution);
        }
    }

    /**
     * Compensate Saga
     * 
     * <p>Executes compensation for all completed steps in reverse order.</p>
     * 
     * @param saga Saga being compensated
     * @param context Saga context
     * @param execution Saga execution state
     * @param <T> Context type
     */
    private <T> void compensate(Saga<T> saga, T context, SagaExecution execution) {
        logger.info("Starting compensation for saga: {}", execution.getSagaId());
        
        // Compensate in reverse order (don't compensate the failed step)
        int lastCompletedStep = execution.getCurrentStep() - 1;
        
        for (int i = lastCompletedStep; i >= 0; i--) {
            SagaStep<T> step = saga.getStep(i);
            
            logger.info("Compensating step {}: {}", i + 1, step.getStepName());
            
            try {
                // Compensate with retry
                compensateStepWithRetry(step, context, execution);
                
                // Update state
                execution.previousStep();
                sagaRepository.save(execution);
                
                logger.info("Step {} compensated successfully: {}", 
                    i + 1, step.getStepName());
                
            } catch (Exception e) {
                // Compensation failed - critical error
                logger.error("Compensation failed for step {}: {}. Manual intervention required!", 
                    i + 1, step.getStepName(), e);
                
                execution.markFailed("Compensation failed: " + e.getMessage());
                sagaRepository.save(execution);
                
                // TODO: Alert operations team
                // alertService.sendCriticalAlert(execution);
                
                return;
            }
        }
        
        // All compensations succeeded
        execution.markCompensated();
        sagaRepository.save(execution);
        
        logger.info("Saga compensated successfully: {}", execution.getSagaId());
    }

    /**
     * Execute Step with Retry
     * 
     * <p>Executes a step with exponential backoff retry.</p>
     * 
     * @param step Step to execute
     * @param context Saga context
     * @param execution Saga execution state
     * @param <T> Context type
     * @throws Exception if all retries fail
     */
    private <T> void executeStepWithRetry(
            SagaStep<T> step, 
            T context, 
            SagaExecution execution) throws Exception {
        
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                // Attempt execution
                step.execute(context);
                return; // Success
                
            } catch (Exception e) {
                lastException = e;
                attempts++;
                execution.incrementRetry();
                sagaRepository.save(execution);
                
                if (attempts < maxRetries) {
                    // Calculate backoff
                    long backoff = initialBackoff * (long) Math.pow(backoffMultiplier, attempts - 1);
                    
                    logger.warn("Step {} failed (attempt {}/{}). Retrying in {}ms...", 
                        step.getStepName(), attempts, maxRetries, backoff);
                    
                    // Wait before retry
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    logger.error("Step {} failed after {} attempts", 
                        step.getStepName(), maxRetries);
                }
            }
        }
        
        // All retries exhausted
        throw lastException;
    }

    /**
     * Compensate Step with Retry
     * 
     * <p>Compensates a step with retry logic.
     * Compensation failures are critical!</p>
     * 
     * @param step Step to compensate
     * @param context Saga context
     * @param execution Saga execution state
     * @param <T> Context type
     * @throws Exception if all retries fail
     */
    private <T> void compensateStepWithRetry(
            SagaStep<T> step, 
            T context, 
            SagaExecution execution) throws Exception {
        
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                step.compensate(context);
                return; // Success
                
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                if (attempts < maxRetries) {
                    long backoff = initialBackoff * (long) Math.pow(backoffMultiplier, attempts - 1);
                    
                    logger.warn("Compensation {} failed (attempt {}/{}). Retrying in {}ms...", 
                        step.getStepName(), attempts, maxRetries, backoff);
                    
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Compensation retry interrupted", ie);
                    }
                } else {
                    logger.error("Compensation {} failed after {} attempts. CRITICAL!", 
                        step.getStepName(), maxRetries);
                }
            }
        }
        
        throw lastException;
    }

    /**
     * Resume Saga
     * 
     * <p>Resumes saga execution from last completed step.
     * Used for recovery after application restart.</p>
     * 
     * @param saga Saga to resume
     * @param context Saga context
     * @param execution Existing saga execution
     * @param <T> Context type
     * @return Updated saga execution
     */
    @Transactional
    public <T> SagaExecution resume(Saga<T> saga, T context, SagaExecution execution) {
        logger.info("Resuming saga: {} from step {}", 
            execution.getSagaId(), execution.getCurrentStep());
        
        if (execution.getStatus() == SagaStatus.IN_PROGRESS) {
            // Resume forward execution
            return resumeForward(saga, context, execution);
        } else if (execution.getStatus() == SagaStatus.COMPENSATING) {
            // Resume compensation
            compensate(saga, context, execution);
            return execution;
        } else {
            logger.warn("Cannot resume saga in status: {}", execution.getStatus());
            return execution;
        }
    }

    /**
     * Resume Forward Execution
     * 
     * @param saga Saga to resume
     * @param context Saga context
     * @param execution Saga execution state
     * @param <T> Context type
     * @return Updated saga execution
     */
    private <T> SagaExecution resumeForward(Saga<T> saga, T context, SagaExecution execution) {
        List<SagaStep<T>> steps = saga.getSteps();
        int startStep = execution.getCurrentStep();
        
        for (int i = startStep; i < steps.size(); i++) {
            SagaStep<T> step = steps.get(i);
            
            try {
                executeStepWithRetry(step, context, execution);
                execution.nextStep();
                execution = sagaRepository.save(execution);
                
            } catch (Exception e) {
                logger.error("Step {} failed during resume. Starting compensation.", 
                    i + 1, e);
                
                execution.setStatus(SagaStatus.COMPENSATING);
                execution = sagaRepository.save(execution);
                compensate(saga, context, execution);
                return execution;
            }
        }
        
        execution.markCompleted();
        return sagaRepository.save(execution);
    }
}

