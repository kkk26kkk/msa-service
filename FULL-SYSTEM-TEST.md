# 🚀 MSA 전체 시스템 테스트 가이드

## 📋 테스트 개요

이 가이드는 완전한 MSA 시스템의 모든 구성 요소를 단계별로 테스트합니다.

### 🏗️ 시스템 아키텍처
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Discovery     │    │     Config      │    │    Gateway      │
│   Service       │    │    Service      │    │   Service       │
│   (Port 8761)   │    │   (Port 8888)   │    │   (Port 8080)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│    Member       │    │     Order       │    │      Auth       │
│   Service       │────│    Service      │    │    Service      │
│   (Port 8081)   │    │   (Port 8082)   │    │   (Port 8083)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔧 1단계: 서비스 시작 순서

### 1.1 Discovery Service 시작 (필수 우선)
```bash
# 터미널 1
./gradlew discovery-service:bootRun
```
**대기**: `Started DiscoveryServiceApplication` 메시지 확인

### 1.2 Config Service 시작
```bash
# 터미널 2
./gradlew config-service:bootRun
```
**대기**: `Started ConfigServiceApplication` 메시지 확인

### 1.3 Gateway Service 시작
```bash
# 터미널 3
./gradlew gateway-service:bootRun
```
**대기**: `Started GatewayServiceApplication` 메시지 확인

### 1.4 Member Service 시작
```bash
# 터미널 4
./gradlew member-service:bootRun
```
**대기**: `Started MemberServiceApplication` 메시지 확인

### 1.5 Order Service 시작
```bash
# 터미널 5
./gradlew order-service:bootRun
```
**대기**: `Started OrderServiceApplication` 메시지 확인

### 1.6 Auth Service 시작
```bash
# 터미널 6
./gradlew auth-service:bootRun
```
**대기**: `Started AuthServiceApplication` 메시지 확인

## ✅ 2단계: 기본 상태 확인

### 2.1 모든 서비스 포트 확인
```bash
netstat -ano | findstr "8761 8888 8080 8081 8082 8083"
```

**예상 결과**:
```
TCP    0.0.0.0:8761    LISTENING    [PID]  # Discovery
TCP    0.0.0.0:8888    LISTENING    [PID]  # Config  
TCP    0.0.0.0:8080    LISTENING    [PID]  # Gateway
TCP    0.0.0.0:8081    LISTENING    [PID]  # Member
TCP    0.0.0.0:8082    LISTENING    [PID]  # Order
TCP    0.0.0.0:8083    LISTENING    [PID]  # Auth
```

### 2.2 Discovery Service 등록 확인
```http
GET http://localhost:8761
```
**확인 항목**: Eureka 대시보드에서 서비스 등록 상태

### 2.3 각 서비스 Health Check
```http
# Discovery Service
GET http://localhost:8761/actuator/health

# Config Service  
GET http://localhost:8888/actuator/health

# Gateway Service
GET http://localhost:8080/actuator/health

# Member Service
GET http://localhost:8081/actuator/health

# Order Service
GET http://localhost:8082/actuator/health

# Auth Service
GET http://localhost:8083/actuator/health
```

## 🧪 3단계: Discovery Service 테스트

### 3.1 서비스 등록 확인
```http
GET http://localhost:8761/eureka/apps
```

### 3.2 특정 서비스 조회
```http
GET http://localhost:8761/eureka/apps/MEMBER-SERVICE
GET http://localhost:8761/eureka/apps/ORDER-SERVICE
GET http://localhost:8761/eureka/apps/GATEWAY-SERVICE
GET http://localhost:8761/eureka/apps/AUTH-SERVICE
```

**예상 결과**: XML 형식으로 서비스 인스턴스 정보 반환

## 🔧 4단계: Config Service 테스트

### 4.1 공통 설정 조회
```http
GET http://localhost:8888/application/default
```

### 4.2 서비스별 설정 조회
```http
GET http://localhost:8888/member-service/default
GET http://localhost:8888/order-service/default
GET http://localhost:8888/gateway-service/default
GET http://localhost:8888/auth-service/default
```

**예상 결과**: JSON 형식으로 설정 정보 반환

## 🌐 5단계: Gateway Service 테스트

