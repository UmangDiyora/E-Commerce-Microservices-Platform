# E-Commerce Microservices Platform - Complete Implementation Blueprint
## Spring Cloud Microservices Project for Resume

---

## PROJECT OVERVIEW

### What We're Building
A production-ready E-Commerce platform built with microservices architecture, demonstrating enterprise-level distributed systems expertise. Complete online shopping platform with service discovery, API gateway, event-driven communication, and distributed tracing.

### Why This Project Stands Out
- **Microservices Architecture**: Shows understanding of distributed systems
- **Spring Cloud Ecosystem**: Demonstrates modern cloud-native development
- **Event-Driven Design**: Asynchronous communication with message queues
- **Service Discovery**: Dynamic service registration and discovery
- **API Gateway Pattern**: Single entry point with routing and load balancing
- **Distributed Tracing**: System-wide observability with Zipkin
- **Scalability**: Each service can scale independently

### Core Features
1. **User Service**: Registration, authentication, profile management, JWT
2. **Product Service**: Catalog management, search, categories, inventory
3. **Order Service**: Cart, checkout, order management, order status
4. **Payment Service**: Payment processing, transaction management, refunds
5. **Notification Service**: Email/SMS notifications for orders and payments
6. **API Gateway**: Single entry point, routing, load balancing, rate limiting
7. **Service Discovery**: Eureka server for dynamic service registration
8. **Config Server**: Centralized configuration management
9. **Event Bus**: RabbitMQ for asynchronous inter-service communication

### Technology Stack
- **Framework**: Spring Boot 3.x, Java 17+
- **Cloud**: Spring Cloud (Eureka, Gateway, Config, Sleuth)
- **Database**: MySQL (per service), Redis (caching)
- **Message Queue**: RabbitMQ
- **Tracing**: Zipkin, Spring Cloud Sleuth
- **Security**: JWT, Spring Security
- **API Docs**: Springdoc OpenAPI
- **Build**: Maven
- **Containerization**: Docker, Docker Compose

---

## MICROSERVICES ARCHITECTURE

### Service Architecture Diagram
```
                         ┌─────────────────┐
                         │   API Gateway   │
                         │   (Port 8080)   │
                         └────────┬────────┘
                                  │
                    ┌─────────────┼─────────────┐
                    │             │             │
         ┌──────────▼──────┐  ┌──▼──────┐  ┌──▼──────────┐
         │  User Service   │  │ Product │  │   Order     │
         │   (Port 8081)   │  │ Service │  │  Service    │
         └────────┬────────┘  │ (8082)  │  │  (8083)     │
                  │           └────┬────┘  └──────┬──────┘
                  │                │               │
         ┌────────▼────────┐  ┌───▼────┐  ┌──────▼──────┐
         │  User Database  │  │Product │  │    Order    │
         │     (MySQL)     │  │  DB    │  │  Database   │
         └─────────────────┘  └────────┘  └──────┬──────┘
                                                  │
              ┌───────────────────────────────────┘
              │
         ┌────▼────────┐      ┌──────────────┐
         │  Payment    │      │ Notification │
         │  Service    │◄────►│   Service    │
         │  (8084)     │      │   (8085)     │
         └────┬────────┘      └──────────────┘
              │
         ┌────▼────────┐
         │  Payment DB │
         └─────────────┘

    ┌──────────────────┐     ┌──────────────┐
    │ Eureka Discovery │     │    Config    │
    │   (Port 8761)    │     │    Server    │
    └──────────────────┘     │  (Port 8888) │
                             └──────────────┘

    ┌──────────────────┐     ┌──────────────┐
    │    RabbitMQ      │     │    Zipkin    │
    │   (Port 5672)    │     │  (Port 9411) │
    └──────────────────┘     └──────────────┘
```

### Service Communication
```
Synchronous (REST):
- API Gateway → Services (routing)
- Order Service → Product Service (inventory check)
- Order Service → User Service (user validation)

Asynchronous (RabbitMQ):
- Order Service → Payment Service (order.created)
- Payment Service → Order Service (payment.completed)
- Payment Service → Notification Service (payment.processed)
- Order Service → Notification Service (order.confirmed)
```

---

## SERVICE-BY-SERVICE BREAKDOWN

### 1. EUREKA SERVICE DISCOVERY (Port 8761)

**Purpose**: Service registry for dynamic service discovery

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

