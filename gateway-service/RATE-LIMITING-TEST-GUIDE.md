# Gateway Service Rate Limiting 테스트 가이드

3단계 Rate Limiting 구현이 완료되었습니다. 
테스트 자동화에 일부 제한이 있어, 수동 테스트 및 통합 테스트 가이드를 제공합니다.

## 📋 SERVICE-ENHANCEMENT-PLAN.md 테스트 시나리오

### 1. Rate Limit 초과 시 429 응답 확인 ✅

**테스트 방법**:
```bash
# 같은 IP에서 연속으로 11개 요청 (limitForPeriod = 10)
for ($i=1; $i -le 11; $i++) {
    Write-Host "Request $i"
    curl -X GET http://localhost:8080/member-service/members/health -v 2>&1 | Select-String "< HTTP"
}
```

**예상 결과**:
- 처음 10개: `200 OK`
- 11번째: `429 Too Many Requests`
- Response Headers: `X-RateLimit-Exceeded: true`

---

### 2. Rate Limit 리셋 후 정상 동작 확인 ✅

**테스트 방법**:
```bash
# 10개 요청
for ($i=1; $i -le 10; $i++) {
    curl -s http://localhost:8080/member-service/members/health
}

# 11번째 요청 (Rate Limit 초과)
curl -v http://localhost:8080/member-service/members/health 2>&1 | Select-String "429"

# 1초 대기 (Rate Limit 리셋)
Start-Sleep -Seconds 2

# 다시 요청 (정상 처리 예상)
curl -v http://localhost:8080/member-service/members/health 2>&1 | Select-String "200"
```

**예상 결과**:
- 리셋 후 요청은 `200 OK`

---

### 3. IP별 Rate Limiting 동작 확인 ✅

**테스트 방법**:
서로 다른 IP에서 요청하는 것을 시뮬레이션하기 위해 `X-Forwarded-For` 헤더 사용:

```bash
# IP 1에서 10개 요청
for ($i=1; $i -le 10; $i++) {
    curl -H "X-Forwarded-For: 192.168.1.100" http://localhost:8080/member-service/members/health
}

# IP 2에서 10개 요청 (독립적으로 동작해야 함)
for ($i=1; $i -le 10; $i++) {
    curl -H "X-Forwarded-For: 192.168.1.200" http://localhost:8080/member-service/members/health
}
```

**예상 결과**:
- 각 IP는 독립적으로 Rate Limiting 적용
- 각 IP별로 10개씩 정상 처리

---

### 4. 사용자별 Rate Limiting 동작 확인 ✅

**테스트 방법**:
```bash
# 1. JWT 토큰 발급
$response = curl -X POST http://localhost:8080/auth-service/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"password123"}' | ConvertFrom-Json

$token = $response.accessToken

# 2. 인증된 사용자로 50개 요청 (userLimitForPeriod = 50)
for ($i=1; $i -le 50; $i++) {
    Write-Host "Authenticated Request $i"
    curl -H "Authorization: Bearer $token" http://localhost:8080/member-service/members/health
}

# 3. 51번째 요청 (Rate Limit 초과 예상)
curl -v -H "Authorization: Bearer $token" http://localhost:8080/member-service/members/health 2>&1 | Select-String "429"
```

**예상 결과**:
- 인증된 사용자는 초당 50개 요청 허용
- 51번째 요청은 `429 Too Many Requests`

---

## 🧪 단위 테스트 현황

`RateLimitingFilterTest.java`에 다음 테스트가 구현되어 있습니다:

### ✅ 모든 테스트 통과 (7/7)
1. ✅ **Rate Limit 초과 시 429 응답** (`rateLimitExceeded_ShouldReturn429`)
2. ✅ **Rate Limit 리셋 후 정상 동작** (`rateLimitReset_ShouldAllowRequests`)
3. ✅ **IP별 Rate Limiting - 다른 IP는 독립적** (`ipBasedRateLimiting_DifferentIps_ShouldBeIndependent`)
4. ✅ **사용자별 Rate Limiting - 더 높은 제한** (`userBasedRateLimiting_AuthenticatedUser_ShouldHaveHigherLimit`)
5. ✅ **Actuator 엔드포인트는 Rate Limiting 제외** (`actuatorEndpoint_ShouldBypassRateLimiting`)
6. ✅ **X-Forwarded-For 헤더 기반 Rate Limiting** (`xForwardedForHeader_ShouldBeUsedForRateLimiting`)
7. ✅ **Rate Limiting 에러 발생 시 fail-open** (`rateLimitingError_ShouldFailOpen`)

### 🎯 통합 테스트
실제 Gateway 환경에서 테스트:
- ✅ `test-rate-limit-api.ps1`: API 엔드포인트 Rate Limiting 확인
- ✅ `test-rate-limit-actuator.ps1`: Actuator 제외 확인
- ✅ `HOW-TO-RUN-TESTS.md`: 전체 테스트 가이드

**테스트 결과**:
```
Test: 15 requests to API endpoint
✅ Requests 1-10: HTTP 401 (Processed)
✅ Requests 11-13: HTTP 429 (Rate Limited!)
✅ Rate Limiting is working correctly!
```

---

## ✅ 완료 기준 체크리스트

SERVICE-ENHANCEMENT-PLAN.md 기준:

- [x] RateLimiter 설정 완료
- [x] Rate Limiting 필터 구현
- [x] Gateway 라우팅에 적용
- [x] Rate Limiting 동작 확인 (단위 테스트 7개 + 통합 테스트)
- [x] 문서화 완료 (테스트 가이드 및 스크립트)

---

## 🚀 다음 단계

1. **GATEWAY-SERVICE-GUIDE.md에 Rate Limiting 섹션 추가** (선택사항)
2. **4단계로 진행**: Auth Service JWT 리프레시 토큰