### 5.1 Gateway를 통한 Member Service 호출
```http
# 직접 호출
GET http://localhost:8081/members

# Gateway를 통한 호출
GET http://localhost:8080/api/members
```

### 5.2 Gateway를 통한 Order Service 호출
```http
# 직접 호출
GET http://localhost:8082/orders

# Gateway를 통한 호출  
GET http://localhost:8080/api/orders
```

### 5.3 Gateway 라우팅 규칙 테스트
```http
# Member Service APIs via Gateway
GET http://localhost:8080/api/members/1
POST http://localhost:8080/api/members
Content-Type: application/json
{
  "username": "gateway-test",
  "email": "gateway@test.com", 
  "fullName": "Gateway Test User",
  "phoneNumber": "010-0000-0000"
}

# Order Service APIs via Gateway
GET http://localhost:8080/api/orders/1
POST http://localhost:8080/api/orders
Content-Type: application/json
{
  "memberId": 1,
  "productName": "Gateway 테스트 상품",
  "quantity": 1,
  "unitPrice": 15000.00
}
```

## 🔐 5.4단계: Auth Service 테스트

### 5.4.1 사용자 로그인 테스트
```http
# 기본 사용자 로그인 (DataInitializer.java에서 생성된 계정)
POST http://localhost:8083/auth/login
Content-Type: application/json
{
  "username": "admin",
  "password": "password123"
}
```

**예상 결과**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "username": "admin",
  "roles": ["ROLE_ADMIN"]
}
```

### 5.4.2 Health Check
```http
GET http://localhost:8083/auth/health
```

### 5.4.3 JWT 토큰 검증
```http
# 발급받은 토큰을 Authorization 헤더에 포함하여 다른 서비스 호출
GET http://localhost:8083/auth/health
Authorization: Bearer {발급받은_토큰}
```

## 🔗 6단계: Auth Service 통합 테스트

### 6.0 초기 사용자 정보

**중요**: Auth Service의 초기 사용자는 **DataInitializer.java**에서 생성됩니다.

| 사용자명 | 비밀번호 | 역할 |
|---------|---------|------|
| `admin` | `password123` | ADMIN |
| `member` | `password123` | USER |

**참고**: 
- Member Service의 `data.sql`에 있는 `admin` / `admin123`은 회원 정보(Member 엔티티)용이며, Auth Service 인증과는 별개입니다.
- Auth Service 로그인은 위의 계정을 사용해야 합니다.

### 6.1 인증 플로우 테스트
```http
# 1. 로그인하여 JWT 토큰 획득
POST http://localhost:8083/auth/login
Content-Type: application/json
{
  "username": "admin",
  "password": "password123"
}

# 2. 획득한 토큰 저장 (변수로 사용)
@token = {위에서_받은_accessToken}

# 3. 토큰을 사용한 인증된 요청 (향후 Gateway 연동 시 사용)
GET http://localhost:8083/auth/health
Authorization: Bearer {{token}}
```

### 6.2 잘못된 인증 정보 테스트
```http
# 잘못된 비밀번호
POST http://localhost:8083/auth/login
Content-Type: application/json
{
  "username": "admin",
  "password": "wrong-password"
}
```

**예상 결과**: 401 Unauthorized

## 🔗 7단계: 서비스 간 통신 테스트

### 7.1 정상 통신 테스트
```http
# Member Service가 정상 동작 중인 상태
GET http://localhost:8082/orders/member/1
GET http://localhost:8082/test/member/1

# OpenFeign을 통한 주문 생성
POST http://localhost:8082/orders  
Content-Type: application/json
{
  "memberId": 1,
  "productName": "서비스 통신 테스트 상품",
  "quantity": 2,
  "unitPrice": 25000.00,
  "orderMemo": "정상 통신 테스트"
}
```

**예상 결과**: 
- 정확한 회원명 표시
- 주문 생성 성공

### 7.2 Circuit Breaker 및 Fallback 테스트

**구현 방식**: Resilience4j의 `@CircuitBreaker` 어노테이션을 사용하여 Circuit Breaker 패턴을 적용합니다.
- `@CircuitBreaker(name = "member-service", fallbackMethod = "validateMemberFallback")` 어노테이션 적용
- Member Service 장애 시 자동으로 Fallback 메서드가 실행됩니다.
- Circuit Breaker가 실패를 올바르게 카운트하여 메트릭을 수집합니다.

#### Member Service 중단
```bash
# Member Service 프로세스 찾기
netstat -ano | findstr :8081

