# 🚀 서비스 심화 계획서

이 문서는 MSA 프로젝트의 각 서비스를 단계적으로 심화시키는 구체적인 계획입니다.

---

## 📋 전체 개요

### 적용 순서 및 이유

| 순서 | 항목 | 서비스 | 난이도 | 예상 소요 | 우선순위 이유 |
|------|------|--------|--------|-----------|---------------|
| 1단계 | 캐싱 전략 | Member Service | ⭐⭐ | 1주 | 기초 개념, 독립적, 즉시 성능 개선 |
| 2단계 | 재시도 메커니즘 | Order Service | ⭐⭐ | 1주 | Resilience4j 확장, Circuit Breaker와 유사 |
| 3단계 | Rate Limiting | Gateway Service | ⭐⭐⭐ | 1주 | 보안 강화, Gateway 필터 패턴 이해 |
| 4단계 | JWT 리프레시 토큰 | Auth Service | ⭐⭐⭐ | 1-2주 | 보안 고도화, 토큰 관리 복잡도 증가 |
| 5단계 | 이벤트 기반 아키텍처 | 전체 시스템 | ⭐⭐⭐⭐ | 2-3주 | 인프라 추가, 비동기 통신 패턴 |
| 6단계 | 모니터링 (Prometheus + Grafana) | Gateway Service | ⭐⭐⭐ | 1-2주 | 운영 관점, 전체 시스템 이해 후 적용 |

**총 예상 소요 시간**: 7-10주

---

## 🎯 1단계: Member Service 캐싱 전략

### 📌 목표
- Spring Cache를 활용한 회원 정보 캐싱 구현
- 캐시 전략 수립 및 성능 최적화
- 캐시 무효화 전략 구현

### 📚 학습 내용
- Spring Cache 추상화
- Caffeine Cache (로컬 캐시)
- Redis (분산 캐시) - 선택사항
- 캐시 전략 (Cache-Aside, Write-Through, Write-Behind)
- TTL (Time To Live) 관리
- 캐시 무효화 전략

### 🔧 구현 내용

#### 1.1 의존성 추가
```gradle
// Caffeine Cache (로컬 캐시)
implementation 'com.github.ben-manes.caffeine:caffeine'

// 또는 Redis (분산 캐시)
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

#### 1.2 캐시 설정
- `@EnableCaching` 활성화
- Caffeine Cache 설정 (크기, TTL)
- 캐시 매니저 빈 등록

#### 1.3 캐시 적용
- `@Cacheable`: 회원 조회 메서드
- `@CacheEvict`: 회원 수정/삭제 시 캐시 무효화
- `@CachePut`: 회원 생성 시 캐시 저장

#### 1.4 캐시 전략
- **읽기**: Cache-Aside 패턴
- **쓰기**: Write-Through 또는 Cache-Aside
- **TTL**: 5분 (설정 가능)

### 🧪 테스트 방법
1. 캐시 히트/미스 확인
2. 성능 측정 (캐시 적용 전/후)
3. 캐시 무효화 동작 확인
4. 동시성 테스트

### 📝 문서화
- `MEMBER-SERVICE-GUIDE.md`에 캐싱 섹션 추가
- 캐시 전략 설명
- 설정 방법 및 모니터링

### ✅ 완료 기준
- [ ] 캐시 설정 완료
- [ ] 회원 조회 메서드에 캐시 적용
- [ ] 캐시 무효화 동작 확인
- [ ] 성능 개선 측정 (50% 이상)
- [ ] 문서화 완료

---

## 🎯 2단계: Order Service 재시도 메커니즘

### 📌 목표
- Resilience4j `@Retryable` 어노테이션을 활용한 재시도 메커니즘 구현
- 지수 백오프 전략 적용
- Circuit Breaker와 재시도 조합

### 📚 학습 내용
- Resilience4j Retry 모듈
- 재시도 전략 (고정 간격, 지수 백오프, 랜덤)
- 최대 재시도 횟수 설정
- 재시도 조건 (어떤 예외에서 재시도할지)
- Circuit Breaker와 Retry 조합

### 🔧 구현 내용

#### 2.1 의존성 확인
```gradle
// 이미 resilience4j-spring-boot3가 포함되어 있음
// Retry 모듈은 별도 추가 불필요
```

#### 2.2 Retry 설정
```yaml
resilience4j:
  retry:
    instances:
      member-service:
        maxAttempts: 3
        waitDuration: 1000
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException
```

#### 2.3 Retry 적용
- `MemberIntegrationService`에 `@Retryable` 추가
- Circuit Breaker와 Retry 조합
- 재시도 실패 시 Fallback 실행

#### 2.4 재시도 전략
- **최대 재시도**: 3회
- **대기 시간**: 1초 (지수 백오프)
- **재시도 조건**: 네트워크 오류, 타임아웃
- **재시도 제외**: 4xx 오류 (클라이언트 오류)

### 🧪 테스트 방법
1. Member Service 일시 중단 후 재시도 동작 확인
2. 재시도 로그 확인
3. 지수 백오프 동작 확인
4. Circuit Breaker와 조합 동작 확인

### 📝 문서화
- `ORDER-SERVICE-GUIDE.md`에 재시도 섹션 추가
- Retry 설정 설명
- Circuit Breaker와 Retry 조합 전략

### ✅ 완료 기준
- [ ] Retry 설정 완료
- [ ] `@Retryable` 어노테이션 적용
- [ ] 재시도 동작 확인
- [ ] 지수 백오프 동작 확인
- [ ] 문서화 완료

---

## 🎯 3단계: Gateway Service Rate Limiting

### 📌 목표
- Resilience4j RateLimiter를 활용한 API Rate Limiting 구현
- IP별, 사용자별 Rate Limiting
- Rate Limiting 예외 처리

### 📚 학습 내용
- Resilience4j RateLimiter
- 토큰 버킷 알고리즘
- Rate Limiting 전략
- API 보호 및 DDoS 방어

### 🔧 구현 내용

#### 3.1 RateLimiter 설정
```yaml
resilience4j:
  ratelimiter:
    instances:
      default:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 0
        subscribeToEvents: true
