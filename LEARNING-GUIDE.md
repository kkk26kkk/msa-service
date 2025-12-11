# 📚 MSA 프로젝트 학습 가이드

이 문서는 MSA 프로젝트의 소스 코드를 체계적으로 학습하기 위한 가이드입니다.

## 🎯 학습 목표

1. MSA 아키텍처의 기본 개념 이해
2. Spring Cloud 기술 스택 활용 방법 습득
3. 서비스 간 통신 및 장애 처리 패턴 이해
4. JWT 기반 인증/인가 구현 방법 이해

## 📖 학습 순서

### 1단계: 인프라 서비스 이해 (기반 다지기)

#### 1.1 Discovery Service
**목적**: 서비스 레지스트리와 디스커버리 메커니즘 이해

**확인 파일**:
- `discovery-service/src/main/java/com/example/discovery/DiscoveryServiceApplication.java`
- `discovery-service/src/main/resources/application.yml`

**학습 포인트**:
- `@EnableEurekaServer` 어노테이션의 역할
- Eureka Server 설정 (`eureka.client.register-with-eureka`, `eureka.client.fetch-registry`)
- 서비스 등록 및 발견 메커니즘

**실습**:
1. Discovery Service 실행 후 http://localhost:8761 접속
2. 다른 서비스들이 등록되는 과정 관찰

---

#### 1.2 Config Service
**목적**: 중앙 집중식 설정 관리 이해

**확인 파일**:
- `config-service/src/main/java/com/example/config/ConfigServiceApplication.java`
- `config-service/src/main/resources/application.yml`
- `config-service/src/main/resources/config-repo/` (모든 설정 파일)

**학습 포인트**:
- `@EnableConfigServer` 어노테이션의 역할
- Native 프로파일을 통한 로컬 파일 시스템 설정 관리
- 각 서비스별 설정 파일 구조 (`gateway-service.yml`, `member-service.yml`, 등)
- `bootstrap.yml`의 역할 (Config Server 연결)

**실습**:
1. Config Service 실행 후 http://localhost:8888/{service-name}/{profile} 접속
2. 각 서비스의 설정 파일 내용 확인

---

### 2단계: 인증 서비스 이해 (보안 기초)

#### 2.1 Auth Service
**목적**: JWT 기반 인증/인가 구현 방법 이해

**확인 파일 순서**:

1. **애플리케이션 진입점**
   - `auth-service/src/main/java/com/example/auth/AuthServiceApplication.java`
   - Spring Boot 애플리케이션 기본 구조

2. **엔티티**
   - `auth-service/src/main/java/com/example/auth/entity/AuthUser.java`
   - 사용자 정보 및 역할 관리

3. **JWT 토큰 생성/검증**
   - `auth-service/src/main/java/com/example/auth/service/JwtTokenProvider.java`
   - JWT 토큰 생성, 파싱, 검증 로직
   - `roles` 클레임 포함 방법

4. **비즈니스 로직**
   - `auth-service/src/main/java/com/example/auth/service/AuthService.java`
   - 사용자 등록, 로그인 처리
   - `auth-service/src/main/java/com/example/auth/service/AuthUserDetailsService.java`
   - Spring Security UserDetailsService 구현

5. **보안 설정**
   - `auth-service/src/main/java/com/example/auth/config/SecurityConfig.java`
   - Spring Security 설정
   - `permitAll()` 경로 설정
   - BCryptPasswordEncoder 설정

6. **API 엔드포인트**
   - `auth-service/src/main/java/com/example/auth/controller/AuthController.java`
   - 로그인 API (`/auth/login`)
   - 헬스 체크 API (`/auth/health`)

**학습 포인트**:
- JWT 토큰 생성 및 검증
- Spring Security 설정
- BCrypt를 통한 비밀번호 암호화
- 역할 기반 권한 관리 (`ROLE_USER`, `ROLE_ADMIN`)
- `roles` 클레임을 배열로 포함하는 방법

**실습**:
1. Auth Service 실행
2. `POST /auth/login` 호출하여 JWT 토큰 발급
3. 발급된 토큰을 https://jwt.io 에서 디코딩하여 내용 확인

---

### 3단계: 비즈니스 서비스 이해 (단순 CRUD)

#### 3.1 Member Service
**목적**: Spring Data JPA를 활용한 CRUD 서비스 구현 방법 이해

**확인 파일 순서**:

1. **애플리케이션 진입점**
   - `member-service/src/main/java/com/example/member/MemberServiceApplication.java`

2. **엔티티**
   - `member-service/src/main/java/com/example/member/entity/Member.java`
   - JPA 엔티티 매핑
   - Lombok 활용

3. **리포지토리**
   - `member-service/src/main/java/com/example/member/repository/MemberRepository.java`
   - Spring Data JPA 인터페이스
   - 커스텀 쿼리 메서드