# 프로세스 종료
taskkill /PID {PID번호} /F
```

#### Fallback 동작 확인
```http
# 1. 회원별 주문 조회 (@CircuitBreaker가 자동으로 Fallback 실행)
GET http://localhost:8082/orders/member/1
Authorization: Bearer {JWT_토큰}

# 2. 새 주문 생성 (@CircuitBreaker가 자동으로 Fallback 실행)
POST http://localhost:8082/orders
Authorization: Bearer {JWT_토큰}
Content-Type: application/json
{
  "memberId": 1,
  "productName": "Fallback 테스트 상품",
  "quantity": 1,
  "unitPrice": 12000.00,
  "orderMemo": "Circuit Breaker 테스트"
}

# 3. Circuit Breaker 상태 확인
GET http://localhost:8082/test/circuit-breaker-status
Authorization: Bearer {JWT_토큰}

# 4. Member Service 연결 상태
GET http://localhost:8082/test/member-health
Authorization: Bearer {JWT_토큰}
```

**예상 결과**:
- 회원명: "알 수 없는 사용자" (Fallback 메서드가 자동 실행)
- 주문 생성 계속 진행 (Fallback 데이터로 처리)
- Circuit Breaker 상태: "OPEN" (Circuit Breaker가 열림)
- `@CircuitBreaker` 어노테이션이 자동으로 Fallback 메서드를 호출

**Circuit Breaker 상태 확인 응답 예시**:
```json
{
  "circuitBreakerStatus": "OPEN",
  "service": "member-service",
  "failureRate": "100.00%",
  "numberOfSuccessfulCalls": 0,
  "numberOfFailedCalls": 5,
  "numberOfNotPermittedCalls": 3,
  "numberOfBufferedCalls": 5,
  "statusDescription": "Circuit Breaker 열림 - 요청이 차단되고 Fallback이 실행됩니다"
}
```

**Circuit Breaker 상태 설명**:
- **CLOSED**: 정상 상태 - 모든 요청이 통과됩니다
- **OPEN**: Circuit Breaker 열림 - 요청이 차단되고 `@CircuitBreaker` 어노테이션이 자동으로 Fallback 메서드를 실행합니다
- **HALF_OPEN**: 반열림 상태 - 제한된 요청만 허용하여 서비스 복구를 테스트합니다
- **DISABLED**: 비활성화됨 - Circuit Breaker가 작동하지 않습니다

**Fallback 동작 원리**:
- Resilience4j의 `@CircuitBreaker` 어노테이션이 적용된 메서드에서 예외 발생 시
- 자동으로 지정된 `fallbackMethod`가 호출됩니다
- Fallback 메서드는 원본 메서드와 같은 클래스에 있어야 하며, `Exception` 파라미터를 포함해야 합니다
- `MemberIntegrationService`라는 별도 서비스 클래스에 Circuit Breaker 로직이 구현되어 있어
- Spring AOP 프록시를 통한 호출이 보장되어 `@CircuitBreaker` 어노테이션이 정상 작동합니다
- 순환 참조 문제 없이 프록시를 통한 호출이 가능합니다

## 🔄 8단계: 서비스 복구 테스트

### 8.1 Member Service 재시작
```bash
./gradlew member-service:bootRun
```

### 8.2 복구 확인
```http
# 정상 통신 복구 확인
GET http://localhost:8082/orders/member/1
GET http://localhost:8082/test/member/1
GET http://localhost:8082/test/circuit-breaker-status
```

**예상 결과**: 
- 정상적인 회원명 반환
- Circuit Breaker 상태: "CLOSED"

## 🏁 9단계: 전체 시나리오 통합 테스트

### 9.1 완전한 주문 처리 플로우
```http
# 1. 회원 생성 (Gateway를 통해)
POST http://localhost:8080/api/members
Content-Type: application/json
{
  "username": "integration-user",
  "email": "integration@test.com",
  "fullName": "통합 테스트 사용자",
  "phoneNumber": "010-1234-5678"
}

