# 🌐 5단계: Gateway Service 상세 가이드

이 문서는 MSA 프로젝트의 **Gateway Service**에 대한 상세한 설명입니다. Spring Cloud Gateway를 사용하여 모든 마이크로서비스의 단일 진입점(Single Entry Point) 역할을 수행하며, 라우팅, 인증, Circuit Breaker 등의 기능을 제공합니다.

---

## 📍 목차

1. [Gateway Service 개요](#1-gateway-service-개요)
2. [코드 구조 분석](#2-코드-구조-분석)
3. [라우팅 설정](#3-라우팅-설정)
4. [인증 필터 (JWT 토큰 검증)](#4-인증-필터-jwt-토큰-검증)
5. [Circuit Breaker 및 Fallback](#5-circuit-breaker-및-fallback)
6. [Rate Limiting](#6-rate-limiting)
7. [CORS 설정](#7-cors-설정)
8. [요청 로깅](#8-요청-로깅)
9. [API 엔드포인트](#9-api-엔드포인트)
10. [실습 가이드](#10-실습-가이드)

---

## 1. Gateway Service 개요

### 1.1 역할

**Gateway Service**는 모든 마이크로서비스의 단일 진입점(Single Entry Point) 역할을 담당합니다.

**주요 기능**:
- **라우팅**: 클라이언트 요청을 적절한 백엔드 서비스로 라우팅
- **로드 밸런싱**: Eureka를 통한 서비스 인스턴스 간 로드 밸런싱
- **인증**: JWT 토큰 검증 및 사용자 정보 추출
- **Circuit Breaker**: 백엔드 서비스 장애 시 Fallback 처리
- **Rate Limiting**: API 요청 제한 (IP별, 사용자별)
- **CORS**: Cross-Origin Resource Sharing 설정
- **요청 로깅**: 모든 요청에 대한 로깅

### 1.2 기술 스택

- **Spring Cloud Gateway**: 리액티브 웹 프레임워크 기반 API Gateway
- **Spring WebFlux**: 비동기 논블로킹 처리
- **Netflix Eureka**: 서비스 디스커버리 및 로드 밸런싱
- **Resilience4j**: Circuit Breaker 및 Rate Limiter 패턴
- **JJWT**: JWT 토큰 검증

### 1.3 서비스 포트

- **포트**: 8080
- **접속 URL**: http://localhost:8080
- **헬스 체크**: http://localhost:8080/health

### 1.4 Gateway 패턴의 장점

1. **단일 진입점**: 클라이언트는 하나의 URL만 알면 됨
2. **인증 중앙화**: 모든 인증 로직을 Gateway에서 처리
3. **라우팅 중앙화**: 서비스 간 통신 경로를 중앙에서 관리
4. **로드 밸런싱**: 자동으로 여러 인스턴스 간 부하 분산
5. **장애 격리**: Circuit Breaker로 장애 서비스 격리
6. **CORS 처리**: 브라우저 CORS 정책 중앙 관리

---

## 2. 코드 구조 분석

### 2.1 프로젝트 구조

```
gateway-service/
├── src/main/java/com/example/gateway/
│   ├── GatewayServiceApplication.java      # 애플리케이션 진입점
│   ├── config/
│   │   └── GatewayConfig.java            # Gateway 설정 및 커스텀 필터
│   ├── filter/
│   │   └── AuthenticationFilter.java     # JWT 토큰 검증 필터
│   ├── security/
│   │   └── JwtTokenValidator.java        # JWT 토큰 검증 로직
│   └── controller/
│       └── FallbackController.java        # Circuit Breaker Fallback 핸들러
└── src/main/resources/
    └── bootstrap.yml                      # Config Server 연결 설정
```

### 2.2 애플리케이션 진입점: GatewayServiceApplication

```java
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
```

**주요 어노테이션**:
- `@SpringBootApplication`: Spring Boot 애플리케이션
- `@EnableDiscoveryClient`: Eureka 클라이언트 활성화

### 2.3 Gateway 설정: GatewayConfig

```java
@Configuration
public class GatewayConfig {
    
    @Slf4j
    @Component
    public static class RequestLoggingGatewayFilterFactory 
            extends AbstractGatewayFilterFactory<RequestLoggingGatewayFilterFactory.Config> {
        
        @Override
        public GatewayFilter apply(Config config) {
            return (exchange, chain) -> {
                ServerHttpRequest request = exchange.getRequest();
                String clientIp = extractClientIp(request);
                
                log.info("[GATEWAY] {} {} -> Client IP: {}", 
                    request.getMethod(), 
                    request.getURI(), 
                    clientIp);
                
                return chain.filter(exchange);
            };
        }

        private String extractClientIp(ServerHttpRequest request) {
            // 1. X-Forwarded-For 헤더 확인
            String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                String firstIp = forwardedFor.split(",")[0].trim();
                return normalizeIp(firstIp);
            }

            // 2. X-Real-IP 헤더 확인
            String realIp = request.getHeaders().getFirst("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) {
                return normalizeIp(realIp);
            }

            // 3. RemoteAddress 사용
            var remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                String remoteIp = remoteAddress.getAddress().getHostAddress();
                return normalizeIp(remoteIp);
            }

            return "unknown";
        }

        private String normalizeIp(String ip) {
            if (ip == null || ip.isEmpty()) {
                return "unknown";
            }

            // IPv6 localhost를 IPv4로 변환
            if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                return "127.0.0.1";
            }

            return ip;
        }
    }
}
```

**기능**:
- 요청 로깅 필터 생성
- 클라이언트 IP 주소 추출 및 로깅 (우선순위: X-Forwarded-For → X-Real-IP → RemoteAddress)
- IPv6 localhost를 IPv4로 자동 변환 (`::1` → `127.0.0.1`)
- 요청 메서드 및 URI 로깅

### 2.4 JWT 토큰 검증: JwtTokenValidator

```java
@Component
public class JwtTokenValidator {
    private final SecretKey secretKey;

    public JwtTokenValidator(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

**기능**:
- JWT 토큰 유효성 검증 (서명, 만료 시간)
- JWT 토큰에서 Claims 추출
- Auth Service와 동일한 secret 사용

**주의사항**:
- `security.jwt.secret` 설정이 Auth Service와 동일해야 함
- Config Server의 `gateway-service.yml`에서 설정 관리

### 2.5 인증 필터: AuthenticationFilter

```java
@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
    
    private static final List<String> WHITELIST_PATH_PREFIXES = List.of(
            "/api/auth",
            "/auth-service",
            "/actuator",
            "/auth/health"
    );

    private final JwtTokenValidator jwtTokenValidator;

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // 1단계: 화이트리스트 경로 확인
            if (isWhitelisted(path) || isOptionsRequest(request)) {
                return chain.filter(exchange);
            }

            // 2단계: Authorization 헤더 확인
            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader == null || !authorizationHeader.toLowerCase().startsWith("bearer ")) {
                return handleUnauthorized(exchange);
            }

            // 3단계: JWT 토큰 추출 및 검증
            String token = authorizationHeader.substring(7);
            if (!jwtTokenValidator.isValid(token)) {
                return handleUnauthorized(exchange);
            }

            // 4단계: 사용자 정보 추출 및 헤더에 추가
            Claims claims = jwtTokenValidator.parseClaims(token);
            List<?> roleClaims = claims.get("roles", List.class);
            String roles = roleClaims == null
                    ? ""
                    : roleClaims.stream().map(Object::toString).collect(Collectors.joining(","));
            
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Authenticated-User", claims.getSubject())
                    .header("X-User-Roles", roles)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }
}
```

**동작 흐름**:
1. 화이트리스트 경로 확인 → 인증 불필요 시 통과
2. Authorization 헤더 확인 → 없거나 형식이 잘못되면 401 반환
3. JWT 토큰 추출 및 검증 → 유효하지 않으면 401 반환
4. 사용자 정보 추출 → 헤더에 추가하여 하위 서비스로 전달

**하위 서비스로 전달되는 헤더**:
- `X-Authenticated-User`: 인증된 사용자명
- `X-User-Roles`: 사용자 역할 목록 (쉼표로 구분)

### 2.6 Fallback 컨트롤러: FallbackController

```java
@RestController
public class FallbackController {

    @GetMapping("/fallback/member-service")
    public ResponseEntity<Map<String, Object>> memberServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Member Service is currently unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "member-service");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/fallback/order-service")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Order Service is currently unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "order-service");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/fallback/auth-service")
    public ResponseEntity<Map<String, Object>> authServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Auth Service is currently unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "auth-service");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "gateway-service");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
```

**기능**:
- Circuit Breaker Fallback 핸들러
- 서비스 장애 시 대체 응답 제공
- Gateway 헬스 체크 엔드포인트

---

## 3. 라우팅 설정

### 3.1 라우팅 설정 파일

라우팅 설정은 Config Server의 `gateway-service.yml`에 정의되어 있습니다.

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - RequestLogging      # 요청 로깅 필터
        - AuthenticationFilter # JWT 인증 필터
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        # Member Service 라우팅
        - id: member-service-api-exact
          uri: lb://member-service
          predicates:
            - Path=/api/members
          filters:
            - RewritePath=/api/members, /members
            - name: CircuitBreaker
              args:
                name: member-service
                fallbackUri: forward:/fallback/member-service
            
        - id: member-service-api-sub
          uri: lb://member-service
          predicates:
            - Path=/api/members/**
          filters:
            - RewritePath=/api/members/(?<path>.*), /members/$\{path}
            - name: CircuitBreaker
              args:
                name: member-service
                fallbackUri: forward:/fallback/member-service
            
        # Order Service 라우팅
        - id: order-service-api-exact
          uri: lb://order-service
          predicates:
            - Path=/api/orders
          filters:
            - RewritePath=/api/orders, /orders
            - name: CircuitBreaker
              args:
                name: order-service
                fallbackUri: forward:/fallback/order-service
            
        - id: order-service-api-sub
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - RewritePath=/api/orders/(?<path>.*), /orders/$\{path}
            - name: CircuitBreaker
              args:
                name: order-service
                fallbackUri: forward:/fallback/order-service
            
        # Auth Service 라우팅
        - id: auth-service-api
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
          filters:
            - RewritePath=/api/auth/(?<path>.*), /auth/$\{path}
            - name: CircuitBreaker
              args:
                name: auth-service
                fallbackUri: forward:/fallback/auth-service
```

### 3.2 라우팅 구성 요소

#### 3.2.1 Route ID
- 각 라우트를 식별하는 고유 ID
- 예: `member-service-api-exact`, `order-service-api-sub`

#### 3.2.2 URI
- `lb://service-name`: Eureka를 통한 로드 밸런싱
- `http://host:port`: 직접 URL 지정

#### 3.2.3 Predicates (조건)
- `Path=/api/members`: 정확한 경로 매칭
- `Path=/api/members/**`: 하위 경로 매칭
- 여러 조건을 AND로 결합 가능

#### 3.2.4 Filters (필터)
- `RewritePath`: 경로 재작성
  - `/api/members` → `/members`
  - `/api/members/{id}` → `/members/{id}`
- `RequestLogging`: 요청 로깅 필터 (모든 라우트에 적용, default-filters)
- `AuthenticationFilter`: JWT 인증 필터 (모든 라우트에 적용, default-filters)

**필터 실행 순서** (default-filters):
1. `RequestLogging`: 요청 로깅
2. `AuthenticationFilter`: JWT 토큰 검증
3. 라우트별 필터 (예: `RewritePath`)

### 3.3 라우팅 예시

#### Member Service 라우팅
```
클라이언트 요청: GET http://localhost:8080/api/members/1
                ↓
Gateway 라우팅: lb://member-service
                ↓
경로 재작성: /api/members/1 → /members/1
                ↓
Member Service: GET http://member-service:8081/members/1
```

#### Order Service 라우팅
```
클라이언트 요청: POST http://localhost:8080/api/orders
                ↓
Gateway 라우팅: lb://order-service
                ↓
경로 재작성: /api/orders → /orders
                ↓
Order Service: POST http://order-service:8082/orders
```

#### Auth Service 라우팅
```
클라이언트 요청: POST http://localhost:8080/api/auth/login
                ↓
Gateway 라우팅: lb://auth-service
                ↓
경로 재작성: /api/auth/login → /auth/login
                ↓
Auth Service: POST http://auth-service:8083/auth/login
```

### 3.4 Eureka 서비스 디스커버리

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
```

**기능**:
- Eureka에서 자동으로 서비스 인스턴스 발견
- `lb://service-name` 형식으로 로드 밸런싱
- 여러 인스턴스 간 자동 부하 분산

---

## 4. 인증 필터 (JWT 토큰 검증)

### 4.1 인증 필터 동작 흐름

```
1. 클라이언트 요청
   ↓
2. 화이트리스트 경로 확인
   ├─ 화이트리스트 → 인증 생략, 바로 통과
   └─ 일반 경로 → 다음 단계
   ↓
3. Authorization 헤더 확인
   ├─ 없음 또는 형식 오류 → 401 Unauthorized
   └─ 정상 → 다음 단계
   ↓
4. JWT 토큰 추출 (Bearer 제거)
   ↓
5. JWT 토큰 검증
   ├─ 유효하지 않음 → 401 Unauthorized
   └─ 유효함 → 다음 단계
   ↓
6. 사용자 정보 추출
   ├─ 사용자명 (subject)
   └─ 역할 목록 (roles)
   ↓
7. 헤더에 사용자 정보 추가
   ├─ X-Authenticated-User: {username}
   └─ X-User-Roles: {roles}
   ↓
8. 하위 서비스로 요청 전달
```

### 4.2 화이트리스트 경로

인증 없이 접근 가능한 경로:

```java
private static final List<String> WHITELIST_PATH_PREFIXES = List.of(
        "/api/auth",      // 원본 경로 (Gateway를 통한 접근)
        "/auth",          // RewritePath 후 경로
        "/auth-service",  // 인증 서비스 직접 접근
        "/actuator",      // Spring Boot Actuator 엔드포인트
        "/auth/health"    // 인증 서비스 헬스 체크
);
```

**주의사항**:
- `/api/auth`는 원본 경로 (클라이언트 요청 경로)
- `/auth`는 `RewritePath` 필터에 의해 재작성된 경로
- 필터 실행 순서에 따라 경로가 변경될 수 있으므로, 원본 경로와 재작성된 경로 모두 화이트리스트에 포함

**화이트리스트 경로 예시**:
- `POST /api/auth/login` → 인증 불필요
- `GET /actuator/health` → 인증 불필요
- `GET /api/members/1` → 인증 필요 (JWT 토큰 필수)

### 4.3 JWT 토큰 검증

**검증 항목**:
1. **토큰 서명 검증**: SecretKey를 사용한 서명이 올바른지 확인
2. **토큰 만료 시간 확인**: 토큰이 만료되지 않았는지 확인
3. **토큰 형식 검증**: JWT 형식이 올바른지 확인

**검증 실패 시**:
- HTTP 401 Unauthorized 응답
- 로그에 경고 메시지 기록

### 4.4 사용자 정보 전달

인증 성공 시 하위 서비스로 전달되는 헤더:

```java
ServerHttpRequest mutatedRequest = request.mutate()
        .header("X-Authenticated-User", claims.getSubject())  // 사용자명
        .header("X-User-Roles", roles)                        // 역할 목록
        .build();
```

**하위 서비스에서 사용**:
- Member Service, Order Service는 이 헤더를 통해 인증된 사용자 정보 확인
- Spring Security의 `@PreAuthorize` 등에서 활용 가능

---

## 5. Circuit Breaker 및 Fallback

### 5.1 Circuit Breaker 설정

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
    instances:
      member-service:
        baseConfig: default
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
      order-service:
        baseConfig: default
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
      auth-service:
        baseConfig: default
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

**설정 항목**:
- `slidingWindowSize`: 슬라이딩 윈도우 크기 (10개 요청)
- `failureRateThreshold`: 실패율 임계값 (50%)
- `waitDurationInOpenState`: OPEN 상태 유지 시간 (10초)
- `minimumNumberOfCalls`: Circuit Breaker 동작을 위한 최소 요청 수 (5개)
- `permittedNumberOfCallsInHalfOpenState`: HALF_OPEN 상태에서 허용되는 요청 수 (3개)

### 5.2 Fallback 핸들러

Circuit Breaker가 OPEN 상태일 때 Fallback 핸들러가 실행됩니다.

**Fallback 응답 예시**:
```json
{
  "error": "Member Service is currently unavailable",
  "message": "Please try again later",
  "timestamp": "2024-01-01T10:00:00",
  "service": "member-service"
}
```

**Fallback 엔드포인트**:
- `/fallback/member-service`: Member Service Fallback
- `/fallback/order-service`: Order Service Fallback

### 5.3 Circuit Breaker 동작 흐름

```
1. 정상 상태 (CLOSED)
   - 모든 요청이 백엔드 서비스로 전달
   ↓
2. 실패율 증가
   - 10개 요청 중 실패율이 50% 이상
   ↓
3. Circuit Breaker 열림 (OPEN)
   - 요청이 차단되고 Fallback 실행
   - 10초 동안 OPEN 상태 유지
   ↓
4. 반열림 상태 (HALF_OPEN)
   - 제한된 요청만 허용하여 서비스 복구 테스트
   ↓
5. 복구 확인
   - 테스트 요청 성공 → CLOSED 상태로 복귀
   - 테스트 요청 실패 → OPEN 상태로 복귀
```

---

## 6. Rate Limiting

### 6.1 Rate Limiting 개요

**Rate Limiting**은 API 요청의 빈도를 제한하여 시스템을 보호하고 DDoS 공격을 방어하는 기능입니다.

**주요 기능**:
- **IP별 Rate Limiting**: 클라이언트 IP 주소별로 요청 제한
- **사용자별 Rate Limiting**: 인증된 사용자별로 요청 제한 (JWT 토큰 기반)
- **Rate Limit 초과 시 429 Too Many Requests 응답**

**Rate Limiting 전략**:
- **기본 제한**: 초당 10개 요청 (IP별)
- **인증된 사용자**: 초당 50개 요청 (사용자별)
- **특정 엔드포인트**: 별도 제한 설정 가능

### 6.2 RateLimiter 설정

**설정 파일**: `config-service/src/main/resources/config-repo/gateway-service.yml`

```yaml
resilience4j:
  ratelimiter:
    configs:
      default:
        limitForPeriod: 10  # 초당 10개 요청
        limitRefreshPeriod: 1s  # 1초마다 리프레시
        timeoutDuration: 0  # 타임아웃 없음
        subscribeToEvents: true  # 이벤트 구독 활성화
      ip-based:
        baseConfig: default
        limitForPeriod: 10  # IP별: 초당 10개 요청
      user-based:
        baseConfig: default
        limitForPeriod: 50  # 사용자별: 초당 50개 요청
    instances:
      ip-rate-limiter:
        baseConfig: ip-based
      user-rate-limiter:
        baseConfig: user-based
```

**설정 항목**:
- `limitForPeriod`: 기간 내 허용되는 최대 요청 수
- `limitRefreshPeriod`: 제한이 리프레시되는 주기 (1초)
- `timeoutDuration`: 요청이 대기할 수 있는 최대 시간 (0 = 타임아웃 없음)
- `subscribeToEvents`: Rate Limiter 이벤트 구독 활성화

### 6.3 Rate Limiting 필터

**필터 클래스**: `RateLimitingFilter`

**필터 설정**: `gateway-service.yml`의 `default-filters`에 추가되어 모든 라우트에 자동으로 적용됩니다.

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - RequestLogging
        - RateLimitingFilter      # Rate Limiting 필터
        - AuthenticationFilter
```

**Rate Limiting 키 결정 로직**:
1. **인증된 사용자 확인**: JWT 토큰이 유효하면 사용자명 사용 (`user:{username}`)
2. **IP 주소 사용**: 인증되지 않은 요청은 IP 주소 사용 (`ip:{ip-address}`)

**Rate Limiting 제외 경로**:
- `/actuator`: Actuator 엔드포인트는 Rate Limiting 제외

### 6.4 Rate Limit 초과 시 응답

**HTTP 상태 코드**: `429 Too Many Requests`

**응답 헤더**:
```
X-RateLimit-Exceeded: true
X-RateLimit-Key: ip:127.0.0.1
```

**응답 예시**:
```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Exceeded: true
X-RateLimit-Key: ip:127.0.0.1
```

### 6.5 Rate Limiting 동작 흐름

```
1. 요청 수신
   ↓
2. Rate Limiting 키 결정
   - 인증된 사용자: user:{username}
   - 비인증 요청: ip:{ip-address}
   ↓
3. RateLimiter 인스턴스 조회 또는 생성
   - ip-rate-limiter: IP별 제한 (초당 10개)
   - user-rate-limiter: 사용자별 제한 (초당 50개)
   ↓
4. Rate Limit 확인
   - 허용: 다음 필터로 전달
   - 초과: 429 응답 반환
```

### 6.6 Rate Limiting 모니터링

**Actuator 엔드포인트**:
- `/actuator/ratelimiters`: RateLimiter 상태 확인
- `/actuator/ratelimiterevents`: RateLimiter 이벤트 확인

**설정**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: ratelimiters,ratelimiterevents
```

**RateLimiter 상태 확인**:
```http
GET http://localhost:8080/actuator/ratelimiters
```

**응답 예시**:
```json
{
  "ip-rate-limiter": {
    "state": "ACTIVE",
    "availablePermissions": 8,
    "numberOfWaitingThreads": 0
  },
  "user-rate-limiter": {
    "state": "ACTIVE",
    "availablePermissions": 45,
    "numberOfWaitingThreads": 0
  }
}
```

### 6.7 Rate Limiting 테스트

**테스트 시나리오**:
1. **정상 요청**: Rate Limit 내에서 요청 시 정상 응답
2. **Rate Limit 초과**: 초당 제한을 초과하는 요청 시 429 응답
3. **Rate Limit 리셋**: 1초 후 제한이 리셋되어 다시 요청 가능

**테스트 예시**:
```bash
# 10개의 요청을 빠르게 전송
for i in {1..12}; do
  curl -X GET http://localhost:8080/api/members/1
  echo ""
done

# 11번째 요청부터 429 응답
```

**예상 결과**:
- 1-10번째 요청: 정상 응답 (200 OK)
- 11-12번째 요청: 429 Too Many Requests

### 6.8 Rate Limiting 고급 설정

**서비스별 다른 Rate Limit 설정**:
특정 서비스에 대해 다른 Rate Limit을 적용하려면 라우트별로 필터를 추가할 수 있습니다.

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: member-service-api
          uri: lb://member-service
          predicates:
            - Path=/api/members/**
          filters:
            - RateLimitingFilter  # 특정 라우트에만 적용
```

**사용자별 Rate Limit 비활성화**:
IP별 Rate Limiting만 사용하려면 설정에서 `userBasedRateLimitEnabled: false`로 설정할 수 있습니다.

---

## 7. CORS 설정

### 6.1 글로벌 CORS 설정

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origin-patterns: "*"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowed-headers: "*"
            allow-credentials: true
```

**설정 항목**:
- `allowed-origin-patterns: "*"`: 모든 Origin 허용
- `allowed-methods`: 허용할 HTTP 메서드
- `allowed-headers: "*"`: 모든 헤더 허용
- `allow-credentials: true`: 인증 정보 포함 허용

### 6.2 CORS 동작

**브라우저에서의 CORS 요청**:
1. 브라우저가 OPTIONS 요청으로 Preflight 체크
2. Gateway가 CORS 헤더와 함께 응답
3. 브라우저가 실제 요청 전송
4. Gateway가 CORS 헤더와 함께 응답

**CORS 헤더 예시**:
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
```

---

## 8. 요청 로깅

### 7.1 로깅 필터

`RequestLoggingGatewayFilterFactory`가 모든 요청을 로깅합니다.

**필터 설정**:
로깅 필터는 `gateway-service.yml`의 `default-filters`에 추가되어 모든 라우트에 자동으로 적용됩니다.

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - RequestLogging      # 로깅 필터 (먼저 실행)
        - AuthenticationFilter # 인증 필터 (그 다음 실행)
```

**필터 이름 규칙**:
- 클래스 이름: `RequestLoggingGatewayFilterFactory`
- 설정 파일에서 사용: `RequestLogging` (GatewayFilterFactory 접미사 제거)
- Spring Cloud Gateway가 자동으로 `GatewayFilterFactory` 접미사를 제거하여 필터 이름 생성

**로깅 정보**:
- HTTP 메서드 (GET, POST 등)
- 요청 URI
- 클라이언트 IP 주소

**클라이언트 IP 추출 우선순위**:
1. `X-Forwarded-For` 헤더 (프록시/로드밸런서 환경에서 사용, 첫 번째 IP만 추출)
2. `X-Real-IP` 헤더 (Nginx 등에서 사용)
3. `RemoteAddress` (직접 연결 시 사용)

**IP 주소 정규화**:
- IPv6 localhost를 IPv4로 자동 변환
  - `::1` → `127.0.0.1`
  - `0:0:0:0:0:0:0:1` → `127.0.0.1`

**로그 예시**:
```
[GATEWAY] GET http://localhost:8080/api/members/1 -> Client IP: 127.0.0.1
[GATEWAY] POST http://localhost:8080/api/orders -> Client IP: 127.0.0.1
```

**필터 실행 순서**:
1. `RequestLogging`: 요청 로깅 (먼저 실행)
2. `AuthenticationFilter`: JWT 토큰 검증 (그 다음 실행)

### 7.2 로깅 레벨 설정

```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty.http.client: DEBUG
```

**로깅 레벨**:
- `DEBUG`: 상세한 라우팅 및 필터 정보
- `INFO`: 기본 요청 로깅

---

## 9. API 엔드포인트

### 8.1 Gateway 헬스 체크

```http
GET http://localhost:8080/health
```

**응답 예시**:
```json
{
  "status": "UP",
  "service": "gateway-service",
  "timestamp": "2024-01-01T10:00:00"
}
```

### 8.2 Fallback 엔드포인트

#### Member Service Fallback
```http
GET http://localhost:8080/fallback/member-service
```

#### Order Service Fallback
```http
GET http://localhost:8080/fallback/order-service
```

#### Auth Service Fallback
```http
GET http://localhost:8080/fallback/auth-service
```

### 8.3 Circuit Breaker 상태 확인 엔드포인트

#### Circuit Breaker 헬스 체크 (전체 헬스 체크에 포함)
```http
GET http://localhost:8080/actuator/health
```

**응답 예시** (Circuit Breaker 정보 포함):
```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "member-service": {
          "status": "CIRCUIT_OPEN",
          "details": {
            "failureRate": "100.0%",
            "state": "OPEN"
          }
        }
      }
    }
  }
}
```

**참고**: Circuit Breaker 정보가 헬스 체크에 포함되려면 `management.health.circuitbreakers.enabled: true` 설정이 필요합니다.

#### Circuit Breaker 상세 정보
```http
GET http://localhost:8080/actuator/circuitbreakers
```

**응답 예시**:
```json
{
  "circuitBreakers": [
    {
      "name": "member-service",
      "state": "OPEN",
      "failureRate": 100.0,
      "slowCallRate": 0.0,
      "bufferedCalls": 10,
      "failedCalls": 10,
      "successfulCalls": 0,
      "notPermittedCalls": 5,
      "slowFailedCalls": 0,
      "slowSuccessfulCalls": 0
    }
  ]
}
```

#### Circuit Breaker 이벤트 조회
```http
GET http://localhost:8080/actuator/circuitbreakerevents/member-service
```

**Circuit Breaker 상태 값**:
- `CLOSED`: 정상 상태 - 모든 요청이 통과됩니다
- `OPEN`: Circuit Breaker 열림 - 요청이 차단되고 Fallback 실행됩니다
- `HALF_OPEN`: 반열림 상태 - 제한된 요청만 허용하여 서비스 복구를 테스트합니다
- `DISABLED`: 비활성화됨 - Circuit Breaker가 작동하지 않습니다

### 8.4 라우팅된 엔드포인트

#### Auth Service
```http
POST http://localhost:8080/api/auth/login
POST http://localhost:8080/api/auth/register
```

#### Member Service
```http
GET    http://localhost:8080/api/members
GET    http://localhost:8080/api/members/{id}
POST   http://localhost:8080/api/members
PUT    http://localhost:8080/api/members/{id}
DELETE http://localhost:8080/api/members/{id}
```

#### Order Service
```http
GET    http://localhost:8080/api/orders
GET    http://localhost:8080/api/orders/{id}
POST   http://localhost:8080/api/orders
PUT    http://localhost:8080/api/orders/{id}
DELETE http://localhost:8080/api/orders/{id}
```

---

## 10. 실습 가이드

### 9.1 Gateway Service 실행

```bash
# Gateway Service 실행
./gradlew gateway-service:bootRun
```

**확인 사항**:
- 포트 8080에서 실행
- Eureka에 등록됨
- Config Server에서 설정 로드

### 9.2 기본 라우팅 테스트

#### 1. Auth Service 라우팅 테스트
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**예상 결과**:
- JWT 토큰 반환
- 인증 불필요 (화이트리스트 경로)

**참고**: 
- Auth Service의 초기 사용자는 **DataInitializer.java**에서 생성됩니다.
- 사용 가능한 계정:
  - `admin` / `password123` (역할: ADMIN)
  - `member` / `password123` (역할: USER)
- Member Service의 `data.sql`에 있는 `admin` / `admin123`은 회원 정보용이며, Auth Service 인증과는 별개입니다.

#### 2. Member Service 라우팅 테스트 (인증 필요)
```http
GET http://localhost:8080/api/members/1
Authorization: Bearer {JWT_토큰}
```

**예상 결과**:
- Member Service로 라우팅
- 경로 재작성: `/api/members/1` → `/members/1`
- 인증 필터 통과

#### 3. Order Service 라우팅 테스트 (인증 필요)
```http
POST http://localhost:8080/api/orders
Authorization: Bearer {JWT_토큰}
Content-Type: application/json

{
  "memberId": 1,
  "productName": "테스트 상품",
  "quantity": 1,
  "unitPrice": 10000.00
}
```

**예상 결과**:
- Order Service로 라우팅
- 경로 재작성: `/api/orders` → `/orders`
- 인증 필터 통과

### 9.3 인증 필터 테스트

#### 1. 인증 없이 요청 (실패 예상)
```http
GET http://localhost:8080/api/members/1
```

**예상 결과**:
- HTTP 401 Unauthorized
- 로그에 "Missing or invalid Authorization header" 메시지

#### 2. 잘못된 토큰으로 요청 (실패 예상)
```http
GET http://localhost:8080/api/members/1
Authorization: Bearer invalid-token
```

**예상 결과**:
- HTTP 401 Unauthorized
- 로그에 "Invalid JWT token" 메시지

#### 3. 유효한 토큰으로 요청 (성공 예상)
```http
GET http://localhost:8080/api/members/1
Authorization: Bearer {유효한_JWT_토큰}
```

**예상 결과**:
- HTTP 200 OK
- Member Service 응답 반환

### 9.4 Circuit Breaker 테스트

#### 1. Member Service 중단
```bash
# Member Service 프로세스 종료
netstat -ano | findstr :8081
taskkill /PID {PID번호} /F
```

#### 2. Circuit Breaker 동작 확인
```http
GET http://localhost:8080/api/members/1
Authorization: Bearer {JWT_토큰}
```

**예상 결과**:
- Circuit Breaker가 OPEN 상태로 전환
- Fallback 응답 반환
- HTTP 503 Service Unavailable

#### 3. Circuit Breaker 상태 확인

**Circuit Breaker 상태 조회 (권장)**:
```http
GET http://localhost:8080/actuator/circuitbreakers
```

**또는 전체 헬스 체크에서 확인**:
```http
GET http://localhost:8080/actuator/health
```

**예상 응답** (`/actuator/circuitbreakers`):
```json
{
  "circuitBreakers": [
    {
      "name": "member-service",
      "state": "OPEN",
      "failureRate": 100.0,
      "slowCallRate": 0.0,
      "bufferedCalls": 10,
      "failedCalls": 10,
      "successfulCalls": 0,
      "notPermittedCalls": 5,
      "slowFailedCalls": 0,
      "slowSuccessfulCalls": 0
    }
  ]
}
```

**Circuit Breaker 상세 정보 조회**:
```http
GET http://localhost:8080/actuator/circuitbreakers
```

**예상 응답**:
```json
{
  "circuitBreakers": [
    {
      "name": "member-service",
      "state": "OPEN",
      "failureRate": 100.0,
      "slowCallRate": 0.0,
      "bufferedCalls": 10,
      "failedCalls": 10,
      "successfulCalls": 0,
      "notPermittedCalls": 5,
      "slowFailedCalls": 0,
      "slowSuccessfulCalls": 0
    }
  ]
}
```

**Circuit Breaker 이벤트 조회**:
```http
GET http://localhost:8080/actuator/circuitbreakerevents/member-service
```

**Circuit Breaker 상태 값**:
- `CLOSED`: 정상 상태 - 모든 요청이 통과됩니다
- `OPEN`: Circuit Breaker 열림 - 요청이 차단되고 Fallback 실행됩니다
- `HALF_OPEN`: 반열림 상태 - 제한된 요청만 허용하여 서비스 복구를 테스트합니다
- `DISABLED`: 비활성화됨 - Circuit Breaker가 작동하지 않습니다

### 9.5 CORS 테스트

#### 방법 1: Postman 또는 HTTP 클라이언트 사용 (권장)

브라우저 콘솔에서 직접 `fetch`를 실행하면 Content Security Policy (CSP) 제한으로 인해 오류가 발생할 수 있습니다. Postman이나 HTTP 클라이언트를 사용하는 것을 권장합니다.

```http
GET http://localhost:8080/api/members/1
Authorization: Bearer {JWT_토큰}
```

#### 방법 2: HTML 페이지를 통한 테스트

브라우저에서 테스트하려면 별도의 HTML 페이지를 만들어서 사용하세요:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Gateway API Test</title>
</head>
<body>
    <h1>Gateway API Test</h1>
    <button onclick="testAPI()">Test API</button>
    <pre id="result"></pre>

    <script>
        async function testAPI() {
            const token = 'YOUR_JWT_TOKEN_HERE';
            try {
                const response = await fetch('http://localhost:8080/api/members/1', {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
                const data = await response.json();
                document.getElementById('result').textContent = JSON.stringify(data, null, 2);
            } catch (error) {
                document.getElementById('result').textContent = 'Error: ' + error.message;
            }
        }
    </script>
</body>
</html>
```

#### 방법 3: 브라우저 콘솔에서 테스트 (CSP 제한 주의)

브라우저 콘솔에서 직접 실행할 때는 Content Security Policy 제한이 있을 수 있습니다. 

**CSP 오류 해결 방법**:
1. 브라우저 확장 프로그램 비활성화 (특히 보안 관련 확장)
2. 새 시크릿 창에서 테스트
3. 브라우저 개발자 도구의 Network 탭에서 직접 요청 확인

```javascript
fetch('http://localhost:8080/api/members/1', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer {JWT_토큰}'
  }
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

**예상 결과**:
- CORS 헤더 포함
- 정상 요청 시 200 OK 응답
- CSP 제한이 없으면 브라우저에서 정상 요청 가능

### 9.6 로깅 확인

Gateway Service 로그에서 다음 정보 확인:
- 요청 메서드 및 URI
- 클라이언트 IP 주소
- 인증 필터 동작 (화이트리스트 통과, 토큰 검증 등)
- 라우팅 정보

---

## 10. 주요 개념 정리

### 10.1 API Gateway 패턴

| 개념 | 설명 |
|------|------|
| **Single Entry Point** | 모든 클라이언트 요청의 단일 진입점 |
| **라우팅** | 요청을 적절한 백엔드 서비스로 전달 |
| **로드 밸런싱** | 여러 인스턴스 간 부하 분산 |
| **인증 중앙화** | 모든 인증 로직을 Gateway에서 처리 |
| **장애 격리** | Circuit Breaker로 장애 서비스 격리 |

### 10.2 Spring Cloud Gateway

| 개념 | 설명 |
|------|------|
| **Route** | 라우팅 규칙 (URI, Predicates, Filters) |
| **Predicate** | 요청 매칭 조건 (Path, Method 등) |
| **Filter** | 요청/응답 처리 (인증, 로깅, 경로 재작성 등) |
| **Global Filter** | 모든 라우트에 적용되는 필터 |
| **Gateway Filter** | 특정 라우트에 적용되는 필터 |

### 10.3 JWT 인증

| 개념 | 설명 |
|------|------|
| **JWT 토큰** | JSON Web Token, 인증 정보를 포함하는 토큰 |
| **Bearer 토큰** | `Authorization: Bearer {token}` 형식 |
| **토큰 검증** | 서명 검증, 만료 시간 확인 |
| **Claims** | 토큰에 포함된 정보 (사용자명, 역할 등) |
| **Secret Key** | 토큰 서명에 사용되는 비밀키 |

---

## 11. 다음 단계

Gateway Service를 이해했다면, 다음 단계로 진행하세요:

1. **전체 시스템 통합 테스트**: 모든 서비스를 연동하여 테스트
2. **성능 테스트**: 로드 밸런싱 및 Circuit Breaker 동작 확인
3. **보안 강화**: Rate Limiting, IP 화이트리스트 등 추가

---

## 12. 실습 체크리스트

- [ ] Gateway Service 실행
- [ ] Eureka에 등록 확인
- [ ] Config Server에서 설정 로드 확인
- [ ] Auth Service 라우팅 테스트 (인증 불필요)
- [ ] Member Service 라우팅 테스트 (인증 필요)
- [ ] Order Service 라우팅 테스트 (인증 필요)
- [ ] 인증 필터 동작 확인 (화이트리스트, 토큰 검증)
- [ ] Circuit Breaker 동작 확인
- [ ] Rate Limiting 동작 확인 (IP별, 사용자별)
- [ ] Rate Limit 초과 시 429 응답 확인
- [ ] CORS 설정 확인
- [ ] 요청 로깅 확인
- [ ] Fallback 핸들러 동작 확인

---

## 13. 문제 해결

### 13.1 Gateway가 시작되지 않음

**원인**:
- Config Server 연결 실패
- Eureka 연결 실패
- 포트 충돌

**해결 방법**:
1. Config Server 실행 확인
2. Eureka 실행 확인
3. 포트 8080 사용 가능 여부 확인

### 13.2 라우팅이 작동하지 않음

**원인**:
- Eureka에 서비스가 등록되지 않음
- 라우팅 설정 오류
- 서비스 이름 불일치

**해결 방법**:
1. Eureka 대시보드에서 서비스 등록 확인
2. `gateway-service.yml` 라우팅 설정 확인
3. 서비스 이름 일치 확인

### 13.3 인증 필터가 작동하지 않음

**원인**:
- JWT secret 불일치
- 토큰 형식 오류
- 필터 순서 문제

**해결 방법**:
1. `security.jwt.secret` 설정 확인 (Auth Service와 동일해야 함)
2. 토큰 형식 확인 (`Bearer {token}`)
3. 필터 설정 확인

### 13.4 로깅 필터가 작동하지 않음

**원인**:
- `default-filters`에 `RequestLogging` 필터가 추가되지 않음
- 필터 이름 오류 (예: `RequestLoggingGatewayFilterFactory` 대신 `RequestLogging` 사용)

**해결 방법**:
1. `gateway-service.yml`의 `default-filters`에 `RequestLogging` 추가 확인
2. 필터 이름 확인: 클래스 이름에서 `GatewayFilterFactory` 접미사 제거
   - 클래스: `RequestLoggingGatewayFilterFactory`
   - 설정: `RequestLogging`
3. Gateway Service 재시작 후 로그 확인
4. 로깅 레벨 확인: `logging.level.com.example.gateway.config.GatewayConfig$RequestLoggingGatewayFilterFactory: INFO`

### 13.5 브라우저에서 CORS 테스트 시 CSP 오류 발생

**원인**:
- 브라우저 콘솔에서 직접 `fetch` 실행 시 Content Security Policy (CSP) 제한
- 브라우저 확장 프로그램의 CSP 설정
- Chrome의 내부 페이지 CSP 정책

**오류 메시지**:
```
Failed to fetch. Refused to connect because it violates the document's Content Security Policy.
```

**해결 방법**:
1. **Postman 또는 HTTP 클라이언트 사용 (권장)**
   - 브라우저 콘솔 대신 Postman, Insomnia, HTTPie 등 사용
   - CSP 제한 없이 테스트 가능

2. **HTML 페이지를 통한 테스트**
   - 별도의 HTML 파일을 만들어서 테스트
   - `file://` 프로토콜로 열거나 로컬 웹 서버에서 실행

3. **브라우저 확장 프로그램 비활성화**
   - 보안 관련 브라우저 확장 프로그램 일시 비활성화
   - 새 시크릿 창에서 테스트

4. **Network 탭에서 확인**
   - 브라우저 개발자 도구의 Network 탭에서 요청/응답 확인
   - CORS 헤더가 정상적으로 포함되어 있는지 확인

**참고**: Gateway Service의 CORS 설정은 정상입니다. 문제는 브라우저의 CSP 정책 때문입니다.

---

이 가이드를 통해 Gateway Service의 구조와 동작 방식을 이해할 수 있습니다. 다음 단계로 전체 시스템 통합 테스트를 진행하세요!