4. **비즈니스 로직**
   - `member-service/src/main/java/com/example/member/service/MemberService.java`
   - CRUD 작업
   - 비즈니스 규칙 검증

5. **API 엔드포인트**
   - `member-service/src/main/java/com/example/member/controller/MemberController.java`
   - RESTful API 설계
   - 페이징 및 정렬

6. **보안 설정**
   - `member-service/src/main/java/com/example/member/config/SecurityConfig.java`
   - JWT 토큰 기반 인증
   - `JwtAuthenticationConverter`를 통한 `roles` 클레임 매핑
   - OAuth2 Resource Server 설정

7. **예외 처리**
   - `member-service/src/main/java/com/example/member/exception/GlobalExceptionHandler.java`
   - `@ControllerAdvice`를 통한 전역 예외 처리

**학습 포인트**:
- Spring Data JPA 활용
- Bean Validation (`@Valid`, `@NotNull`, 등)
- JWT 토큰 기반 인증 통합
- `JwtAuthenticationConverter`를 통한 권한 매핑
- 예외 처리 패턴

**실습**:
1. Member Service 실행
2. JWT 토큰을 사용하여 `GET /members` 호출
3. 회원 생성, 수정, 삭제 테스트

---

### 4단계: 통합 서비스 이해 (서비스 간 통신)

#### 4.1 Order Service
**목적**: OpenFeign을 통한 서비스 간 통신 및 Circuit Breaker 패턴 이해

**확인 파일 순서**:

1. **애플리케이션 진입점**
   - `order-service/src/main/java/com/example/order/OrderServiceApplication.java`
   - `@EnableFeignClients` 어노테이션

2. **엔티티**
   - `order-service/src/main/java/com/example/order/entity/Order.java`
   - 주문 정보 및 상태 관리

3. **Feign Client**
   - `order-service/src/main/java/com/example/order/client/MemberServiceClient.java`
   - OpenFeign 인터페이스 정의
   - `@FeignClient` 어노테이션 설정
   - Fallback 클래스 지정

4. **Fallback 구현**
   - `order-service/src/main/java/com/example/order/client/MemberServiceClientFallback.java`
   - 서비스 장애 시 대체 로직

5. **Feign 설정**
   - `order-service/src/main/java/com/example/order/config/FeignClientConfig.java`
   - JWT 토큰 전파 (`RequestInterceptor`)
   - `SecurityContext`에서 토큰 추출

6. **비즈니스 로직**
   - `order-service/src/main/java/com/example/order/service/OrderService.java`
   - Member Service 호출
   - Circuit Breaker 적용 (`@CircuitBreaker`)

7. **API 엔드포인트**
   - `order-service/src/main/java/com/example/order/controller/OrderController.java`
   - 주문 CRUD API
   - `order-service/src/main/java/com/example/order/controller/TestController.java`
   - Circuit Breaker 상태 확인 API

8. **보안 설정**
   - `order-service/src/main/java/com/example/order/config/SecurityConfig.java`
   - JWT 인증 설정 (Member Service와 동일)

**학습 포인트**:
- OpenFeign을 통한 선언적 REST 클라이언트
- Circuit Breaker 패턴 (Resilience4j)
- Fallback 메커니즘
- JWT 토큰 전파 (`RequestInterceptor`)
- Resilience4j 설정 (`resilience4j.circuitbreaker`)

**실습**:
1. Order Service 실행
2. `POST /orders` 호출하여 Member Service 연동 확인
3. Member Service 중지 후 Fallback 동작 확인
4. `GET /test/circuit-breaker-status` 호출하여 Circuit Breaker 상태 확인

---

### 5단계: 게이트웨이 이해 (전체 흐름 통합)

#### 5.1 Gateway Service
**목적**: API Gateway의 역할 및 라우팅, 인증 필터 이해

**확인 파일 순서**:

1. **애플리케이션 진입점**
   - `gateway-service/src/main/java/com/example/gateway/GatewayServiceApplication.java`
   - Spring Cloud Gateway 설정

2. **라우팅 설정**
   - `config-service/src/main/resources/config-repo/gateway-service.yml`
   - 각 서비스별 라우팅 규칙
   - `RewritePath` 필터
   - 정확한 경로 매칭 vs 하위 경로 매칭

3. **JWT 검증**
   - `gateway-service/src/main/java/com/example/gateway/security/JwtTokenValidator.java`
   - JWT 토큰 검증 로직
   - 클레임 추출

4. **인증 필터**
   - `gateway-service/src/main/java/com/example/gateway/filter/AuthenticationFilter.java`
   - `GlobalFilter` 구현
   - 요청 경로별 인증 처리 (whitelist)
   - JWT 토큰 검증 및 헤더 전파

