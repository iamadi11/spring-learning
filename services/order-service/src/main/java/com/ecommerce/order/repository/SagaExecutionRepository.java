package com.ecommerce.order.repository;

import com.ecommerce.order.entity.SagaExecution;
import com.ecommerce.order.entity.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga Execution Repository
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface SagaExecutionRepository extends JpaRepository<SagaExecution, String> {

    /**
     * Find by order ID
     */
    List<SagaExecution> findByOrderId(String orderId);

    /**
     * Find by status
     */
    List<SagaExecution> findByStatus(SagaStatus status);

    /**
     * Find incomplete sagas (for recovery)
     */
    @Query("SELECT s FROM SagaExecution s WHERE s.status IN ('IN_PROGRESS', 'COMPENSATING')")
    List<SagaExecution> findIncompleteSagas();

    /**
     * Find long-running sagas (potential stuck sagas)
     */
    @Query("SELECT s FROM SagaExecution s WHERE s.status = 'IN_PROGRESS' AND s.createdAt < ?1")
    List<SagaExecution> findLongRunningSagas(LocalDateTime threshold);

    /**
     * Find failed sagas requiring manual intervention
     */
    List<SagaExecution> findByStatusOrderByCreatedAtDesc(SagaStatus status);
}