# 2. 생성된 회원 ID로 주문 생성 (Gateway를 통해)
POST http://localhost:8080/api/orders
Content-Type: application/json
{
  "memberId": {새로_생성된_회원ID},
  "productName": "통합 테스트 상품",
  "quantity": 3,
  "unitPrice": 33000.00,
  "orderMemo": "전체 시스템 통합 테스트"
}

# 3. 생성된 주문 확인
GET http://localhost:8080/api/orders/{새로_생성된_주문ID}

# 4. 회원별 주문 목록 확인
GET http://localhost:8082/orders/member/{회원ID}
```

### 9.2 인증 통합 테스트
```http
# 1. Auth Service에서 로그인하여 토큰 획득
POST http://localhost:8083/auth/login
Content-Type: application/json
{
  "username": "admin",
  "password": "password123"
}

# 2. 획득한 토큰으로 보호된 리소스 접근
GET http://localhost:8080/api/members/1
Authorization: Bearer {토큰}
```

### 9.3 부하 및 안정성 테스트
```http
# 연속 요청을 통한 안정성 확인
GET http://localhost:8080/api/members (10회 반복)
GET http://localhost:8080/api/orders (10회 반복)

# 동시 주문 생성 테스트
POST http://localhost:8080/api/orders (동시 5개 요청)
```

### 9.4 Gateway Service Circuit Breaker 테스트

#### Circuit Breaker 상태 확인
```http
# Gateway Service의 Circuit Breaker 상태 조회
GET http://localhost:8080/actuator/circuitbreakers

# 특정 Circuit Breaker 이벤트 조회
GET http://localhost:8080/actuator/circuitbreakerevents/member-service
GET http://localhost:8080/actuator/circuitbreakerevents/order-service
GET http://localhost:8080/actuator/circuitbreakerevents/auth-service
```

**예상 응답**:
```json
{
  "circuitBreakers": [
    {
      "name": "member-service",
      "state": "CLOSED",
      "failureRate": 0.0,
      "slowCallRate": 0.0,
      "bufferedCalls": 10,
      "failedCalls": 0,
      "successfulCalls": 10
    }
  ]
}
```

#### Circuit Breaker Health Check
```http
# Health Check에 Circuit Breaker 상태 포함
GET http://localhost:8080/actuator/health
```

**참고**: `management.health.circuitbreakers.enabled: true` 설정이 필요합니다.

### 9.5 Gateway Service를 통한 인증된 요청 테스트

```http
# 1. Auth Service에서 로그인하여 토큰 획득
POST http://localhost:8080/api/auth/login
Content-Type: application/json
{
  "username": "admin",
  "password": "password123"
}

# 2. 획득한 토큰으로 보호된 리소스 접근
GET http://localhost:8080/api/members/1
Authorization: Bearer {발급받은_토큰}

GET http://localhost:8080/api/orders/1
Authorization: Bearer {발급받은_토큰}
```

### 9.6 로깅 확인

#### Gateway Service 로깅 확인
Gateway Service의 로그에서 다음 정보를 확인할 수 있습니다:

**요청 로깅**:
```
[GATEWAY] GET http://localhost:8080/api/members/1 -> Client IP: 127.0.0.1
AuthenticationFilter - Path: /members/1, Whitelisted: false, Method: GET
```

**Circuit Breaker 로깅**:
```
Circuit breaker 'member-service' changed state from CLOSED to OPEN
```

#### Order Service 로깅 확인
Order Service의 로그에서 다음 정보를 확인할 수 있습니다:

**정상 통신**:
```
MemberIntegrationService - Validating member with ID: 1
```

**Fallback 실행**:
```
MemberIntegrationService - Member Service unavailable. Using fallback for memberId: 1
```

#### 브라우저에서 CORS 테스트 시 주의사항

**중요**: 브라우저 콘솔에서 직접 `fetch`를 실행하면 Content Security Policy (CSP) 제한으로 인해 오류가 발생할 수 있습니다.

**권장 방법**:
1. **Postman 또는 HTTP 클라이언트 사용** (가장 권장)
   ```http
   GET http://localhost:8080/api/members/1
   Authorization: Bearer {JWT_토큰}
   ```

2. **HTML 페이지를 통한 테스트**
   - 별도의 HTML 파일을 만들어서 테스트
   - `file://` 프로토콜로 열거나 로컬 웹 서버에서 실행