5. **게이트웨이 설정**
   - `gateway-service/src/main/java/com/example/gateway/config/GatewayConfig.java`
   - CORS 설정
   - Circuit Breaker 설정

6. **Fallback 컨트롤러**
   - `gateway-service/src/main/java/com/example/gateway/controller/FallbackController.java`
   - 서비스 장애 시 응답

**학습 포인트**:
- Spring Cloud Gateway 라우팅
- `RewritePath` 필터를 통한 URL 재작성
- `GlobalFilter`를 통한 요청 전처리
- JWT 토큰 검증 및 전파
- CORS 설정
- Circuit Breaker 통합

**실습**:
1. Gateway Service 실행
2. `GET /api/members` 호출하여 라우팅 확인
3. JWT 토큰 없이 호출하여 인증 필터 동작 확인
4. 유효한 JWT 토큰으로 호출하여 정상 동작 확인

---

## 🔄 전체 흐름 이해

### 요청 흐름 예시: 주문 생성

```
1. 클라이언트
   ↓ POST /api/orders (JWT 토큰 포함)
   
2. Gateway Service (8080)
   - AuthenticationFilter: JWT 토큰 검증
   - 라우팅 규칙: /api/orders → /orders
   - Load Balancer: order-service 인스턴스 선택
   ↓
   
3. Order Service (8082)
   - SecurityConfig: JWT 토큰 인증
   - OrderController: 요청 처리
   - OrderService: 비즈니스 로직
   - MemberServiceClient (OpenFeign): Member Service 호출
     - FeignClientConfig: JWT 토큰 전파
     - Circuit Breaker: 장애 처리
   ↓
   
4. Member Service (8081)
   - SecurityConfig: JWT 토큰 인증
   - MemberController: 요청 처리
   - MemberService: 비즈니스 로직
   ↓
   
5. 응답 반환
   Order Service → Gateway Service → 클라이언트
```

---

## 📝 학습 체크리스트

### 인프라 서비스
- [ ] Discovery Service의 역할과 설정 이해
- [ ] Config Service의 역할과 설정 파일 구조 이해

### 인증 서비스
- [ ] JWT 토큰 생성 및 검증 로직 이해
- [ ] Spring Security 설정 이해
- [ ] BCrypt 비밀번호 암호화 이해

### 비즈니스 서비스
- [ ] Spring Data JPA 활용 방법 이해
- [ ] JWT 토큰 기반 인증 통합 이해
- [ ] 예외 처리 패턴 이해

### 통합 서비스
- [ ] OpenFeign을 통한 서비스 간 통신 이해
- [ ] Circuit Breaker 패턴 이해
- [ ] Fallback 메커니즘 이해
- [ ] JWT 토큰 전파 방법 이해

### 게이트웨이
- [ ] Spring Cloud Gateway 라우팅 이해
- [ ] 인증 필터 구현 방법 이해
- [ ] CORS 설정 이해

---

## 🎓 추가 학습 자료

### Spring Cloud 공식 문서
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Spring Cloud Config](https://spring.io/projects/spring-cloud-config)
- [Spring Cloud OpenFeign](https://spring.io/projects/spring-cloud-openfeign)

### Resilience4j
- [Resilience4j 공식 문서](https://resilience4j.readme.io/)

### JWT
- [JWT.io](https://jwt.io/) - JWT 토큰 디코딩 및 테스트

---

## 💡 학습 팁

1. **순차적 학습**: 위 순서대로 학습하면 각 개념이 자연스럽게 연결됩니다.
2. **실습 중심**: 각 단계마다 실제로 서비스를 실행하고 API를 호출해보세요.
3. **디버깅 활용**: IDE의 디버거를 사용하여 요청 흐름을 추적해보세요.
4. **설정 파일 이해**: `application.yml`과 `bootstrap.yml`의 차이를 이해하세요.
5. **로그 확인**: 각 서비스의 로그를 확인하여 실제 동작을 관찰하세요.

---

## ❓ 자주 묻는 질문

### Q: 왜 Discovery Service를 먼저 봐야 하나요?
A: Discovery Service는 모든 마이크로서비스가 등록되는 기반이 됩니다. 다른 서비스들이 어떻게 서비스를 찾는지 이해하는 것이 중요합니다.

### Q: Auth Service를 왜 두 번째로 봐야 하나요?
A: Auth Service는 보안의 기초가 됩니다. 다른 서비스들이 JWT 토큰을 어떻게 검증하는지 이해하려면 먼저 토큰이 어떻게 생성되는지 알아야 합니다.

### Q: Gateway Service를 마지막에 보는 이유는?
A: Gateway Service는 모든 서비스를 통합하는 역할을 합니다. 각 서비스의 동작을 이해한 후 Gateway를 보면 전체 흐름을 더 잘 이해할 수 있습니다.
