# 🛒 4단계: Order Service 상세 가이드

이 문서는 MSA 프로젝트의 **Order Service**에 대한 상세한 설명입니다. OpenFeign을 통한 서비스 간 통신, Resilience4j의 `@CircuitBreaker` 어노테이션을 사용한 Circuit Breaker 패턴 및 Fallback 메커니즘을 구현한 서비스입니다.

---

## 📍 목차

1. [Order Service 개요](#1-order-service-개요)
2. [코드 구조 분석](#2-코드-구조-분석)
3. [OpenFeign을 통한 서비스 간 통신](#3-openfeign을-통한-서비스-간-통신)
4. [Circuit Breaker 및 Fallback](#4-circuit-breaker-및-fallback)
5. [JWT 토큰 전파](#5-jwt-토큰-전파)
6. [API 엔드포인트](#6-api-엔드포인트)
7. [실습 가이드](#7-실습-가이드)

---

## 1. Order Service 개요

### 1.1 역할

**Order Service**는 주문 관리를 담당하는 마이크로서비스입니다.

**주요 기능**:
- 주문 등록, 조회, 수정, 삭제 (CRUD)
- OpenFeign을 통한 Member Service 연동
- Resilience4j `@CircuitBreaker` 어노테이션을 통한 Circuit Breaker 패턴
- Fallback 메커니즘을 통한 서비스 장애 대응
- JWT 토큰 전파를 통한 인증 유지

### 1.2 기술 스택

- **Spring Boot**: 웹 애플리케이션 프레임워크
- **Spring Data JPA**: 데이터베이스 접근
- **Spring Security**: JWT 토큰 기반 인증
- **OpenFeign**: 선언적 REST 클라이언트
- **Resilience4j**: Circuit Breaker 패턴
- **H2 Database**: 인메모리 데이터베이스 (개발용)

### 1.3 서비스 포트

- **포트**: 8082
- **접속 URL**: http://localhost:8082
- **H2 Console**: http://localhost:8082/h2-console

---

## 2. 코드 구조 분석

### 2.1 프로젝트 구조

```
order-service/
├── src/main/java/com/example/order/
│   ├── OrderServiceApplication.java      # 애플리케이션 진입점
│   ├── config/
│   │   ├── SecurityConfig.java          # Spring Security 설정
│   │   └── FeignClientConfig.java      # OpenFeign 설정 (JWT 토큰 전파)
│   ├── controller/
│   │   ├── OrderController.java        # REST API 엔드포인트
│   │   └── TestController.java        # 테스트 엔드포인트
│   ├── client/
│   │   └── MemberServiceClient.java    # OpenFeign 클라이언트
│   ├── dto/
│   │   └── OrderDto.java               # 데이터 전송 객체
│   ├── entity/
│   │   └── Order.java                  # 주문 엔티티
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # 전역 예외 처리
│   │   ├── OrderNotFoundException.java
│   │   └── InvalidOrderException.java
│   ├── repository/
│   │   └── OrderRepository.java       # JPA 리포지토리
│   ├── security/
│   │   └── SecurityRoles.java          # 역할 상수 정의
│   └── service/
│       ├── OrderService.java          # 비즈니스 로직
│       └── MemberIntegrationService.java  # Member Service 통합 서비스 (Circuit Breaker 적용)
```

### 2.2 엔티티: Order

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다")
    private String productName;

    @Column(nullable = false)
    @NotNull(message = "수량은 필수입니다")
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "단가는 필수입니다")
    @DecimalMin(value = "0.0", inclusive = false, message = "단가는 0보다 커야 합니다")
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    @NotNull(message = "총 금액은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = false, message = "총 금액은 0보다 커야 합니다")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(length = 500)
    @Size(max = 500, message = "주문 메모는 500자를 초과할 수 없습니다")
    private String orderMemo;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING("대기중"),
        CONFIRMED("확인완료"),
        PROCESSING("처리중"),
        SHIPPED("배송중"),
        DELIVERED("배송완료"),
        CANCELLED("취소됨"),
        REFUNDED("환불됨");
    }
}
```

**핵심 필드**:
- `id`: 주문 ID (자동 생성)
- `memberId`: 회원 ID (Member Service와 연동)
- `productName`: 상품명
- `quantity`: 수량
- `unitPrice`: 단가
- `totalAmount`: 총 금액 (단가 × 수량)
- `status`: 주문 상태 (PENDING, CONFIRMED, 등)
- `orderMemo`: 주문 메모
- `createdAt`: 생성 시간 (자동 생성)
- `updatedAt`: 수정 시간 (자동 업데이트)

### 2.3 OpenFeign 클라이언트: MemberServiceClient

```java
@FeignClient(
    configuration = FeignClientConfig.class,
    name = "member-service",
    url = "${member-service.url:http://localhost:8081}"
    // 주의: Fallback을 제거하여 Circuit Breaker가 실패를 올바르게 카운트하도록 합니다.
    // Fallback이 실행되면 예외가 발생하지 않으므로 Circuit Breaker가 실패로 카운트하지 않습니다.
)
public interface MemberServiceClient {
    @GetMapping("/members/{id}")
    MemberDto getMemberById(@PathVariable("id") Long id);

    @GetMapping("/members/username/{username}")
    MemberDto getMemberByUsername(@PathVariable("username") String username);

    @GetMapping("/members/health")
    HealthCheckResponse getHealthCheck();
}
```

**핵심 설정**:
- `name = "member-service"`: Eureka에서 서비스를 찾을 때 사용하는 이름
- `url`: 직접 URL 지정 (Eureka 미사용 시)
- `configuration = FeignClientConfig.class`: JWT 토큰 전파 설정
- **OpenFeign Fallback 미사용**: Resilience4j의 `@CircuitBreaker` 어노테이션을 Service 레벨에서 사용합니다.

### 2.4 Circuit Breaker 및 Fallback 구현 (Resilience4j @CircuitBreaker)

**Resilience4j의 `@CircuitBreaker` 어노테이션을 사용하여 Circuit Breaker 패턴을 적용합니다.**

**구현 방식**:
- `MemberIntegrationService`라는 별도 서비스 클래스에 Circuit Breaker 로직 구현
- `OrderService`는 `MemberIntegrationService`를 주입받아 사용
- 순환 참조 문제 없이 프록시를 통한 호출 보장

**장점**:
- Circuit Breaker가 실패를 올바르게 카운트
- AOP 기반으로 깔끔한 코드
- 메트릭 수집이 정확함
- 비즈니스 로직과 Fallback 로직 분리
- 순환 참조 문제 해결 (자기 주입 불필요)

```java
// MemberIntegrationService.java
@Service
public class MemberIntegrationService {
    private final MemberServiceClient memberServiceClient;

    @CircuitBreaker(name = "member-service", fallbackMethod = "validateMemberFallback")
    public MemberServiceClient.MemberDto validateMember(Long memberId) {
        log.debug("Validating member with ID: {}", memberId);
        return memberServiceClient.getMemberById(memberId);
    }

    // Fallback 메서드 (같은 클래스 내에 있어야 함)
    @SuppressWarnings("unused")
    public MemberServiceClient.MemberDto validateMemberFallback(Long memberId, Exception e) {
        log.error("Member Service unavailable. Using fallback for memberId: {}", memberId, e);
        
        return new MemberServiceClient.MemberDto(
            memberId,
            "unknown-user-" + memberId,
            "unknown@example.com",
            "알 수 없는 사용자",
            "000-0000-0000",
            "UNKNOWN",
            "서비스 일시 중단"
        );
    }

    @CircuitBreaker(name = "member-service", fallbackMethod = "getMemberNameFallback")
    public String getMemberName(Long memberId) {
        log.debug("Getting member name for ID: {}", memberId);
        MemberServiceClient.MemberDto member = memberServiceClient.getMemberById(memberId);
        return member.getFullName();
    }

    @SuppressWarnings("unused")
    public String getMemberNameFallback(Long memberId, Exception e) {
        log.warn("Member Service unavailable. Using fallback for member name, memberId: {}", memberId, e);
        return "알 수 없는 사용자";
    }
}
```

```java
// OrderService.java
@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberIntegrationService memberIntegrationService;

    @Transactional
    public OrderDto.Response createOrder(OrderDto.CreateRequest request) {
        // MemberIntegrationService를 통한 호출로 @CircuitBreaker 작동 보장
        MemberServiceClient.MemberDto member = memberIntegrationService.validateMember(request.getMemberId());
        
        // ... 주문 생성 로직 ...
    }
}
```

**Fallback 동작**:
- Member Service 장애 시 `@CircuitBreaker`가 자동으로 Fallback 메서드 호출
- Circuit Breaker가 실패를 올바르게 카운트
- Fallback 데이터 반환으로 주문 처리는 계속 진행
- 로그에 에러 메시지 기록

**주의사항**:
- Fallback 메서드는 원본 메서드와 같은 클래스에 있어야 함
- Fallback 메서드 시그니처: 원본 메서드 파라미터 + `Exception` 파라미터
- `@CircuitBreaker` 어노테이션이 작동하려면 프록시를 통한 호출이 필요하므로, 별도 서비스로 분리하여 순환 참조 문제를 해결

### 2.5 JWT 토큰 전파: FeignClientConfig

```java
@Configuration
public class FeignClientConfig {
    @Bean
    public RequestInterceptor authorizationHeaderInterceptor() {
        return template -> {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
                // 현재 요청의 Authorization 헤더 추출
                String authorization = servletRequestAttributes.getRequest()
                    .getHeader(HttpHeaders.AUTHORIZATION);
                
                // Authorization 헤더가 있으면 OpenFeign 요청에 추가
                if (StringUtils.hasText(authorization)) {
                    template.header(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
        };
    }
}
```

**동작 원리**:
1. 클라이언트 → Order Service (JWT 토큰 포함)
2. Order Service → Member Service (OpenFeign, JWT 토큰 자동 전달)
3. Member Service는 JWT 토큰을 검증하여 인증 수행

### 2.6 서비스: OrderService

```java
@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberIntegrationService memberIntegrationService;

    public OrderService(OrderRepository orderRepository, MemberIntegrationService memberIntegrationService) {
        this.orderRepository = orderRepository;
        this.memberIntegrationService = memberIntegrationService;
    }

    @Transactional
    public OrderDto.Response createOrder(OrderDto.CreateRequest request) {
        // 1. 회원 정보 조회 및 검증 (MemberIntegrationService를 통한 호출로 @CircuitBreaker 작동 보장)
        MemberServiceClient.MemberDto member = memberIntegrationService.validateMember(request.getMemberId());
        
        // 2. 주문 데이터 검증
        validateOrderRequest(request);

        // 3. 엔터티 생성 및 저장
        Order order = request.toEntity();
        Order savedOrder = orderRepository.save(order);

        // 4. 회원명과 함께 응답 DTO 생성
        return OrderDto.Response.from(savedOrder, member.getFullName());
    }

    @CircuitBreaker(name = "member-service", fallbackMethod = "validateMemberFallback")
    private MemberServiceClient.MemberDto validateMember(Long memberId) {
        log.debug("Validating member with ID: {}", memberId);
        return memberServiceClient.getMemberById(memberId);
    }

    private MemberServiceClient.MemberDto validateMemberFallback(Long memberId, Exception e) {
        log.error("Member Service unavailable. Using fallback for memberId: {}", memberId, e);
        return new MemberServiceClient.MemberDto(
            memberId,
            "unknown-user-" + memberId,
            "unknown@example.com",
            "알 수 없는 사용자",
            "000-0000-0000",
            "UNKNOWN",
            "서비스 일시 중단"
        );
    }
}
```

**주요 메서드**:
- `createOrder()`: 주문 생성 (Member Service 연동)
- `validateMember()`: 회원 정보 검증 (`@CircuitBreaker` 적용)
- `validateMemberFallback()`: 회원 정보 검증 Fallback 메서드
- `getMemberName()`: 회원명 조회 (`@CircuitBreaker` 적용)
- `getMemberNameFallback()`: 회원명 조회 Fallback 메서드

---

## 3. OpenFeign을 통한 서비스 간 통신

### 3.1 OpenFeign이란?

**OpenFeign**은 선언적 REST 클라이언트입니다. 인터페이스만 정의하면 자동으로 HTTP 요청을 생성합니다.

**장점**:
- 인터페이스만으로 REST 클라이언트 구현
- 자동으로 HTTP 요청 생성
- Circuit Breaker 통합 지원
- Fallback 메커니즘 지원

### 3.2 OpenFeign 설정

#### 3.2.1 애플리케이션 설정

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients  // OpenFeign 활성화
public class OrderServiceApplication {
    // ...
}
```

#### 3.2.2 설정 파일

```yaml
# OpenFeign 설정
feign:
  client:
    config:
      member-service:
        connect-timeout: 5000
        read-timeout: 5000
        logger-level: full
```

**설정 항목**:
- `connect-timeout`: 연결 타임아웃 (5초)
- `read-timeout`: 읽기 타임아웃 (5초)
- `logger-level`: 로그 레벨 (full)

### 3.3 OpenFeign 사용 예시

```java
@FeignClient(
    name = "member-service",
    url = "${member-service.url:http://localhost:8081}"
    // 주의: Fallback을 제거하여 Circuit Breaker가 실패를 올바르게 카운트하도록 합니다.
)
public interface MemberServiceClient {
    @GetMapping("/members/{id}")
    MemberDto getMemberById(@PathVariable("id") Long id);
}
```

**사용 방법**:
1. 인터페이스에 `@FeignClient` 어노테이션 추가
2. 메서드에 HTTP 메서드 어노테이션 추가 (`@GetMapping`, `@PostMapping` 등)
3. Spring이 자동으로 구현체 생성

**Fallback 처리**:
- OpenFeign의 Fallback을 사용하지 않습니다.
- 대신 Resilience4j의 `@CircuitBreaker` 어노테이션을 Service 레벨에서 사용합니다.
- Circuit Breaker가 실패를 올바르게 카운트하고 Fallback을 자동으로 처리합니다.

---

## 4. Circuit Breaker 및 Fallback

### 4.1 Circuit Breaker란?

**Circuit Breaker**는 서비스 장애 시 자동으로 요청을 차단하고 Fallback을 실행하는 패턴입니다.

**상태**:
- **CLOSED**: 정상 상태 (요청 통과)
- **OPEN**: Circuit Breaker 열림 (요청 차단, Fallback 실행)
- **HALF_OPEN**: 반열림 상태 (테스트 요청 허용)

### 4.2 Resilience4j 설정

```yaml
# Circuit Breaker 설정
resilience4j:
  circuitbreaker:
    instances:
      member-service:
        sliding-window-size: 10
        failure-rate-threshold: 50  # 50% 실패율 임계값
        wait-duration-in-open-state: 10000
        permitted-number-of-calls-in-half-open-state: 3
        minimum-number-of-calls: 5

# OpenFeign Circuit Breaker 활성화
feign:
  circuitbreaker:
    enabled: true
```

**설정 항목**:
- `sliding-window-size`: 슬라이딩 윈도우 크기 (10개 요청)
- `failure-rate-threshold`: 실패율 임계값 (50%)
- `wait-duration-in-open-state`: OPEN 상태 유지 시간 (10초)
- `permitted-number-of-calls-in-half-open-state`: HALF_OPEN 상태에서 허용되는 요청 수 (3개)
- `minimum-number-of-calls`: Circuit Breaker 동작을 위한 최소 요청 수 (5개)
- `feign.circuitbreaker.enabled`: OpenFeign Circuit Breaker 활성화

### 4.3 Circuit Breaker 동작 흐름

```
1. 정상 상태 (CLOSED)
   - 모든 요청이 Member Service로 전달
   ↓

2. 실패율 증가
   - 10개 요청 중 실패율이 50% 이상 (failure-rate-threshold: 50)
   - 최소 5개 요청 발생 (minimum-number-of-calls: 5)
   ↓

3. Circuit Breaker 열림 (OPEN)
   - 요청이 차단되고 Fallback 실행
   - 10초 동안 OPEN 상태 유지
   ↓

4. 반열림 상태 (HALF_OPEN)
   - 10초 후 3개의 테스트 요청 허용
   ↓

5. 복구 확인
   - 테스트 요청 성공 → CLOSED 상태로 복귀
   - 테스트 요청 실패 → OPEN 상태로 복귀
```

### 4.4 Fallback 메커니즘 (Resilience4j @CircuitBreaker)

**Resilience4j의 `@CircuitBreaker` 어노테이션을 사용하여 Fallback을 처리합니다.**

**장점**:
- Circuit Breaker가 실패를 올바르게 카운트
- AOP 기반으로 깔끔한 코드
- 메트릭 수집이 정확함
- 비즈니스 로직과 Fallback 로직 분리

```java
// MemberIntegrationService.validateMember() 메서드
@CircuitBreaker(name = "member-service", fallbackMethod = "validateMemberFallback")
public MemberServiceClient.MemberDto validateMember(Long memberId) {
    log.debug("Validating member with ID: {}", memberId);
    return memberServiceClient.getMemberById(memberId);
}

// Fallback 메서드 (같은 클래스 내에 있어야 함)
@SuppressWarnings("unused")
public MemberServiceClient.MemberDto validateMemberFallback(Long memberId, Exception e) {
    log.error("Member Service unavailable. Using fallback for memberId: {}", memberId, e);
    
    return new MemberServiceClient.MemberDto(
        memberId,
        "unknown-user-" + memberId,
        "unknown@example.com",
        "알 수 없는 사용자",
        "000-0000-0000",
        "UNKNOWN",
        "서비스 일시 중단"
    );
}
```

**Fallback 동작**:
- Member Service 장애 시 `@CircuitBreaker`가 자동으로 Fallback 메서드 호출
- Circuit Breaker가 실패를 올바르게 카운트
- Fallback 데이터 반환으로 주문 처리는 계속 진행
- 로그에 에러 메시지 기록

**Circuit Breaker와의 관계**:
- 예외가 발생하면 Circuit Breaker가 실패로 카운트
- 실패율이 임계값(50%)을 초과하면 Circuit Breaker가 OPEN 상태로 전환
- OPEN 상태에서는 요청이 차단되고 Fallback 메서드가 자동 실행
- Fallback 메서드가 실행되어 주문 처리는 계속 진행

**구현 방식의 장점**:
- 별도 서비스(`MemberIntegrationService`)로 분리하여 순환 참조 문제 해결
- Spring AOP 프록시를 통한 호출이 보장되어 `@CircuitBreaker` 어노테이션이 정상 작동
- 자기 주입(self-injection) 불필요

**Fallback 메서드 작성 규칙**:
1. Fallback 메서드는 원본 메서드와 같은 클래스에 있어야 함
2. Fallback 메서드 시그니처: 원본 메서드 파라미터 + `Exception` 파라미터
3. 반환 타입은 원본 메서드와 동일해야 함

### 4.5 Circuit Breaker 상태 확인

```java
@GetMapping("/test/circuit-breaker-status")
public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("member-service");
    CircuitBreaker.State state = circuitBreaker.getState();
    CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
    
    String statusDescription = switch (state) {
        case CLOSED -> "정상 상태 - 모든 요청이 통과됩니다";
        case OPEN -> "Circuit Breaker 열림 - 요청이 차단되고 Fallback이 실행됩니다";
        case HALF_OPEN -> "반열림 상태 - 제한된 요청만 허용하여 서비스 복구를 테스트합니다";
        case DISABLED -> "비활성화됨 - Circuit Breaker가 작동하지 않습니다";
        default -> "알 수 없는 상태";
    };
    
    return ResponseEntity.ok(Map.of(
        "circuitBreakerStatus", state.toString(),
        "service", "member-service",
        "failureRate", String.format("%.2f%%", metrics.getFailureRate()),
        "numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls(),
        "numberOfFailedCalls", metrics.getNumberOfFailedCalls(),
        "statusDescription", statusDescription
    ));
}
```

---

## 5. JWT 토큰 전파

### 5.1 JWT 토큰 전파 흐름

```
1. 클라이언트 요청
   POST /api/orders
   Authorization: Bearer {JWT_TOKEN}
   ↓

2. Gateway Service
   - JWT 토큰 검증
   - Order Service로 요청 전달 (JWT 토큰 포함)
   ↓

3. Order Service
   - JWT 토큰 검증
   - Member Service 호출 필요
   ↓

4. FeignClientConfig
   - 현재 요청의 Authorization 헤더 추출
   - OpenFeign 요청에 Authorization 헤더 추가
   ↓

5. Member Service
   - JWT 토큰 검증
   - 회원 정보 반환
   ↓

6. Order Service
   - 회원 정보와 함께 주문 처리
```

### 5.2 FeignClientConfig 구현

```java
@Configuration
public class FeignClientConfig {
    @Bean
    public RequestInterceptor authorizationHeaderInterceptor() {
        return template -> {
            // 현재 요청의 RequestAttributes 추출
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
                // 현재 요청의 Authorization 헤더 추출
                String authorization = servletRequestAttributes.getRequest()
                    .getHeader(HttpHeaders.AUTHORIZATION);
                
                // Authorization 헤더가 있으면 OpenFeign 요청에 추가
                if (StringUtils.hasText(authorization)) {
                    template.header(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
        };
    }
}
```

**핵심 포인트**:
- `RequestContextHolder`: 현재 요청 컨텍스트 접근
- `RequestInterceptor`: OpenFeign 요청 전에 실행되는 인터셉터
- Authorization 헤더를 자동으로 전달

---

## 6. API 엔드포인트

### 6.1 API 목록

| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | `/orders` | 주문 생성 | ADMIN |
| GET | `/orders` | 주문 목록 조회 (페이징) | ADMIN, USER |
| GET | `/orders/all` | 모든 주문 조회 | ADMIN, USER |
| GET | `/orders/{id}` | ID로 주문 조회 | ADMIN, USER |
| GET | `/orders/member/{memberId}` | 회원별 주문 조회 | ADMIN, USER |
| GET | `/orders/status/{status}` | 상태별 주문 조회 | ADMIN, USER |
| GET | `/orders/search?productName=상품명` | 상품명으로 주문 검색 | ADMIN, USER |
| GET | `/orders/period?startDate=...&endDate=...` | 기간별 주문 조회 | ADMIN, USER |
| GET | `/orders/recent` | 최근 주문 조회 | ADMIN, USER |
| PUT | `/orders/{id}` | 주문 정보 수정 | ADMIN |
| DELETE | `/orders/{id}` | 주문 삭제 | ADMIN |
| GET | `/orders/stats/total-amount/{memberId}` | 회원별 총 주문 금액 조회 | ADMIN, USER |
| GET | `/orders/stats/count/{status}` | 상태별 주문 수 조회 | ADMIN, USER |
| GET | `/orders/health` | 헬스 체크 | 인증 불필요 |
| GET | `/test/member/{id}` | Member Service 직접 호출 테스트 | ADMIN |
| GET | `/test/member-health` | Member Service 헬스 체크 | ADMIN |
| GET | `/test/circuit-breaker-status` | Circuit Breaker 상태 확인 | ADMIN |

### 6.2 API 예시

#### 주문 생성
```http
POST /orders
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "memberId": 1,
  "productName": "노트북",
  "quantity": 1,
  "unitPrice": 1500000.00,
  "orderMemo": "빠른 배송 부탁드립니다"
}
```

**응답**:
```json
{
  "id": 1,
  "memberId": 1,
  "memberName": "홍길동",
  "productName": "노트북",
  "quantity": 1,
  "unitPrice": 1500000.00,
  "totalAmount": 1500000.00,
  "status": "PENDING",
  "statusDescription": "대기중",
  "orderMemo": "빠른 배송 부탁드립니다",
  "createdAt": "2024-01-01 12:00:00",
  "updatedAt": "2024-01-01 12:00:00"
}
```

---

## 7. 실습 가이드

### 7.1 Order Service 실행

```bash
./gradlew order-service:bootRun
```

### 7.2 JWT 토큰 발급

먼저 Auth Service에서 JWT 토큰을 발급받습니다:

```bash
curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'
```

**응답에서 `accessToken` 값을 복사합니다.**

### 7.3 주문 생성

```bash
curl -X POST http://localhost:8082/orders \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": 1,
    "productName": "노트북",
    "quantity": 1,
    "unitPrice": 1500000.00,
    "orderMemo": "빠른 배송 부탁드립니다"
  }'
```

### 7.4 주문 조회

```bash
curl -X GET http://localhost:8082/orders/1 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 7.5 Member Service 연동 테스트

```bash
curl -X GET http://localhost:8082/test/member/1 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 7.6 Circuit Breaker 상태 확인

```bash
curl -X GET http://localhost:8082/test/circuit-breaker-status \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

**예상 응답**:
```json
{
  "circuitBreakerStatus": "CLOSED",
  "service": "member-service",
  "failureRate": "0.00%",
  "numberOfSuccessfulCalls": 5,
  "numberOfFailedCalls": 0,
  "numberOfNotPermittedCalls": 0,
  "numberOfBufferedCalls": 5,
  "statusDescription": "정상 상태 - 모든 요청이 통과됩니다"
}
```

### 7.7 Fallback 동작 테스트

#### 7.7.1 Member Service 중지

1. Member Service를 중지합니다.
2. 주문 생성 요청을 여러 번 보냅니다:

```bash
curl -X POST http://localhost:8082/orders \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": 1,
    "productName": "테스트 상품",
    "quantity": 1,
    "unitPrice": 10000.00
  }'
```

**예상 동작**:
- Member Service 장애로 예외 발생
- `@CircuitBreaker`가 실패를 카운트 (최소 5회 요청 후)
- 실패율이 50% 이상이면 Circuit Breaker가 OPEN 상태로 전환
- `@CircuitBreaker`가 자동으로 Fallback 메서드 호출
- Fallback 메서드가 실행되어 "알 수 없는 사용자"로 주문 생성

#### 7.7.2 Circuit Breaker 상태 확인

```bash
curl -X GET http://localhost:8082/test/circuit-breaker-status \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

**예상 응답**:
```json
{
  "circuitBreakerStatus": "OPEN",
  "service": "member-service",
  "failureRate": "100.00%",
  "numberOfSuccessfulCalls": 0,
  "numberOfFailedCalls": 5,
  "statusDescription": "Circuit Breaker 열림 - 요청이 차단되고 Fallback이 실행됩니다"
}
```

#### 7.7.3 Member Service 재시작

1. Member Service를 재시작합니다.
2. 10초 후 Circuit Breaker 상태 확인:

```bash
curl -X GET http://localhost:8082/test/circuit-breaker-status \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

**예상 응답**:
```json
{
  "circuitBreakerStatus": "HALF_OPEN",
  "service": "member-service",
  "statusDescription": "반열림 상태 - 제한된 요청만 허용하여 서비스 복구를 테스트합니다"
}
```

3. 테스트 요청이 성공하면 CLOSED 상태로 복귀합니다.

### 7.8 JWT 토큰 전파 확인

1. 주문 생성 요청:

```bash
curl -X POST http://localhost:8082/orders \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": 1,
    "productName": "테스트 상품",
    "quantity": 1,
    "unitPrice": 10000.00
  }'
```

2. Order Service 로그에서 다음을 확인:
   - Member Service 호출 시 Authorization 헤더가 포함되어 있는지
   - Member Service가 JWT 토큰을 검증하여 인증 성공하는지

---

## 8. 핵심 개념 정리

### 8.1 OpenFeign

| 개념 | 설명 |
|------|------|
| **@FeignClient** | OpenFeign 클라이언트 인터페이스 표시 |
| **@GetMapping, @PostMapping** | HTTP 메서드 지정 |
| **RequestInterceptor** | 요청 전처리 (JWT 토큰 전파 등) |
| **@CircuitBreaker** | Resilience4j 어노테이션으로 Circuit Breaker 및 Fallback 적용 |

### 8.2 Circuit Breaker

| 개념 | 설명 |
|------|------|
| **CLOSED** | 정상 상태 (요청 통과) |
| **OPEN** | Circuit Breaker 열림 (요청 차단, Fallback 실행) |
| **HALF_OPEN** | 반열림 상태 (테스트 요청 허용) |
| **Failure Rate** | 실패율 (실패한 요청 / 전체 요청) |

### 8.3 Fallback (Resilience4j @CircuitBreaker)

| 개념 | 설명 |
|------|------|
| **@CircuitBreaker** | Resilience4j 어노테이션으로 Circuit Breaker 패턴 적용 |
| **fallbackMethod** | Fallback 메서드 이름 지정 |
| **Fallback 메서드** | 원본 메서드와 같은 클래스에 있어야 함, Exception 파라미터 필요 |
| **자동 호출** | Circuit Breaker가 OPEN 상태일 때 자동으로 Fallback 메서드 호출 |
| **기본값 반환** | 서비스 장애 시에도 비즈니스 로직 계속 진행 |
| **메트릭 수집** | Circuit Breaker가 실패를 올바르게 카운트하여 메트릭 수집 |
| **MemberIntegrationService** | Member Service 통신 전담 서비스, Circuit Breaker 로직 포함 |
| **순환 참조 해결** | 별도 서비스로 분리하여 자기 주입(self-injection) 불필요 |

---

## 9. 다음 단계

Order Service를 이해했다면, 다음 단계로 진행하세요:

1. **Gateway Service**: API Gateway의 라우팅 및 인증 필터 학습

---

## 10. 실습 체크리스트

- [ ] Order Service 실행
- [ ] Auth Service에서 JWT 토큰 발급
- [ ] JWT 토큰으로 주문 생성
- [ ] 주문 조회, 수정, 삭제 테스트
- [ ] Member Service 연동 테스트
- [ ] Circuit Breaker 상태 확인
- [ ] Member Service 중지 후 Fallback 동작 확인
- [ ] Circuit Breaker OPEN 상태 확인
- [ ] Member Service 재시작 후 HALF_OPEN → CLOSED 전환 확인
- [ ] JWT 토큰 전파 확인