**Main Class**:
```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**application.yml**:
```yaml
spring:
  application:
    name: eureka-server

server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false
```

---

### 2. CONFIG SERVER (Port 8888)

**Purpose**: Centralized configuration management

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

**Main Class**:
```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

**application.yml**:
```yaml
spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: https://github.com/your-repo/config-repo
          default-label: main
          search-paths: configs

server:
  port: 8888
```

---

### 3. API GATEWAY (Port 8080)

**Purpose**: Single entry point, routing, load balancing

**Dependencies**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

**Configuration**:
```java
@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // User Service Routes
            .route("user-service", r -> r
                .path("/api/users/**", "/api/auth/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("userServiceCircuitBreaker"))
                    .retry(3))
                .uri("lb://USER-SERVICE"))
            
            // Product Service Routes
            .route("product-service", r -> r
                .path("/api/products/**", "/api/categories/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("productServiceCircuitBreaker"))
                    .retry(3))
                .uri("lb://PRODUCT-SERVICE"))
            
            // Order Service Routes
            .route("order-service", r -> r
                .path("/api/orders/**", "/api/cart/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("orderServiceCircuitBreaker"))
                    .retry(3))
                .uri("lb://ORDER-SERVICE"))
            
            // Payment Service Routes
            .route("payment-service", r -> r
                .path("/api/payments/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("paymentServiceCircuitBreaker"))
                    .retry(3))
                .uri("lb://PAYMENT-SERVICE"))
            
            .build();
    }
}
```

**JWT Authentication Filter**:
```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(request.getPath().toString())) {
            return chain.filter(exchange);
        }
        
        // Extract JWT token
        String token = extractToken(request);
        
        if (token == null || !jwtUtils.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        // Add user info to headers for downstream services
        String userId = jwtUtils.getUserIdFromToken(token);
        ServerHttpRequest modifiedRequest = request.mutate()
            .header("X-User-Id", userId)
            .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
    
    @Override
    public int getOrder() {
        return -100;
    }
}
```

**Rate Limiting**:
```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> {
        String token = extractToken(exchange.getRequest());
        String userId = jwtUtils.getUserIdFromToken(token);
        return Mono.just(userId != null ? userId : "anonymous");
    };
}

// In application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

---

### 4. USER SERVICE (Port 8081)

**Database Schema**:
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL, -- CUSTOMER, ADMIN
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    address_type VARCHAR(20), -- BILLING, SHIPPING
    street_address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    is_default BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_address_user ON addresses(user_id);
```

**Key Entities**:
```java
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    private String firstName;
    private String lastName;
    private String phone;
    
    @Enumerated(EnumType.STRING)
    private Role role;
    
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Address> addresses;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Authentication Service**:
```java
@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    public AuthResponse register(RegisterRequest request) {
        // Check if user exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }
        
        // Create user
        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .role(Role.CUSTOMER)
            .isActive(true)
            .build();
        
        user = userRepository.save(user);
        
        // Generate JWT
        String token = jwtUtils.generateToken(user);
        
        return AuthResponse.builder()
            .token(token)
            .userId(user.getId())
            .email(user.getEmail())
            .role(user.getRole().toString())
            .build();
    }
    
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        
        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is inactive");
        }
        
        String token = jwtUtils.generateToken(user);
        
        return AuthResponse.builder()
            .token(token)
            .userId(user.getId())
            .email(user.getEmail())
            .role(user.getRole().toString())
            .build();
    }
}
```

**REST Endpoints**:
```
POST   /api/auth/register
POST   /api/auth/login
GET    /api/users/me
PUT    /api/users/me
GET    /api/users/{id} (Admin only)
POST   /api/users/me/addresses
GET    /api/users/me/addresses
PUT    /api/users/me/addresses/{id}
DELETE /api/users/me/addresses/{id}
```

---

### 5. PRODUCT SERVICE (Port 8082)

**Database Schema**:
```sql
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    parent_category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_category_id) REFERENCES categories(id)
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT,
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    sku VARCHAR(100) UNIQUE,
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE product_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_category ON products(category_id);
CREATE INDEX idx_product_sku ON products(sku);
CREATE INDEX idx_product_name ON products(name);
```

**Key Features**:

**Inventory Management**:
```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapToResponse(product);
    }
    
    @Transactional
    public boolean reserveStock(Long productId, Integer quantity) {
        Product product = productRepository.findByIdWithLock(productId);
        
        if (product.getStockQuantity() < quantity) {
            return false;
        }
        
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
        
        // Invalidate cache
        redisTemplate.delete("products::" + productId);
        
        return true;
    }
    
    @Transactional
    public void releaseStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow();
        
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
        
        redisTemplate.delete("products::" + productId);
    }
}
```

**Search Functionality**:
```java
@Service
public class ProductSearchService {
    