```

#### 3.2 Rate Limiting 필터 구현
- Gateway Filter Factory 생성
- IP별 Rate Limiting
- 사용자별 Rate Limiting (JWT 토큰 기반)
- Rate Limiting 초과 시 429 Too Many Requests 응답

#### 3.3 Rate Limiting 적용
- Gateway 라우팅 규칙에 Rate Limiting 필터 추가
- 서비스별 다른 Rate Limit 설정
- 인증된 요청과 비인증 요청 구분

#### 3.4 Rate Limiting 전략
- **기본 제한**: 초당 10개 요청
- **인증된 사용자**: 초당 50개 요청
- **특정 엔드포인트**: 별도 제한 설정

### 🧪 테스트 방법
1. Rate Limit 초과 시 429 응답 확인
2. Rate Limit 리셋 후 정상 동작 확인
3. IP별 Rate Limiting 동작 확인
4. 사용자별 Rate Limiting 동작 확인

### 📝 문서화
- `GATEWAY-SERVICE-GUIDE.md`에 Rate Limiting 섹션 추가
- Rate Limiting 설정 설명
- Rate Limiting 전략 및 모니터링

### ✅ 완료 기준
- [ ] RateLimiter 설정 완료
- [ ] Rate Limiting 필터 구현
- [ ] Gateway 라우팅에 적용
- [ ] Rate Limiting 동작 확인
- [ ] 문서화 완료

---

## 🎯 4단계: Auth Service JWT 리프레시 토큰

### 📌 목표
- JWT Access Token + Refresh Token 구현
- 토큰 갱신 API 구현
- 토큰 저장소 관리 (Redis 또는 데이터베이스)
- 토큰 블랙리스트 (로그아웃) 구현

### 📚 학습 내용
- JWT 토큰 구조 (Access Token, Refresh Token)
- 토큰 갱신 플로우
- 토큰 저장소 선택 (Redis vs Database)
- 토큰 블랙리스트 관리
- 보안 고려사항

### 🔧 구현 내용

#### 4.1 토큰 구조 변경
- Access Token: 짧은 만료 시간 (15분)
- Refresh Token: 긴 만료 시간 (7일)
- Refresh Token은 데이터베이스 또는 Redis에 저장

#### 4.2 토큰 발급 API 수정
```java
POST /auth/login
Response: {
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

#### 4.3 토큰 갱신 API 추가
```java
POST /auth/refresh
Request: {
  "refreshToken": "..."
}
Response: {
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

#### 4.4 토큰 저장소
- Redis 사용 (선택사항: H2 Database)
- Refresh Token 저장 및 검증
- 토큰 블랙리스트 관리

#### 4.5 로그아웃 API 추가
```java
POST /auth/logout
Request: {
  "refreshToken": "..."
}
```

### 🧪 테스트 방법
1. 로그인 후 Access Token + Refresh Token 발급 확인
2. Access Token 만료 후 Refresh Token으로 갱신 확인
3. Refresh Token 만료 시 재로그인 필요 확인
4. 로그아웃 후 토큰 무효화 확인

### 📝 문서화
- `AUTH-SERVICE-GUIDE.md`에 리프레시 토큰 섹션 추가
- 토큰 갱신 플로우 설명
- 보안 고려사항

### ✅ 완료 기준
- [ ] Refresh Token 발급 구현
- [ ] 토큰 갱신 API 구현
- [ ] 토큰 저장소 구현
- [ ] 로그아웃 API 구현
- [ ] 토큰 블랙리스트 구현
- [ ] 문서화 완료

---

## 🎯 5단계: 이벤트 기반 아키텍처 (Kafka/RabbitMQ)

### 📌 목표
- 메시지 브로커 도입 (Kafka 또는 RabbitMQ)
- 이벤트 발행 및 구독 구현
- 비동기 통신 패턴 적용
- 이벤트 소싱 패턴 이해

### 📚 학습 내용
- 메시지 브로커 선택 (Kafka vs RabbitMQ)
- 이벤트 발행/구독 패턴
- 이벤트 소싱
- 비동기 통신
- 이벤트 순서 보장
- 이벤트 재생

### 🔧 구현 내용

#### 5.1 메시지 브로커 선택
- **Kafka**: 대용량, 고성능, 이벤트 스트리밍
- **RabbitMQ**: 간단한 설정, AMQP 프로토콜
- **추천**: RabbitMQ (학습 용이성)

#### 5.2 의존성 추가
```gradle
// RabbitMQ
implementation 'org.springframework.boot:spring-boot-starter-amqp'

// 또는 Kafka
implementation 'org.springframework.kafka:spring-kafka'
```

#### 5.3 이벤트 정의
- 회원 생성 이벤트 (`MemberCreatedEvent`)
- 주문 생성 이벤트 (`OrderCreatedEvent`)
- 주문 상태 변경 이벤트 (`OrderStatusChangedEvent`)

#### 5.4 이벤트 발행
- Member Service: 회원 생성 시 이벤트 발행
- Order Service: 주문 생성/수정 시 이벤트 발행

#### 5.5 이벤트 구독
- Order Service: 회원 생성 이벤트 구독
- 알림 서비스 (선택사항): 주문 이벤트 구독

#### 5.6 이벤트 처리
- 비동기 처리
- 이벤트 순서 보장 (필요 시)
- 이벤트 재시도 메커니즘

### 🧪 테스트 방법
1. 이벤트 발행 확인
2. 이벤트 구독 및 처리 확인
3. 이벤트 순서 보장 확인
4. 이벤트 재시도 확인
5. 메시지 브로커 장애 시나리오 테스트

### 📝 문서화
- 이벤트 기반 아키텍처 가이드 작성
- 이벤트 정의 및 플로우 설명
- 메시지 브로커 설정 및 모니터링

### ✅ 완료 기준
- [ ] 메시지 브로커 설정 완료
- [ ] 이벤트 발행 구현
- [ ] 이벤트 구독 구현
- [ ] 이벤트 처리 로직 구현
- [ ] 이벤트 재시도 메커니즘 구현
- [ ] 문서화 완료

---

## 🎯 6단계: Gateway Service 모니터링 (Prometheus + Grafana)

### 📌 목표
- Micrometer를 활용한 메트릭 수집
- Prometheus 연동
- Grafana 대시보드 구성
- 실시간 모니터링 및 알림

### 📚 학습 내용
- Micrometer 메트릭
- Prometheus 메트릭 형식
- Grafana 대시보드 구성
- 메트릭 종류 (Counter, Gauge, Timer, Histogram)
- 알림 규칙 설정

### 🔧 구현 내용

#### 6.1 의존성 추가
```gradle
// Micrometer Prometheus
implementation 'io.micrometer:micrometer-registry-prometheus'
```

#### 6.2 Prometheus 설정
- Actuator 엔드포인트에 Prometheus 추가
- 메트릭 수집 설정
- 커스텀 메트릭 추가

#### 6.3 커스텀 메트릭
- API 요청 수 (Counter)
- 응답 시간 (Timer)
- Circuit Breaker 상태 (Gauge)
- Rate Limiting 상태 (Gauge)
- 에러율 (Counter)

#### 6.4 Grafana 대시보드 구성
- API 요청 수 그래프
- 응답 시간 분포
- Circuit Breaker 상태
- Rate Limiting 상태
- 에러율 추이

#### 6.5 알림 설정
- Circuit Breaker OPEN 상태 알림
- 에러율 임계값 초과 알림
- 응답 시간 임계값 초과 알림

### 🧪 테스트 방법
1. Prometheus 메트릭 수집 확인
2. Grafana 대시보드 동작 확인
3. 커스텀 메트릭 확인
4. 알림 동작 확인

### 📝 문서화
- `GATEWAY-SERVICE-GUIDE.md`에 모니터링 섹션 추가
- Prometheus 설정 설명
- Grafana 대시보드 구성 가이드
- 알림 설정 방법

### ✅ 완료 기준
- [ ] Prometheus 연동 완료
- [ ] 커스텀 메트릭 구현
- [ ] Grafana 대시보드 구성
- [ ] 알림 설정 완료
- [ ] 문서화 완료

---

## 📊 진행 상황 추적

각 단계별로 체크리스트를 관리하여 진행 상황을 추적합니다.

### 1단계: Member Service 캐싱 전략
- [ ] 시작일: ____
- [ ] 완료일: ____
- [ ] 상태: ⬜ 미시작 / 🟡 진행중 / ✅ 완료

### 2단계: Order Service 재시도 메커니즘
- [ ] 시작일: ____
- [ ] 완료일: ____
- [ ] 상태: ⬜ 미시작 / 🟡 진행중 / ✅ 완료

### 3단계: Gateway Service Rate Limiting
- [ ] 시작일: ____
- [ ] 완료일: ____
- [ ] 상태: ⬜ 미시작 / 🟡 진행중 / ✅ 완료

### 4단계: Auth Service JWT 리프레시 토큰
- [ ] 시작일: ____
- [ ] 완료일: ____
- [ ] 상태: ⬜ 미시작 / 🟡 진행중 / ✅ 완료

### 5단계: 이벤트 기반 아키텍처
- [ ] 시작일: ____
- [ ] 완료일: ____
- [ ] 상태: ⬜ 미시작 / 🟡 진행중 / ✅ 완료

### 6단계: Gateway Service 모니터링
- [ ] 시작일: ____
- [ ] 완료일: ____
- [ ] 상태: ⬜ 미시작 / 🟡 진행중 / ✅ 완료

---

## 🎓 학습 로드맵

각 단계를 완료하면서 다음 개념들을 학습하게 됩니다:

1. **캐싱**: 성능 최적화, 캐시 전략, 분산 캐시
2. **재시도**: 장애 복구, 지수 백오프, 재시도 전략
3. **Rate Limiting**: API 보호, 트래픽 제어, DDoS 방어
4. **JWT 리프레시 토큰**: 보안 강화, 토큰 관리, 세션 관리
5. **이벤트 기반 아키텍처**: 비동기 통신, 이벤트 소싱, 메시지 브로커
6. **모니터링**: 메트릭 수집, 대시보드 구성, 알림 설정

---

## 💡 추가 고려사항

### 각 단계별 공통 작업
1. **테스트 코드 작성**: 단위 테스트 및 통합 테스트
2. **문서화**: 가이드 문서 업데이트
3. **성능 측정**: 적용 전/후 성능 비교
4. **모니터링**: 각 기능의 동작 모니터링

### 주의사항
- 각 단계를 완료한 후 다음 단계로 진행
- 각 단계마다 커밋 및 문서화
- 테스트를 통한 검증 필수
- 성능 측정 및 모니터링 지속

---

## 🚀 시작하기

1단계부터 순차적으로 진행하시면 됩니다. 각 단계가 완료되면 다음 단계로 넘어가세요.

**1단계 시작**: Member Service 캐싱 전략 구현

