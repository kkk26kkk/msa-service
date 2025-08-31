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
│    Member       │    │     Order       │    │     Test        │
│   Service       │────│    Service      │    │  Controllers    │
│   (Port 8081)   │    │   (Port 8082)   │    │                 │
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

## ✅ 2단계: 기본 상태 확인

### 2.1 모든 서비스 포트 확인
```bash
netstat -ano | findstr "8761 8888 8080 8081 8082"
```

**예상 결과**:
```
TCP    0.0.0.0:8761    LISTENING    [PID]  # Discovery
TCP    0.0.0.0:8888    LISTENING    [PID]  # Config  
TCP    0.0.0.0:8080    LISTENING    [PID]  # Gateway
TCP    0.0.0.0:8081    LISTENING    [PID]  # Member
TCP    0.0.0.0:8082    LISTENING    [PID]  # Order
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

## 🔗 6단계: 서비스 간 통신 테스트

### 6.1 정상 통신 테스트
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

### 6.2 Circuit Breaker 및 Fallback 테스트

#### Member Service 중단
```bash
# Member Service 프로세스 찾기
netstat -ano | findstr :8081

# 프로세스 종료
taskkill /PID {PID번호} /F
```

#### Fallback 동작 확인
```http
# 1. 회원별 주문 조회 (Fallback)
GET http://localhost:8082/orders/member/1

# 2. 새 주문 생성 (Fallback으로 회원 검증)
POST http://localhost:8082/orders
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

# 4. Member Service 연결 상태
GET http://localhost:8082/test/member-health
```

**예상 결과**:
- 회원명: "알 수 없는 사용자"
- 주문 생성 계속 진행
- Circuit Breaker 상태: "FALLBACK" 또는 "OPEN"

## 🔄 7단계: 서비스 복구 테스트

### 7.1 Member Service 재시작
```bash
./gradlew member-service:bootRun
```

### 7.2 복구 확인
```http
# 정상 통신 복구 확인
GET http://localhost:8082/orders/member/1
GET http://localhost:8082/test/member/1
GET http://localhost:8082/test/circuit-breaker-status
```

**예상 결과**: 
- 정상적인 회원명 반환
- Circuit Breaker 상태: "CLOSED"

## 🏁 8단계: 전체 시나리오 통합 테스트

### 8.1 완전한 주문 처리 플로우
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

### 8.2 부하 및 안정성 테스트
```http
# 연속 요청을 통한 안정성 확인
GET http://localhost:8080/api/members (10회 반복)
GET http://localhost:8080/api/orders (10회 반복)

# 동시 주문 생성 테스트
POST http://localhost:8080/api/orders (동시 5개 요청)
```

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

### ✅ Member Service
- [ ] CRUD 작업 모두 정상
- [ ] 데이터 검증 동작
- [ ] 예외 처리 적절

### ✅ Order Service
- [ ] 주문 CRUD 정상 동작
- [ ] OpenFeign 통신 성공
- [ ] Fallback 메커니즘 동작

### ✅ Circuit Breaker
- [ ] 서비스 장애 시 Fallback 동작
- [ ] Circuit Breaker 상태 전환
- [ ] 서비스 복구 시 정상 통신

### ✅ 전체 시스템
- [ ] Gateway를 통한 모든 API 접근 가능
- [ ] 서비스 간 통신 안정적
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

## 🎯 최종 검증

모든 테스트가 완료되면 다음을 확인:

1. **5개 서비스 모두 실행 중**
2. **Gateway를 통한 모든 API 접근 가능**
3. **서비스 간 통신 정상**
4. **Circuit Breaker 동작 확인**
5. **로그에 에러 없음**