    public Page<ProductResponse> searchProducts(ProductSearchRequest request, 
                                               Pageable pageable) {
        Specification<Product> spec = Specification.where(null);
        
        if (request.getKeyword() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("name")), 
                       "%" + request.getKeyword().toLowerCase() + "%"));
        }
        
        if (request.getCategoryId() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("category").get("id"), request.getCategoryId()));
        }
        
        if (request.getMinPrice() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
        }
        
        if (request.getMaxPrice() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
        }
        
        spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), true));
        
        return productRepository.findAll(spec, pageable)
            .map(this::mapToResponse);
    }
}
```

**REST Endpoints**:
```
GET    /api/products (with search, filters, pagination)
GET    /api/products/{id}
POST   /api/products (Admin only)
PUT    /api/products/{id} (Admin only)
DELETE /api/products/{id} (Admin only)
GET    /api/categories
POST   /api/categories (Admin only)
POST   /api/products/{id}/reserve-stock (Internal - from Order Service)
POST   /api/products/{id}/release-stock (Internal)
```

---

### 6. ORDER SERVICE (Port 8083)

**Database Schema**:
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    total_amount DECIMAL(10,2) NOT NULL,
    shipping_address_id BIGINT,
    payment_status VARCHAR(20), -- PENDING, COMPLETED, FAILED, REFUNDED
    payment_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255),
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE shopping_cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES shopping_cart(id) ON DELETE CASCADE,
    UNIQUE KEY unique_cart_product (cart_id, product_id)
);

CREATE INDEX idx_order_user ON orders(user_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_number ON orders(order_number);
```

**Order Creation Flow**:
```java
@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        // 1. Get cart items
        List<CartItem> cartItems = cartService.getCartItems(userId);
        
        if (cartItems.isEmpty()) {
            throw new InvalidRequestException("Cart is empty");
        }
        
        // 2. Validate user and address
        UserResponse user = userServiceClient.getUserById(userId);
        AddressResponse shippingAddress = userServiceClient
            .getAddress(request.getShippingAddressId());
        
        // 3. Reserve stock for all products
        List<StockReservation> reservations = new ArrayList<>();
        for (CartItem item : cartItems) {
            boolean reserved = productServiceClient.reserveStock(
                item.getProductId(),
                item.getQuantity()
            );
            
            if (!reserved) {
                // Rollback previous reservations
                rollbackReservations(reservations);
                throw new OutOfStockException("Product out of stock: " + item.getProductId());
            }
            
            reservations.add(new StockReservation(item.getProductId(), item.getQuantity()));
        }
        
        // 4. Calculate total amount
        BigDecimal totalAmount = cartItems.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 5. Create order
        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .userId(userId)
            .status(OrderStatus.PENDING)
            .totalAmount(totalAmount)
            .shippingAddressId(request.getShippingAddressId())
            .paymentStatus(PaymentStatus.PENDING)
            .build();
        
        // 6. Create order items
        List<OrderItem> orderItems = cartItems.stream()
            .map(cartItem -> OrderItem.builder()
                .order(order)
                .productId(cartItem.getProductId())
                .productName(cartItem.getProductName())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .subtotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .build())
            .collect(Collectors.toList());
        
        order.setOrderItems(orderItems);
        order = orderRepository.save(order);
        
        // 7. Clear cart
        cartService.clearCart(userId);
        
        // 8. Publish order created event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(order.getId())
            .orderNumber(order.getOrderNumber())
            .userId(userId)
            .totalAmount(totalAmount)
            .shippingAddress(shippingAddress)
            .items(orderItems.stream()
                .map(this::mapToEventItem)
                .collect(Collectors.toList()))
            .build();
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
            event
        );
        
        log.info("Order created: {}", order.getOrderNumber());
        
        return mapToResponse(order);
    }
    
    private void rollbackReservations(List<StockReservation> reservations) {
        for (StockReservation reservation : reservations) {
            productServiceClient.releaseStock(
                reservation.getProductId(),
                reservation.getQuantity()
            );
        }
    }
}
```

