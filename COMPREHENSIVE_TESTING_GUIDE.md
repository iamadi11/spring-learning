# Comprehensive Testing Guide

## Overview

This guide covers **complete testing strategies** for microservices, from unit tests to production load tests. Learn how to test like companies such as Google, Netflix, and Amazon to achieve 99.99% reliability.

## 🎯 Testing Pyramid

```
        /\
       /  \       E2E Tests
      /----\      (Slow, Expensive, Few)
     /      \
    /--------\    Integration Tests
   /          \   (Medium Speed, Medium Cost, Some)
  /------------\
 /______________\ Unit Tests
                  (Fast, Cheap, Many)
```

**Principle**: More unit tests at the base, fewer E2E tests at the top.

**Rationale**:
- **Unit Tests**: Fast (milliseconds), cheap to maintain, many tests (70%)
- **Integration Tests**: Medium speed (seconds), moderate cost, some tests (20%)
- **E2E Tests**: Slow (minutes), expensive to maintain, few tests (10%)

---

## 1. Unit Tests 🧪

### What are Unit Tests?

**Definition**: Test a single unit (method/class) in isolation.

**Characteristics**:
- ✅ Fast (milliseconds)
- ✅ No external dependencies (DB, network, etc.)
- ✅ Use mocks/stubs
- ✅ Test one thing at a time

### Tools

- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework
- **AssertJ**: Fluent assertions
- **MockMvc**: Spring MVC testing

### Example 1: Service Layer Test

```java
/**
 * Unit Test for ProductService
 * Tests business logic in isolation
 */
@ExtendWith(MockitoExtension.class)  // Use Mockito for mocking
class ProductServiceTest {
    
    // Mock dependencies
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private EventStoreRepository eventStoreRepository;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    // Class under test (inject mocks)
    @InjectMocks
    private ProductCommandService productService;
    
    /**
     * Test: Creating a product should save event and projection
     */
    @Test
    @DisplayName("Should create product and save to event store")
    void testCreateProduct() {
        // Given (Arrange)
        ProductRequest request = ProductRequest.builder()
            .name("iPhone 15")
            .description("Latest iPhone")
            .price(999.99)
            .category("Electronics")
            .stock(100)
            .build();
        
        Product expectedProduct = Product.builder()
            .id("prod-123")
            .name("iPhone 15")
            .price(999.99)
            .build();
        
        // Mock repository behavior
        when(productRepository.save(any(Product.class)))
            .thenReturn(expectedProduct);
        
        // When (Act)
        Product result = productService.createProduct(request);
        
        // Then (Assert)
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("iPhone 15");
        assertThat(result.getPrice()).isEqualTo(999.99);
        
        // Verify interactions
        verify(eventStoreRepository, times(1))
            .save(any(ProductCreatedEvent.class));
        verify(productRepository, times(1))
            .save(any(Product.class));
    }
    
    /**
     * Test: Getting non-existent product should throw exception
     */
    @Test
    @DisplayName("Should throw exception when product not found")
    void testGetProductNotFound() {
        // Given
        String productId = "non-existent";
        when(productRepository.findById(productId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> productService.getProduct(productId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Product not found: non-existent");
        
        verify(productRepository, times(1)).findById(productId);
    }
    
    /**
     * Test: Updating product price should create PriceChangedEvent
     */
    @Test
    @DisplayName("Should update product price and create event")
    void testUpdateProductPrice() {
        // Given
        String productId = "prod-123";
        double oldPrice = 999.99;
        double newPrice = 899.99;
        
        Product existingProduct = Product.builder()
            .id(productId)
            .name("iPhone 15")
            .price(oldPrice)
            .version(1L)
            .build();
        
        when(productRepository.findById(productId))
            .thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Product result = productService.updatePrice(productId, newPrice);
        
        // Then
        assertThat(result.getPrice()).isEqualTo(newPrice);
        assertThat(result.getVersion()).isEqualTo(2L);
        
        // Verify PriceChangedEvent was saved
        verify(eventStoreRepository).save(argThat(event -> 
            event instanceof PriceChangedEvent &&
            ((PriceChangedEvent) event).getOldPrice() == oldPrice &&
            ((PriceChangedEvent) event).getNewPrice() == newPrice
        ));
    }
    
    /**
     * Test: Parametrized test for multiple scenarios
     */
    @ParameterizedTest
    @DisplayName("Should validate product prices")
    @CsvSource({
        "0.01, true",      // Minimum valid price
        "0.00, false",     // Zero is invalid
        "-10.00, false",   // Negative is invalid
        "999999.99, true", // Maximum valid price
        "1000000.00, false" // Over maximum
    })
    void testValidatePrice(double price, boolean expected) {
        boolean result = productService.isValidPrice(price);
        assertThat(result).isEqualTo(expected);
    }
}
```