3. **브라우저 개발자 도구의 Network 탭 사용**
   - Network 탭에서 요청/응답 확인
   - CORS 헤더가 정상적으로 포함되어 있는지 확인

**CSP 오류 해결 방법**:
- 브라우저 확장 프로그램 비활성화 (특히 보안 관련 확장)
- 새 시크릿 창에서 테스트
- Gateway Service의 CORS 설정은 정상입니다. 문제는 브라우저의 CSP 정책 때문입니다.

## 📊 성공 기준 체크리스트

### ✅ Discovery Service
- [ ] 모든 서비스가 Eureka에 등록됨
- [ ] 서비스 상태가 UP으로 표시됨
- [ ] 서비스 목록 조회 가능

### ✅ Config Service  
- [ ] 각 서비스의 설정 정보 제공
- [ ] 공통 설정 정상 로드
- [ ] 환경별 설정 분리 동작

### ✅ Gateway Service
- [ ] 모든 라우팅 규칙 정상 동작
- [ ] CORS 설정 적용
- [ ] 로드 밸런싱 동작
- [ ] Circuit Breaker 필터 동작 (Member, Order, Auth Service)
- [ ] 요청 로깅 정상 동작
- [ ] JWT 인증 필터 정상 동작

### ✅ Member Service
- [ ] CRUD 작업 모두 정상
- [ ] 데이터 검증 동작
- [ ] 예외 처리 적절

### ✅ Order Service
- [ ] 주문 CRUD 정상 동작
- [ ] OpenFeign 통신 성공
- [ ] Resilience4j `@CircuitBreaker` 어노테이션을 통한 Fallback 메커니즘 동작
- [ ] Circuit Breaker가 실패를 올바르게 카운트

### ✅ Auth Service
- [ ] 사용자 로그인 정상 동작
- [ ] JWT 토큰 발급 성공
- [ ] 비밀번호 암호화 동작
- [ ] 역할 기반 권한 관리
- [ ] 잘못된 인증 정보 처리

### ✅ Circuit Breaker
- [ ] 서비스 장애 시 `@CircuitBreaker` 어노테이션이 자동으로 Fallback 메서드 실행
- [ ] Circuit Breaker 상태 전환 (CLOSED → OPEN → HALF_OPEN → CLOSED)
- [ ] Circuit Breaker가 실패를 올바르게 카운트하여 메트릭 수집
- [ ] 서비스 복구 시 정상 통신

### ✅ 전체 시스템
- [ ] Gateway를 통한 모든 API 접근 가능
- [ ] 서비스 간 통신 안정적
- [ ] 인증 및 인가 시스템 동작
- [ ] 장애 시 시스템 복원력 확인
- [ ] 모든 로그 정상 출력

## 🔍 트러블슈팅 가이드

### 일반적인 문제들
1. **서비스 시작 순서**: Discovery → Config → 나머지
2. **포트 충돌**: `netstat`로 포트 사용 확인
3. **설정 로딩 실패**: Config Service 먼저 확인
4. **서비스 등록 실패**: Discovery Service 상태 확인
5. **Gateway 라우팅 실패**: 서비스 등록 상태 및 라우팅 규칙 확인

### 로그 확인 포인트
- **Eureka 등록**: `DiscoveryClient_XXX - registration status: 204`
- **Config 로딩**: `Located environment: name=xxx`
- **OpenFeign 호출**: `Sending request`
- **Circuit Breaker**: `Circuit breaker 'xxx' changed state`
- **Fallback 실행**: `Member Service unavailable. Using fallback for memberId: {id}` (MemberIntegrationService 로그에서 확인)
- **Gateway 요청 로깅**: `[GATEWAY] {METHOD} {URI} -> Client IP: {IP}`
- **인증 필터**: `AuthenticationFilter - Path: {path}, Whitelisted: {boolean}, Method: {method}`

## 🎯 최종 검증

모든 테스트가 완료되면 다음을 확인:

1. **6개 서비스 모두 실행 중** (Discovery, Config, Gateway, Member, Order, Auth)
2. **Gateway를 통한 모든 API 접근 가능**
3. **서비스 간 통신 정상**
4. **인증 및 인가 시스템 동작**
5. **Circuit Breaker 동작 확인**
6. **로그에 에러 없음**
