# OpenFeign Fallback 테스트 가이드

## 🔧 문제 해결 완료

### 이전 문제
- Fallback이 동작하지 않고 Exception 발생
- `InvalidOrderException: 회원 정보를 확인할 수 없습니다`

### 수정 사항
1. **validateMember 메서드 개선**: Exception 대신 Fallback 데이터 생성
2. **Circuit Breaker 의존성 추가**: resilience4j 추가
3. **Circuit Breaker 설정 추가**: application.yml에 resilience4j 설정
4. **테스트 컨트롤러 추가**: 다양한 Fallback 시나리오 테스트

## 🧪 개선된 테스트 순서

### 1단계: Order Service 재시작
```bash
# Order Service 중단 후 재시작
Ctrl + C (Order Service 터미널에서)
./gradlew order-service:bootRun
```

### 2단계: 정상 통신 확인 (Member Service 실행 중)
```bash
# Member Service 재시작
./gradlew member-service:bootRun
```

```http
# 정상 통신 테스트
GET http://localhost:8082/test/member/1
GET http://localhost:8082/orders/member/1
```

**예상 결과**: 정상적인 회원 정보 반환

### 3단계: Member Service 중단
```bash
# Member Service 프로세스 확인 및 종료
netstat -ano | findstr :8081
taskkill /PID {PID번호} /F
```

### 4단계: Fallback 동작 테스트
```http
# 1. 직접 Member Service 호출 (Fallback 확인)
GET http://localhost:8082/test/member/1

# 2. 회원별 주문 조회 (Fallback 데이터 사용)
GET http://localhost:8082/orders/member/1

# 3. 주문 생성 (Fallback으로 회원 검증)
POST http://localhost:8082/orders
Content-Type: application/json

{
  "memberId": 1,
  "productName": "Fallback 테스트 상품",
  "quantity": 1,
  "unitPrice": 10000.00,
  "orderMemo": "Member Service 장애 시 Fallback 테스트"
}

# 4. Circuit Breaker 상태 확인 (테스트용 엔드포인트)
GET http://localhost:8082/test/circuit-breaker/status

# 5. Member Service 헬스 체크 (테스트용 엔드포인트) 
GET http://localhost:8082/test/member/health
```

## 📊 예상 결과

### Member Service 장애 시 (Fallback 동작)

#### 1. 직접 Member Service 호출
```json
{
  "id": 1,
  "username": "unknown-user",
  "email": "unknown@example.com",
  "fullName": "알 수 없는 사용자",
  "phoneNumber": "000-0000-0000",
  "status": "UNKNOWN",
  "statusDescription": "서비스 일시 중단"
}
```

#### 2. 회원별 주문 조회
```json
[
  {
    "id": 1,
    "memberId": 1,
    "memberName": "알 수 없는 사용자",  // Fallback 데이터
    "productName": "노트북",
    "quantity": 1,
    "totalAmount": 1500000.00,
    "status": "CONFIRMED",
    "createdAt": "2024-01-01 10:00:00"
  }
]
```

#### 3. 주문 생성 성공
```json
{
  "id": 13,
  "memberId": 1,
  "memberName": "알 수 없는 사용자",  // Fallback 데이터
  "productName": "Fallback 테스트 상품",
  "quantity": 1,
  "unitPrice": 10000.00,
  "totalAmount": 10000.00,
  "status": "PENDING",
  "orderMemo": "Member Service 장애 시 Fallback 테스트"
}
```

#### 4. Circuit Breaker 상태
```json
{
  "circuitBreakerStatus": "FALLBACK",
  "service": "member-service"
}
```

## 🔍 로그 확인 포인트

### Order Service 로그에서 확인할 내용
```
[WARN] Member Service is not available for member ID: 1, using fallback data
[WARN] Member Service fallback triggered - service is unavailable
```

### Circuit Breaker 동작 로그
```
[DEBUG] Circuit breaker 'member-service' recorded a call which was not permitted.
[INFO] Circuit breaker 'member-service' changed state from CLOSED to OPEN
```

## 🔄 복구 테스트

### Member Service 재시작 후
```bash
# Member Service 재시작
./gradlew member-service:bootRun

# 동일한 API 다시 호출
GET http://localhost:8082/orders/member/1
```

**예상 결과**: 정상적인 회원명 ("관리자") 반환

## ⚡ 추가 테스트 시나리오

### 1. 대량 실패 테스트
```http
# Circuit Breaker를 열기 위한 연속 실패 유도 (Member Service 중단 상태에서 반복 호출)
GET http://localhost:8082/test/member/999 (여러 번 연속 호출)
```

### 2. 타임아웃 테스트
```yaml
# application.yml에서 타임아웃 단축
feign:
  client:
    config:
      member-service:
        connect-timeout: 100  # 매우 짧게 설정
        read-timeout: 100
```

### 3. 부분 장애 테스트
- Member Service는 실행 중이지만 매우 느린 응답
- 특정 API만 장애 상황

## 🎯 성공 기준

1. ✅ **Exception 대신 Fallback 데이터 반환**
2. ✅ **주문 처리 계속 진행** (서비스 복원력)
3. ✅ **Circuit Breaker 정상 동작**
4. ✅ **로그에 Fallback 경고 메시지**
5. ✅ **Member Service 복구 시 정상 동작**