### Example 2: Controller Layer Test

```java
/**
 * Unit Test for ProductController
 * Tests REST endpoints in isolation using MockMvc
 */
@WebMvcTest(ProductController.class)  // Only load ProductController
class ProductControllerTest {
    
    @Autowired
    private MockMvc mockMvc;  // Simulates HTTP requests
    
    @MockBean  // Mock the service
    private ProductCommandService productCommandService;
    
    @MockBean
    private ProductQueryService productQueryService;
    
    @Autowired
    private ObjectMapper objectMapper;  // JSON serialization
    
    /**
     * Test: GET /api/v1/products/{id} should return product
     */
    @Test
    @DisplayName("GET /api/v1/products/{id} - Success")
    void testGetProduct() throws Exception {
        // Given
        String productId = "prod-123";
        ProductResponse expectedResponse = ProductResponse.builder()
            .id(productId)
            .name("iPhone 15")
            .price(999.99)
            .build();
        
        when(productQueryService.getProduct(productId))
            .thenReturn(expectedResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/products/{id}", productId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(productId))
            .andExpect(jsonPath("$.name").value("iPhone 15"))
            .andExpect(jsonPath("$.price").value(999.99))
            .andDo(print());  // Print request/response for debugging
        
        verify(productQueryService, times(1)).getProduct(productId);
    }
    
    /**
     * Test: GET /api/v1/products/{id} - Not Found
     */
    @Test
    @DisplayName("GET /api/v1/products/{id} - Not Found")
    void testGetProductNotFound() throws Exception {
        // Given
        String productId = "non-existent";
        when(productQueryService.getProduct(productId))
            .thenThrow(new ResourceNotFoundException("Product not found"));
        
        // When & Then
        mockMvc.perform(get("/api/v1/products/{id}", productId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Product not found"));
    }
    
    /**
     * Test: POST /api/v1/products should create product
     */
    @Test
    @DisplayName("POST /api/v1/products - Create Product")
    void testCreateProduct() throws Exception {
        // Given
        ProductRequest request = ProductRequest.builder()
            .name("iPhone 15")
            .description("Latest iPhone")
            .price(999.99)
            .category("Electronics")
            .stock(100)
            .build();
        
        ProductResponse response = ProductResponse.builder()
            .id("prod-123")
            .name("iPhone 15")
            .price(999.99)
            .build();
        
        when(productCommandService.createProduct(any(ProductRequest.class)))
            .thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("prod-123"))
            .andExpect(jsonPath("$.name").value("iPhone 15"));
    }
    
    /**
     * Test: POST /api/v1/products - Validation Error
     */
    @Test
    @DisplayName("POST /api/v1/products - Validation Error")
    void testCreateProductValidationError() throws Exception {
        // Given - Invalid request (empty name)
        ProductRequest request = ProductRequest.builder()
            .name("")  // Invalid: empty name
            .price(-10.0)  // Invalid: negative price
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }
}
```

### Example 3: Repository Layer Test

