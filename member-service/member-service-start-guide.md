# 🚀 Member Service 시작 가이드

## 📋 Member Service 시작 방법

### 1. 새 터미널 열기
```bash
# 새 Command Prompt 또는 PowerShell 창 열기
cd C:\Users\kkk26\msa-service
./gradlew member-service:bootRun
```

### 2. 시작 완료 확인 메시지
다음 메시지가 나타날 때까지 대기:
```
Started MemberServiceApplication in X.XX seconds
```

### 3. 포트 확인
```bash
netstat -ano | findstr :8081
```
**예상 결과**: `TCP 0.0.0.0:8081 LISTENING`

## ✅ Member Service 시작 후 테스트할 내용

### 1. Health Check
```http
GET http://localhost:8081/actuator/health
```

### 2. 회원 목록 조회
```http
GET http://localhost:8081/members
```

### 3. **Circuit Breaker 복구 테스트**
```http
# 이제 정상적인 회원명이 표시되어야 함
GET http://localhost:8082/orders/member/1
```

**예상 결과 변화:**
- **이전**: `"memberName": "알 수 없는 사용자"`
- **이후**: `"memberName": "관리자"` (실제 회원명)

### 4. **새 주문 생성 테스트**
```http
POST http://localhost:8082/orders
Content-Type: application/json

{
  "memberId": 1,
  "productName": "정상 통신 테스트 상품",
  "quantity": 1,
  "unitPrice": 15000.00,
  "orderMemo": "Member Service 복구 후 테스트"
}
```

**예상 결과**: 정확한 회원명으로 주문 생성 성공

## 🎯 성공 기준

1. ✅ Member Service 포트 8081 LISTENING
2. ✅ Health Check 응답: `{"status":"UP"}`
3. ✅ 회원 목록 정상 조회
4. ✅ Order Service에서 정확한 회원명 표시
5. ✅ 새 주문 생성 시 회원 검증 성공

## 🔄 다음 단계

Member Service 시작이 완료되면:
1. **Gateway Service 시작** (포트 8080)
2. **전체 MSA 플로우 테스트**
3. **부하 테스트 및 안정성 확인**

---

**Member Service를 시작하신 후 "Member Service 시작 완료"라고 알려주시면, 즉시 통합 테스트를 계속 진행하겠습니다!** 🚀


