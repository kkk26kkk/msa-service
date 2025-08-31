# Order Service OpenFeign 테스트 시나리오

## 문제 해결 방법

### Load Balancer 오류: "Load balancer does not contain an instance for the service member-service"

**원인**: OpenFeign이 Service Discovery 없이 서비스를 찾으려고 시도함

**해결책**: URL 직접 지정 또는 Eureka 사용

## 테스트 방법

### 방법 1: 직접 URL 사용 (현재 적용됨)

```bash
# 1. Member Service 실행
./gradlew member-service:bootRun

# 2. Order Service 실행 
./gradlew order-service:bootRun

# 3. OpenFeign 통신 테스트
GET http://localhost:8082/orders/member/1
```

### 방법 2: Eureka를 사용한 Service Discovery

```bash
# 1. Discovery Service 실행
./gradlew discovery-service:bootRun

# 2. Member Service 실행 (Eureka 등록)
./gradlew member-service:bootRun

# 3. Order Service 실행 (Eureka 프로필 사용)
java -Dspring.profiles.active=eureka -jar order-service/build/libs/order-service.jar

# 또는 application.yml에서 eureka.client.enabled=true로 변경
```

## 테스트 확인 방법

### 1. Member Service 상태 확인
```bash
curl http://localhost:8081/actuator/health
```

### 2. Order Service에서 Member Service 호출 테스트
```bash
curl http://localhost:8082/test/member/1
curl http://localhost:8082/orders/member/1
```

### 3. OpenFeign 로그 확인
Order Service 실행 로그에서 다음을 확인:
- Feign 클라이언트 생성 로그
- HTTP 호출 로그
- 응답 데이터

## 예상 응답

### 성공 시
```json
[
  {
    "id": 1,
    "memberId": 1,
    "memberName": "관리자",
    "productName": "노트북",
    "quantity": 1,
    "totalAmount": 1500000.00,
    "status": "CONFIRMED",
    "createdAt": "2024-01-01 10:00:00"
  }
]
```

### Member Service 장애 시 (Fallback)
```json
[
  {
    "id": 1,
    "memberId": 1,
    "memberName": "알 수 없는 사용자",
    "productName": "노트북",
    "quantity": 1,
    "totalAmount": 1500000.00,
    "status": "CONFIRMED",
    "createdAt": "2024-01-01 10:00:00"
  }
]
```

## 트러블슈팅

### 1. Connection Refused
- Member Service가 실행 중인지 확인
- 포트 8081이 사용 중인지 확인

### 2. Timeout 오류
- feign.client.config.member-service.connect-timeout 설정 확인
- 네트워크 연결 상태 확인

### 3. 404 Not Found
- Member Service API 경로 확인 (/members/{id})
- API 응답 형식 확인

### 4. Circuit Breaker 동작
- MemberServiceClientFallback 로그 확인
- 연속 실패 시 Circuit Open 상태 확인