**REST Endpoints**:
```
POST   /api/orders (create order from cart)
GET    /api/orders (user's orders with pagination)
GET    /api/orders/{id}
PUT    /api/orders/{id}/cancel
GET    /api/orders/{orderNumber}/track

POST   /api/cart/items
GET    /api/cart
PUT    /api/cart/items/{id}
DELETE /api/cart/items/{id}
DELETE /api/cart (clear cart)
```

---

### 7. PAYMENT SERVICE (Port 8084)

**Database Schema**:
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id VARCHAR(100) UNIQUE NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20), -- CREDIT_CARD, DEBIT_CARD, PAYPAL, STRIPE
    status VARCHAR(20) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED
    transaction_id VARCHAR(255),
    payment_gateway_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_order ON payments(order_id);
CREATE INDEX idx_payment_user ON payments(user_id);
CREATE INDEX idx_payment_status ON payments(status);
```

**Payment Processing**:
```java
@Service
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PaymentGatewayService paymentGatewayService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event for order: {}", event.getOrderNumber());
        
        try {
            // Create payment record
            Payment payment = Payment.builder()
                .paymentId(generatePaymentId())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();
            
            payment = paymentRepository.save(payment);
            
            // Process payment asynchronously
            processPaymentAsync(payment.getId());
            
        } catch (Exception e) {
            log.error("Failed to handle order created event", e);
            // Publish payment failed event
            publishPaymentFailedEvent(event.getOrderId(), e.getMessage());
        }
    }
    
    @Async
    public void processPaymentAsync(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        
        try {
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);
            
            // Call payment gateway (Stripe, PayPal, etc.)
            PaymentGatewayResponse response = paymentGatewayService.processPayment(
                payment.getAmount(),
                payment.getPaymentMethod()
            );
            
            if (response.isSuccess()) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(response.getTransactionId());
                payment.setPaymentGatewayResponse(response.toString());
                paymentRepository.save(payment);
                
                // Publish payment completed event
                PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(payment.getPaymentId())
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .transactionId(payment.getTransactionId())
                    .build();
                
                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_EXCHANGE,
                    RabbitMQConfig.PAYMENT_COMPLETED_ROUTING_KEY,
                    event
                );
                
                log.info("Payment completed: {}", payment.getPaymentId());
                
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setPaymentGatewayResponse(response.toString());
                paymentRepository.save(payment);
                
                publishPaymentFailedEvent(payment.getOrderId(), response.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            publishPaymentFailedEvent(payment.getOrderId(), e.getMessage());
        }
    }
    
    @Transactional
    public RefundResponse processRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidRequestException("Cannot refund non-completed payment");
        }
        
        // Process refund with gateway
        RefundGatewayResponse response = paymentGatewayService.processRefund(
            payment.getTransactionId(),
            payment.getAmount()
        );
        
        if (response.isSuccess()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
            
            // Publish refund event
            publishRefundEvent(payment);
            
            return RefundResponse.builder()
                .success(true)
                .refundId(response.getRefundId())
                .amount(payment.getAmount())
                .message("Refund processed successfully")
                .build();
        } else {
            throw new PaymentException("Refund failed: " + response.getErrorMessage());
        }
    }
}
```

**REST Endpoints**:
```
POST   /api/payments/process (Internal - triggered by event)
GET    /api/payments/{paymentId}
GET    /api/payments/order/{orderId}
POST   /api/payments/{paymentId}/refund (Admin only)
GET    /api/payments (user's payment history)
```

---

### 8. NOTIFICATION SERVICE (Port 8085)

**Purpose**: Send email/SMS notifications for orders and payments

**Event Listeners**:
```java
@Service
public class NotificationService {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SmsService smsService;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Sending order confirmation for order: {}", event.getOrderNumber());
        
        try {
            UserResponse user = userServiceClient.getUserById(event.getUserId());
            
            // Send email
            emailService.sendOrderConfirmationEmail(user, event);
            
            // Send SMS (optional)
            if (user.getPhone() != null) {
                smsService.sendOrderConfirmationSms(user.getPhone(), event.getOrderNumber());
            }
            
        } catch (Exception e) {
            log.error("Failed to send order notification", e);
        }
    }
    
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Sending payment confirmation for payment: {}", event.getPaymentId());
        
        try {
            UserResponse user = userServiceClient.getUserById(event.getUserId());
            
            emailService.sendPaymentConfirmationEmail(user, event);
            
            if (user.getPhone() != null) {
                smsService.sendPaymentConfirmationSms(user.getPhone(), event.getAmount());
            }
            
        } catch (Exception e) {
            log.error("Failed to send payment notification", e);
        }
    }
    
    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_CHANGED_QUEUE)
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Order status changed to {} for order: {}", 
                event.getNewStatus(), event.getOrderNumber());
        
        try {
            UserResponse user = userServiceClient.getUserById(event.getUserId());
            emailService.sendOrderStatusUpdateEmail(user, event);
            
        } catch (Exception e) {
            log.error("Failed to send status update notification", e);
        }
    }
}
```

**Email Templates**:
```java
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendOrderConfirmationEmail(UserResponse user, OrderCreatedEvent event) {
        String subject = "Order Confirmation - " + event.getOrderNumber();
        
        String content = buildOrderConfirmationHtml(user, event);
        
        sendEmail(user.getEmail(), subject, content);
    }
    
    private String buildOrderConfirmationHtml(UserResponse user, OrderCreatedEvent event) {
        return """
            <html>
            <body>
                <h2>Thank you for your order!</h2>
                <p>Hi %s,</p>
                <p>Your order has been received and is being processed.</p>
                
                <h3>Order Details:</h3>
                <p><strong>Order Number:</strong> %s</p>
                <p><strong>Total Amount:</strong> $%.2f</p>
                
                <h3>Items:</h3>
                <ul>
                    %s
                </ul>
                
                <h3>Shipping Address:</h3>
                <p>%s</p>
                
                <p>You will receive another email once your payment is confirmed.</p>
                
                <p>Thanks,<br>E-Commerce Team</p>
            </body>
            </html>
            """.formatted(
                user.getFirstName(),
                event.getOrderNumber(),
                event.getTotalAmount(),
                formatOrderItems(event.getItems()),
                formatAddress(event.getShippingAddress())
            );
    }
}
```

---

## RABBITMQ EVENT-DRIVEN ARCHITECTURE

### RabbitMQ Configuration

```java
@Configuration
public class RabbitMQConfig {
    