```java
/**
 * Unit Test for JPA Repository
 * Tests custom query methods
 */
@DataJpaTest  // Load only JPA components
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers  // Use real database in Docker
class ProductRepositoryTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @BeforeEach
    void setUp() {
        // Clean database before each test
        productRepository.deleteAll();
    }
    
    /**
     * Test: Find products by category
     */
    @Test
    @DisplayName("Should find products by category")
    void testFindByCategory() {
        // Given
        Product phone1 = createProduct("iPhone 15", "Electronics");
        Product phone2 = createProduct("Samsung S24", "Electronics");
        Product laptop = createProduct("MacBook Pro", "Computers");
        
        entityManager.persist(phone1);
        entityManager.persist(phone2);
        entityManager.persist(laptop);
        entityManager.flush();
        
        // When
        List<Product> electronics = productRepository.findByCategory("Electronics");
        
        // Then
        assertThat(electronics).hasSize(2);
        assertThat(electronics)
            .extracting(Product::getName)
            .containsExactlyInAnyOrder("iPhone 15", "Samsung S24");
    }
    
    /**
     * Test: Find products in price range
     */
    @Test
    @DisplayName("Should find products in price range")
    void testFindByPriceRange() {
        // Given
        Product cheap = createProduct("Phone Case", 10.0);
        Product mid = createProduct("Headphones", 100.0);
        Product expensive = createProduct("Laptop", 2000.0);
        
        productRepository.saveAll(List.of(cheap, mid, expensive));
        
        // When
        List<Product> midRange = productRepository
            .findByPriceBetween(50.0, 500.0);
        
        // Then
        assertThat(midRange).hasSize(1);
        assertThat(midRange.get(0).getName()).isEqualTo("Headphones");
    }
    
    private Product createProduct(String name, String category) {
        return Product.builder()
            .name(name)
            .category(category)
            .price(999.99)
            .build();
    }
    
    private Product createProduct(String name, double price) {
        return Product.builder()
            .name(name)
            .price(price)
            .category("Electronics")
            .build();
    }
}
```

### Best Practices

1. **AAA Pattern**: Arrange, Act, Assert
2. **One Assert Per Test**: Focus on single behavior
3. **Descriptive Names**: `testCreateProduct_WhenValidInput_ShouldReturnProduct`
4. **Use AssertJ**: Fluent, readable assertions
5. **Mock External Dependencies**: Database, HTTP, etc.
6. **Test Edge Cases**: Null, empty, boundary values

---

## 2. Integration Tests 🔌

### What are Integration Tests?

**Definition**: Test multiple components working together with real dependencies.

**Characteristics**:
- ✅ Medium speed (seconds)
- ✅ Use real databases (via Testcontainers)
- ✅ Test actual integration points
- ✅ More confidence than unit tests

### Tools

- **Spring Boot Test**: Full application context
- **Testcontainers**: Real databases in Docker
- **WireMock**: Mock external APIs
- **Rest Assured**: API testing

### Example 1: Full Application Integration Test

```java
/**
 * Integration Test for Order Service
 * Tests complete order creation flow with real database
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderServiceIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    // Real PostgreSQL in Docker
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    
    // Real Kafka in Docker
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    // Mock external service (Product Service)
    @WireMockTest(httpPort = 8081)
    static WireMockServer wireMockServer;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure test database
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Configure test Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        
        // Configure Product Service URL
        registry.add("product.service.url", () -> "http://localhost:8081");
    }
    
    @BeforeEach
    void setUp() {
        // Clean database
        orderRepository.deleteAll();
        
        // Mock Product Service response
        stubFor(post(urlEqualTo("/api/products/reserve"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\": true}")));
    }
    
    /**
     * Test: End-to-end order creation
     */
    @Test
    @DisplayName("Should create order with all integrations")
    void testCreateOrder() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
            .userId(789L)
            .items(List.of(
                OrderItemRequest.builder()
                    .productId("prod-123")
                    .quantity(2)
                    .price(999.99)
                    .build()
            ))
            .shippingAddress("123 Main St")
            .build();
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            request,
            OrderResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo(789L);
        assertThat(response.getBody().getStatus()).isEqualTo("PENDING");
        
        // Verify database
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        
        // Verify Product Service was called
        verify(postRequestedFor(urlEqualTo("/api/products/reserve")));
    }
    
    /**
     * Test: Order creation fails when product service is down
     */
    @Test
    @DisplayName("Should handle product service failure")
    void testProductServiceFailure() {
        // Given
        stubFor(post(urlEqualTo("/api/products/reserve"))
            .willReturn(aResponse().withStatus(500)));
        
        CreateOrderRequest request = CreateOrderRequest.builder()
            .userId(789L)
            .items(List.of(OrderItemRequest.builder()
                .productId("prod-123")
                .quantity(2)
                .build()))
            .build();
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            request,
            OrderResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        // Verify order was rolled back
        assertThat(orderRepository.findAll()).isEmpty();
    }
}
```