    // Exchange Names
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    
    // Queue Names
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String PAYMENT_COMPLETED_QUEUE = "payment.completed.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.queue";
    public static final String ORDER_STATUS_CHANGED_QUEUE = "order.status.changed.queue";
    
    // Routing Keys
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String ORDER_STATUS_CHANGED_ROUTING_KEY = "order.status.changed";
    
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }
    
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }
    
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
            .withArgument("x-dead-letter-exchange", "dlx.exchange")
            .build();
    }
    
    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(PAYMENT_COMPLETED_QUEUE).build();
    }
    
    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build();
    }
    
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
            .to(orderExchange())
            .with(ORDER_CREATED_ROUTING_KEY);
    }
    
    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder.bind(paymentCompletedQueue())
            .to(paymentExchange())
            .with(PAYMENT_COMPLETED_ROUTING_KEY);
    }
    
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

### Event Models

```java
@Data
@Builder
public class OrderCreatedEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private BigDecimal totalAmount;
    private AddressResponse shippingAddress;
    private List<OrderItemEvent> items;
    private LocalDateTime createdAt;
}

@Data
@Builder
public class PaymentCompletedEvent implements Serializable {
    private String paymentId;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String transactionId;
    private LocalDateTime completedAt;
}

@Data
@Builder
public class PaymentFailedEvent implements Serializable {
    private String paymentId;
    private Long orderId;
    private String errorMessage;
    private LocalDateTime failedAt;
}

@Data
@Builder
public class OrderStatusChangedEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime changedAt;
}
```

### Event Flow Diagram

```
1. ORDER CREATION FLOW:
   Order Service → order.created event → Payment Service
                                     ↘
                                      Notification Service

2. PAYMENT COMPLETION FLOW:
   Payment Service → payment.completed event → Order Service (update status)
                                            ↘
                                             Notification Service

3. PAYMENT FAILURE FLOW:
   Payment Service → payment.failed event → Order Service (cancel order)
                                         ↘
                                          Product Service (release stock)

4. ORDER STATUS CHANGE FLOW:
   Order Service → order.status.changed event → Notification Service
```

---

## INTER-SERVICE COMMUNICATION (FEIGN CLIENTS)