### Example 2: Database Integration Test

```java
/**
 * Integration Test for CQRS pattern
 * Tests read/write database separation
 */
@SpringBootTest
@Testcontainers
class UserServiceCQRSIntegrationTest {
    
    @Autowired
    private UserProfileCommandService commandService;
    
    @Autowired
    private UserProfileQueryService queryService;
    
    @Autowired
    private UserProfileCommandRepository commandRepository;
    
    @Autowired
    private UserProfileQueryRepository queryRepository;
    
    // Primary database (writes)
    @Container
    static PostgreSQLContainer<?> primaryDb = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("primary");
    
    // Replica database (reads)
    @Container
    static PostgreSQLContainer<?> replicaDb = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("replica");
    
    /**
     * Test: Write to primary, read from replica
     */
    @Test
    @DisplayName("Should write to primary and read from replica")
    void testCQRSSeparation() {
        // Given
        UserProfileRequest request = UserProfileRequest.builder()
            .userId(789L)
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .build();
        
        // When - Write (goes to primary)
        UserProfileResponse created = commandService.createProfile(request);
        
        // Simulate replication delay
        await().atMost(Duration.ofSeconds(2))
            .until(() -> queryService.getProfile(created.getUserId()) != null);
        
        // Then - Read (comes from replica)
        UserProfileResponse retrieved = queryService.getProfile(created.getUserId());
        
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getFirstName()).isEqualTo("John");
    }
}
```

### Example 3: Kafka Integration Test

```java
/**
 * Integration Test for Kafka event publishing
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(topics = {"order-events"})
class KafkaIntegrationTest {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private OrderEventConsumer consumer;
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    /**
     * Test: Publishing and consuming events
     */
    @Test
    @DisplayName("Should publish and consume Kafka events")
    void testKafkaIntegration() throws Exception {
        // Given
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId("ord-123")
            .userId(789L)
            .totalAmount(999.99)
            .build();
        
        // When
        kafkaTemplate.send("order-events", event).get();
        
        // Then - Wait for consumer to process
        await().atMost(Duration.ofSeconds(5))
            .until(() -> consumer.getProcessedEvents().contains("ord-123"));
        
        assertThat(consumer.getProcessedEvents()).contains("ord-123");
    }
}
```

---

## 3. Contract Tests 📝

### What are Contract Tests?

**Definition**: Verify that service provider meets consumer's expectations.

**Problem**: Microservices break when APIs change unexpectedly.

**Solution**: Define contracts (API specifications) and verify both sides.

### Tools

- **Spring Cloud Contract**: Consumer-driven contracts
- **Pact**: Contract testing framework

### Example: Spring Cloud Contract

**Step 1: Define Contract (Provider Side)**

```groovy
// contracts/shouldReturnProduct.groovy
Contract.make {
    description "Should return product by ID"
    
    request {
        method GET()
        url('/api/v1/products/123')
        headers {
            contentType(applicationJson())
        }
    }
    
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: '123',
            name: 'iPhone 15',
            price: 999.99,
            category: 'Electronics'
        ])
    }
}
```

**Step 2: Generate Tests (Provider Side)**

```java
/**
 * Auto-generated test from contract
 * Verifies provider implements contract correctly
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
class ContractVerifierTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ProductService productService;
    
    @Test
    void validate_shouldReturnProduct() throws Exception {
        // Given
        Product product = Product.builder()
            .id("123")
            .name("iPhone 15")
            .price(999.99)
            .category("Electronics")
            .build();
        
        when(productService.getProduct("123")).thenReturn(product);
        
        // When & Then
        mockMvc.perform(get("/api/v1/products/123")
                .header("Content-Type", "application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("123"))
            .andExpect(jsonPath("$.name").value("iPhone 15"))
            .andExpect(jsonPath("$.price").value(999.99));
    }
}
```

**Step 3: Use Stub (Consumer Side)**

```java
/**
 * Consumer test using provider's stub
 */
@SpringBootTest
@AutoConfigureStubRunner(
    ids = "com.ecommerce:product-service:+:stubs:8081",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class OrderServiceContractTest {
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Test
    @DisplayName("Should call product service according to contract")
    void testProductServiceContract() {
        // When - Call real HTTP client (hits WireMock stub)
        ProductResponse product = productServiceClient.getProduct("123");
        
        // Then - Verify response matches contract
        assertThat(product.getId()).isEqualTo("123");
        assertThat(product.getName()).isEqualTo("iPhone 15");
        assertThat(product.getPrice()).isEqualTo(999.99);
    }
}
```

### Benefits

- ✅ **Early Detection**: Catch breaking changes before production
- ✅ **Independent Development**: Services can develop independently
- ✅ **Documentation**: Contracts serve as API documentation
- ✅ **Confidence**: Both sides verified

---

## 4. End-to-End (E2E) Tests 🌐

### What are E2E Tests?

**Definition**: Test complete user workflows across all services.

**Characteristics**:
- ✅ Tests real user scenarios
- ✅ All services running
- ✅ Real databases
- ✅ Slow but high confidence

### Tools

- **Selenium**: Browser automation
- **Cucumber**: BDD framework
- **Rest Assured**: API testing

### Example: E2E Order Flow

```java
/**
 * End-to-End Test for complete order flow
 */
@SpringBootTest
@Testcontainers
class OrderFlowE2ETest {
    
    @Container
    static DockerComposeContainer<?> environment = 
        new DockerComposeContainer<>(new File("docker-compose.yml"))
            .withExposedService("api-gateway", 8080)
            .withExposedService("order-service", 8089)
            .withExposedService("payment-service", 8091)
            .withExposedService("notification-service", 8092);
    
    private String apiGatewayUrl;
    
    @BeforeEach
    void setUp() {
        String host = environment.getServiceHost("api-gateway", 8080);
        Integer port = environment.getServicePort("api-gateway", 8080);
        apiGatewayUrl = "http://" + host + ":" + port;
    }
    
    /**
     * Test: Complete order flow from user registration to order completion
     */
    @Test
    @DisplayName("Complete order flow E2E test")
    void testCompleteOrderFlow() {
        // Step 1: Register user
        String authToken = given()
            .contentType(ContentType.JSON)
            .body(new RegisterRequest("john@example.com", "password123"))
        .when()
            .post(apiGatewayUrl + "/api/auth/register")
        .then()
            .statusCode(201)
            .extract().path("accessToken");
        
        // Step 2: Create order
        String orderId = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(new CreateOrderRequest(
                List.of(new OrderItem("prod-123", 2, 999.99)),
                "123 Main St"
            ))
        .when()
            .post(apiGatewayUrl + "/api/orders")
        .then()
            .statusCode(201)
            .extract().path("orderId");
        
        // Step 3: Wait for async processing
        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> getOrderStatus(orderId, authToken)
                .equals("COMPLETED"));
        
        // Step 4: Verify order completed
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get(apiGatewayUrl + "/api/orders/" + orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETED"))
            .body("totalAmount", equalTo(1999.98f));
        
        // Step 5: Verify notification sent
        // Check notification service logs or database
        assertThat(wasNotificationSent(orderId)).isTrue();
    }
    
    private String getOrderStatus(String orderId, String token) {
        try {
            return given()
                .header("Authorization", "Bearer " + token)
            .when()
                .get(apiGatewayUrl + "/api/orders/" + orderId)
            .then()
                .extract().path("status");
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
```

### Example: BDD with Cucumber

**Feature File**:
```gherkin
# features/order.feature
Feature: Order Management
  As a customer
  I want to place orders
  So that I can buy products

  Scenario: Successful order placement
    Given I am logged in as "john@example.com"
    And product "iPhone 15" is in stock
    When I place an order for 2 "iPhone 15"
    Then the order should be created
    And I should receive confirmation email
    And my account balance should be debited
    
  Scenario: Order fails when product out of stock
    Given I am logged in as "john@example.com"
    And product "iPhone 15" is out of stock
    When I place an order for 1 "iPhone 15"
    Then the order should fail
    And I should see error message "Product out of stock"
```