### Product Service Client

```java
@FeignClient(
    name = "PRODUCT-SERVICE",
    fallback = ProductServiceClientFallback.class
)
public interface ProductServiceClient {
    
    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);
    
    @PostMapping("/api/products/{id}/reserve-stock")
    Boolean reserveStock(@PathVariable Long id, 
                        @RequestParam Integer quantity);
    
    @PostMapping("/api/products/{id}/release-stock")
    void releaseStock(@PathVariable Long id, 
                     @RequestParam Integer quantity);
    
    @GetMapping("/api/products/batch")
    List<ProductResponse> getProductsByIds(@RequestParam List<Long> ids);
}

@Component
public class ProductServiceClientFallback implements ProductServiceClient {
    
    @Override
    public ProductResponse getProductById(Long id) {
        log.warn("Product service is unavailable, returning fallback");
        return ProductResponse.builder()
            .id(id)
            .name("Product Unavailable")
            .build();
    }
    
    @Override
    public Boolean reserveStock(Long id, Integer quantity) {
        log.error("Failed to reserve stock, product service unavailable");
        return false;
    }
    
    @Override
    public void releaseStock(Long id, Integer quantity) {
        log.error("Failed to release stock, product service unavailable");
    }
    
    @Override
    public List<ProductResponse> getProductsByIds(List<Long> ids) {
        return Collections.emptyList();
    }
}
```

### User Service Client

```java
@FeignClient(
    name = "USER-SERVICE",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {
    
    @GetMapping("/api/users/{id}")
    UserResponse getUserById(@PathVariable Long id);
    
    @GetMapping("/api/users/{userId}/addresses/{addressId}")
    AddressResponse getAddress(@PathVariable Long userId, 
                              @PathVariable Long addressId);
}
```

---

## DISTRIBUTED TRACING WITH ZIPKIN

### Configuration in Each Service

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

**application.yml**:
```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0  # Sample 100% of requests (dev), use 0.1 in prod
  zipkin:
    base-url: http://localhost:9411
    sender:
      type: web

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### Custom Span Creation

```java
@Service
public class OrderService {
    
    @Autowired
    private Tracer tracer;
    
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        Span span = tracer.nextSpan().name("create-order").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("user.id", userId.toString());
            span.tag("order.items", String.valueOf(request.getItems().size()));
            
            // Create order logic
            OrderResponse response = processOrder(request, userId);
            
            span.tag("order.id", response.getOrderNumber());
            span.event("order.created");
            
            return response;
            
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Viewing Traces

Access Zipkin UI at `http://localhost:9411` to:
- View request traces across microservices
- Identify bottlenecks and latency issues
- Debug distributed transactions
- Monitor service dependencies

---

## RESILIENCE PATTERNS

### Circuit Breaker (Resilience4j)

```java
@Configuration
public class Resilience4jConfig {
    
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(5)
                .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3))
                .build())
            .build());
    }
}

// Usage in Feign Client
@FeignClient(
    name = "PRODUCT-SERVICE",
    fallback = ProductServiceClientFallback.class,
    configuration = FeignConfig.class
)
public interface ProductServiceClient {
    // ... methods
}
```

### Retry Mechanism

```java
@Service
public class OrderService {
    
    @Retryable(
        value = {ServiceUnavailableException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ProductResponse getProductWithRetry(Long productId) {
        return productServiceClient.getProductById(productId);
    }
    
    @Recover
    public ProductResponse recoverFromProductServiceFailure(
            ServiceUnavailableException e, Long productId) {
        log.error("Failed to get product after retries: {}", productId);
        return ProductResponse.builder()
            .id(productId)
            .name("Product Temporarily Unavailable")
            .build();
    }
}
```

### Rate Limiting

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws IOException {
        
        String userId = extractUserId(request);
        String key = "rate_limit:" + userId;
        
        Long requestCount = redisTemplate.opsForValue().increment(key);
        
        if (requestCount == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        
        if (requestCount > 100) { // 100 requests per minute
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## TESTING STRATEGIES

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private ProductServiceClient productServiceClient;
    
    @Mock
    private RabbitTemplate rabbitTemplate;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void createOrder_ValidCart_Success() {
        // Arrange
        CreateOrderRequest request = createValidRequest();
        when(productServiceClient.reserveStock(any(), any())).thenReturn(true);
        when(orderRepository.save(any())).thenReturn(createOrder());
        
        // Act
        OrderResponse response = orderService.createOrder(request, 1L);
        
        // Assert
        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(rabbitTemplate).convertAndSend(any(), any(), any());
    }
    
    @Test
    void createOrder_OutOfStock_ThrowsException() {
        // Arrange
        when(productServiceClient.reserveStock(any(), any())).thenReturn(false);
        
        // Act & Assert
        assertThrows(OutOfStockException.class,
            () -> orderService.createOrder(request, 1L));
        verify(orderRepository, never()).save(any());
    }
}
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ProductServiceClient productServiceClient;
    
    @Test
    void createOrder_ValidRequest_ReturnsCreated() throws Exception {
        // Mock external service
        when(productServiceClient.reserveStock(any(), any())).thenReturn(true);
        
        mockMvc.perform(post("/api/orders")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
```

### Contract Testing (Pact)

```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ProductService", port = "8082")
class ProductServiceContractTest {
    
    @Pact(consumer = "OrderService")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        return builder
            .given("product with id 1 exists")
            .uponReceiving("a request to get product by id")
                .path("/api/products/1")
                .method("GET")
            .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                    .integerType("id", 1)
                    .stringType("name", "Test Product")
                    .decimalType("price", 99.99))
            .toPact();
    }
    
    @Test
    @PactTestFor(pactMethod = "createPact")
    void testGetProductById(MockServer mockServer) {
        ProductServiceClient client = new ProductServiceClientImpl(mockServer.getUrl());
        ProductResponse product = client.getProductById(1L);
        
        assertNotNull(product);
        assertEquals("Test Product", product.getName());
    }
}
```

---

## DOCKER DEPLOYMENT

### docker-compose.yml

```yaml
version: '3.8'

services:
  # Infrastructure Services
  eureka-server:
    build: ./eureka-server
    ports:
      - "8761:8761"
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  config-server:
    build: ./config-server
    ports:
      - "8888:8888"
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server
    networks:
      - ecommerce-network

  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_REDIS_HOST=redis
    depends_on:
      - eureka-server
      - redis
    networks:
      - ecommerce-network

  # Databases
  mysql-user:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: user_db
    volumes:
      - mysql-user-data:/var/lib/mysql
    networks:
      - ecommerce-network

  mysql-product:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: product_db
    volumes:
      - mysql-product-data:/var/lib/mysql
    networks:
      - ecommerce-network

  mysql-order:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: order_db
    volumes:
      - mysql-order-data:/var/lib/mysql
    networks:
      - ecommerce-network

  mysql-payment:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: payment_db
    volumes:
      - mysql-payment-data:/var/lib/mysql
    networks:
      - ecommerce-network

  # Message Queue
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin
    networks:
      - ecommerce-network

  # Caching
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - ecommerce-network

  # Tracing
  zipkin:
    image: openzipkin/zipkin
    ports:
      - "9411:9411"
    networks:
      - ecommerce-network

  # Microservices
  user-service:
    build: ./user-service
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-user:3306/user_db
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_ZIPKIN_BASE_URL=http://zipkin:9411
    depends_on:
      - mysql-user
      - eureka-server
      - zipkin
    networks:
      - ecommerce-network

  product-service:
    build: ./product-service
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-product:3306/product_db
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_REDIS_HOST=redis
      - SPRING_ZIPKIN_BASE_URL=http://zipkin:9411
    depends_on:
      - mysql-product
      - eureka-server
      - redis
      - zipkin
    networks:
      - ecommerce-network

  order-service:
    build: ./order-service
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-order:3306/order_db
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_RABBITMQ_HOST=rabbitmq
      - SPRING_ZIPKIN_BASE_URL=http://zipkin:9411
    depends_on:
      - mysql-order
      - eureka-server
      - rabbitmq
      - zipkin
    networks:
      - ecommerce-network

  payment-service:
    build: ./payment-service
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-payment:3306/payment_db
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_RABBITMQ_HOST=rabbitmq
      - SPRING_ZIPKIN_BASE_URL=http://zipkin:9411
    depends_on:
      - mysql-payment
      - eureka-server
      - rabbitmq
      - zipkin
    networks:
      - ecommerce-network

  notification-service:
    build: ./notification-service
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_RABBITMQ_HOST=rabbitmq
      - SPRING_ZIPKIN_BASE_URL=http://zipkin:9411
    depends_on:
      - eureka-server
      - rabbitmq
      - zipkin
    networks:
      - ecommerce-network

networks:
  ecommerce-network:
    driver: bridge

volumes:
  mysql-user-data:
  mysql-product-data:
  mysql-order-data:
  mysql-payment-data:
```

### Deployment Commands

```bash
# Build all services
mvn clean package -DskipTests

# Build Docker images
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f [service-name]

# Scale a service
docker-compose up -d --scale product-service=3

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## MONITORING & OBSERVABILITY

### Health Checks

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Override
    public Health health() {
        try {
            long count = orderRepository.count();
            return Health.up()
                .withDetail("database", "available")
                .withDetail("order_count", count)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Metrics (Micrometer)

```java
@Service
public class OrderService {
    
    private final Counter orderCreatedCounter;
    private final Timer orderProcessingTimer;
    
    public OrderService(MeterRegistry registry) {
        this.orderCreatedCounter = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("service", "order-service")
            .register(registry);
            
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Time taken to process order")
            .register(registry);
    }
    
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        return orderProcessingTimer.record(() -> {
            OrderResponse response = processOrder(request, userId);
            orderCreatedCounter.increment();
            return response;
        });
    }
}
```

### Actuator Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## INTERVIEW PREPARATION

### Key Talking Points

**"Why microservices architecture?"**
"I chose microservices to demonstrate my understanding of distributed systems and cloud-native development. Each service is independently deployable and scalable, uses its own database (database-per-service pattern), and communicates via REST APIs and event-driven messaging. This architecture allows for technology flexibility, fault isolation, and independent team scaling."

**"Explain service discovery"**
"I use Netflix Eureka for service discovery. Each microservice registers itself with Eureka on startup, including its hostname and port. When Service A needs to call Service B, it queries Eureka for available instances and uses client-side load balancing (Ribbon) to distribute requests. This enables dynamic scaling without hardcoded URLs."

**"How does event-driven communication work?"**
"I use RabbitMQ for asynchronous communication. When an order is created, the Order Service publishes an 'order.created' event to the message queue. The Payment Service subscribes to this event and processes payment asynchronously. This decouples services, improves resilience, and allows for better scalability. If Payment Service is down, messages queue up and process when it's back online."

**"Explain distributed tracing"**
"I use Spring Cloud Sleuth and Zipkin for distributed tracing. Sleuth automatically injects trace and span IDs into every request. As a request flows through multiple services (Gateway → Order Service → Product Service), these IDs are propagated. Zipkin collects all spans and visualizes the complete request path, showing latency at each hop. This is crucial for debugging distributed transactions and identifying bottlenecks."

**"How do you handle service failures?"**
"I implement multiple resilience patterns: Circuit Breaker (Resilience4j) prevents cascading failures by failing fast when a service is down. Fallback methods provide degraded functionality. Retry logic with exponential backoff handles transient failures. Rate limiting prevents service overload. These patterns ensure the system remains partially functional even when some services fail."

### Technical Deep-Dive Questions

1. **CAP Theorem**: How does your architecture handle consistency vs availability?
2. **Saga Pattern**: How do you handle distributed transactions?
3. **API Versioning**: How would you version your APIs?
4. **Data Consistency**: How do you maintain consistency across service databases?
5. **Security**: How do you secure inter-service communication?
6. **Monitoring**: What metrics do you track in production?
7. **Scalability**: Which services need to scale most and why?

---

## CONCLUSION

This blueprint provides everything needed to build a **production-ready E-Commerce Microservices Platform**. The project demonstrates:

✅ Microservices architecture expertise
✅ Spring Cloud ecosystem proficiency
✅ Event-driven design with RabbitMQ
✅ Service discovery and API gateway patterns
✅ Distributed tracing and observability
✅ Resilience patterns (Circuit Breaker, Retry)
✅ Docker containerization
✅ Inter-service communication (REST + Events)

### Success Metrics

- All services independently deployable
- Service discovery working
- Event-driven communication functional
- Distributed tracing showing full request paths
- Circuit breakers preventing cascading failures
- System scales horizontally
- Comprehensive monitoring and logging

**This project will significantly stand out as a complex, enterprise-level system!** 🚀

---

*Document Version: 1.0*  
*Target: Java Spring Boot Developers*  
*Estimated Time: 30-35 days*  
*Skill Level: Advanced*  
*Architecture: Microservices*