**Step Definitions**:
```java
@SpringBootTest
public class OrderStepDefinitions {
    
    private String authToken;
    private String orderId;
    private Response response;
    
    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String email) {
        authToken = loginUser(email, "password123");
    }
    
    @Given("product {string} is in stock")
    public void productIsInStock(String productName) {
        // Mock or ensure product has stock
        ensureProductStock(productName, 100);
    }
    
    @When("I place an order for {int} {string}")
    public void iPlaceAnOrderFor(int quantity, String productName) {
        String productId = getProductId(productName);
        
        response = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(createOrderRequest(productId, quantity))
        .when()
            .post("/api/orders");
        
        if (response.statusCode() == 201) {
            orderId = response.path("orderId");
        }
    }
    
    @Then("the order should be created")
    public void theOrderShouldBeCreated() {
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(orderId).isNotNull();
    }
    
    @Then("I should receive confirmation email")
    public void iShouldReceiveConfirmationEmail() {
        // Verify email was sent
        await().atMost(Duration.ofSeconds(5))
            .until(() -> emailWasSent(orderId));
    }
}
```

---

## 5. Load/Performance Tests ⚡

### What are Load Tests?

**Definition**: Test system behavior under expected and peak load.

**Goals**:
- Find performance bottlenecks
- Verify scalability
- Measure response times
- Test resource usage

### Tools

- **Gatling**: Scala-based load testing
- **JMeter**: Java-based load testing
- **k6**: JavaScript load testing

### Example: Gatling Load Test

```scala
/**
 * Gatling Load Test for Order Service
 * Simulates 1000 users creating orders over 5 minutes
 */
class OrderServiceLoadTest extends Simulation {
  
  // HTTP configuration
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
  
  // Scenario: Create orders
  val createOrderScenario = scenario("Create Orders")
    // Login
    .exec(http("Login")
      .post("/api/auth/login")
      .body(StringBody("""{"email":"user@example.com","password":"password123"}"""))
      .check(status.is(200))
      .check(jsonPath("$.accessToken").saveAs("token")))
    
    // Create order
    .exec(http("Create Order")
      .post("/api/orders")
      .header("Authorization", "Bearer ${token}")
      .body(StringBody("""
        {
          "userId": 789,
          "items": [
            {"productId": "prod-123", "quantity": 2, "price": 999.99}
          ],
          "shippingAddress": "123 Main St"
        }
      """))
      .check(status.is(201))
      .check(jsonPath("$.orderId").saveAs("orderId")))
    
    // Get order status
    .exec(http("Get Order")
      .get("/api/orders/${orderId}")
      .header("Authorization", "Bearer ${token}")
      .check(status.is(200)))
  
  // Load profile: Ramp up to 1000 users over 5 minutes
  setUp(
    createOrderScenario.inject(
      rampUsers(1000) during (5 minutes)
    ).protocols(httpProtocol)
  ).assertions(
    global.responseTime.max.lt(5000),      // Max response time < 5s
    global.responseTime.mean.lt(1000),     // Average < 1s
    global.successfulRequests.percent.gt(95) // 95% success rate
  )
}
```

### Example: JMeter Test Plan

```xml
<!-- JMeter Test Plan -->
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.comments">Order Service Load Test</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
    </TestPlan>
    
    <hashTree>
      <!-- Thread Group: 1000 users, ramp up 60s -->
      <ThreadGroup>
        <stringProp name="ThreadGroup.num_threads">1000</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
      </ThreadGroup>
      
      <hashTree>
        <!-- HTTP Request: Create Order -->
        <HTTPSamplerProxy>
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.path">/api/orders</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
        </HTTPSamplerProxy>
        
        <!-- Assertions -->
        <ResponseAssertion>
          <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
          <stringProp name="Assertion.test_strings">201</stringProp>
        </ResponseAssertion>
        
        <!-- Listeners: Results -->
        <ResultCollector>
          <stringProp name="filename">results.jtl</stringProp>
        </ResultCollector>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

### Load Test Metrics

**Key Metrics to Monitor**:

1. **Response Time**:
   - p50 (median)
   - p95 (95th percentile)
   - p99 (99th percentile)
   - Max response time

2. **Throughput**:
   - Requests per second (RPS)
   - Transactions per second (TPS)

3. **Error Rate**:
   - % of failed requests
   - Error types

4. **Resource Usage**:
   - CPU utilization
   - Memory usage
   - Database connections
   - Thread pool usage

**Example Results**:
```
Load Test Results (1000 concurrent users):
- p50 response time: 250ms ✅
- p95 response time: 800ms ✅
- p99 response time: 1500ms ✅
- Max response time: 3000ms ⚠️
- Throughput: 200 RPS ✅
- Error rate: 0.5% ✅
- CPU: 70% ✅
- Memory: 2GB ✅
```

---

## 6. Test Best Practices 📋

### 1. Test Naming

**Good Names**:
```java
@Test
void createOrder_WhenValidRequest_ShouldReturnCreatedOrder()

@Test
void getProduct_WhenProductNotFound_ShouldThrowException()

@Test
void processPayment_WhenInsufficientFunds_ShouldReturnFailure()
```

**Bad Names**:
```java
@Test
void test1()  // What does this test?

@Test
void testCreateOrder()  // What scenario?

@Test
void testPayment()  // Too vague
```

### 2. Test Structure (AAA Pattern)

```java
@Test
void testExample() {
    // Arrange (Given) - Setup
    User user = new User("john@example.com");
    when(userRepository.findByEmail(anyString())).thenReturn(user);
    
    // Act (When) - Execute
    UserResponse result = userService.getUser("john@example.com");
    
    // Assert (Then) - Verify
    assertThat(result.getEmail()).isEqualTo("john@example.com");
}
```

### 3. Test Data Builders

```java
/**
 * Builder for creating test data
 */
public class ProductTestBuilder {
    private String id = "prod-123";
    private String name = "Test Product";
    private double price = 99.99;
    private String category = "Electronics";
    
    public static ProductTestBuilder aProduct() {
        return new ProductTestBuilder();
    }
    
    public ProductTestBuilder withId(String id) {
        this.id = id;
        return this;
    }
    
    public ProductTestBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public ProductTestBuilder withPrice(double price) {
        this.price = price;
        return this;
    }
    
    public Product build() {
        return Product.builder()
            .id(id)
            .name(name)
            .price(price)
            .category(category)
            .build();
    }
}

// Usage
Product product = aProduct()
    .withName("iPhone 15")
    .withPrice(999.99)
    .build();
```

### 4. Test Coverage Goals

**Target Coverage**:
- **Line Coverage**: 80%+
- **Branch Coverage**: 70%+
- **Critical Paths**: 100%

**Measure with JaCoCo**:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <rules>
            <rule>
                <element>PACKAGE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

### 5. Continuous Integration

**GitHub Actions Example**:
```yaml
name: CI Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Run Unit Tests
        run: ./gradlew test
      
      - name: Run Integration Tests
        run: ./gradlew integrationTest
      
      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport
      
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: ./build/reports/jacoco/test/jacocoTestReport.xml
```

---

## 7. Test Types Summary 📊

| Test Type | Speed | Cost | Coverage | Confidence | Quantity |
|-----------|-------|------|----------|------------|----------|
| **Unit** | Fast (ms) | Low | Single unit | Low | Many (70%) |
| **Integration** | Medium (s) | Medium | Multiple units | Medium | Some (20%) |
| **Contract** | Fast (ms) | Low | API contract | High | Few |
| **E2E** | Slow (min) | High | Full system | High | Few (10%) |
| **Load** | Slow (min) | High | Performance | N/A | Few |

---

## 🎯 Key Takeaways

1. **Follow Test Pyramid**: Many unit tests, fewer E2E tests
2. **Test Behavior, Not Implementation**: Focus on what, not how
3. **Use Real Dependencies**: Prefer Testcontainers over H2
4. **Automate Everything**: CI/CD pipeline
5. **Monitor Coverage**: Aim for 80%+ line coverage
6. **Test Edge Cases**: Null, empty, boundary values
7. **Keep Tests Fast**: Slow tests won't be run
8. **Independent Tests**: No order dependencies
9. **Clear Names**: Test name = documentation
10. **Fail Fast**: Stop on first failure in CI

---

**Congratulations!** You now have production-grade testing strategies covering all levels! 🎉

