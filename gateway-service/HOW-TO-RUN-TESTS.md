# 🚀 Gateway Service Rate Limiting 테스트 실행 가이드

이 가이드는 RATE-LIMITING-TEST-GUIDE.md의 테스트를 실제로 실행하는 방법을 안내합니다.

## 📋 목차
1. [사전 준비](#사전-준비)
2. [서비스 시작](#서비스-시작)
3. [자동화 테스트 실행](#자동화-테스트-실행)
4. [수동 테스트 실행](#수동-테스트-실행)
5. [문제 해결](#문제-해결)

---

## 1️⃣ 사전 준비

### 필수 도구 설치 확인
```powershell
# Java 17 이상
java -version

# Gradle
.\gradlew --version

# curl (PowerShell에 기본 포함)
curl --version
```

### 프로젝트 빌드
```powershell
# 프로젝트 루트에서 실행
.\gradlew clean build -x test
```

---

## 2️⃣ 서비스 시작

Rate Limiting 테스트를 위해 **5개의 서비스**를 순서대로 시작해야 합니다.

### 🔹 터미널 1: Config Service 시작 (포트 8888)
```powershell
.\gradlew config-service:bootRun
```

**확인**: http://localhost:8888/actuator/health → `{"status":"UP"}`

### 🔹 터미널 2: Discovery Service 시작 (포트 8761)
```powershell
.\gradlew discovery-service:bootRun
```

**확인**: http://localhost:8761 → Eureka Dashboard 열림

### 🔹 터미널 3: Auth Service 시작 (포트 8081)
```powershell
.\gradlew auth-service:bootRun
```

**확인**: http://localhost:8081/actuator/health

### 🔹 터미널 4: Member Service 시작 (포트 8082)
```powershell
.\gradlew member-service:bootRun
```

**확인**: http://localhost:8082/actuator/health

### 🔹 터미널 5: Gateway Service 시작 (포트 8080)
```powershell
.\gradlew gateway-service:bootRun
```

**확인**: http://localhost:8080/actuator/health

### ⏱️ 서비스 준비 시간
모든 서비스가 Discovery Service에 등록될 때까지 **약 30초~1분** 대기하세요.

**Discovery 등록 확인**:
```powershell
# Gateway를 통해 Member Service Health 체크
curl http://localhost:8080/member-service/members/health
# 결과: {"status":"UP"}
```

---

## 3️⃣ 자동화 테스트 실행

### 🎯 테스트 스크립트

새로운 PowerShell 터미널을 열고:

#### 옵션 1: API 엔드포인트 Rate Limiting 테스트 (권장)
```powershell
# 프로젝트 루트에서 실행
.\gateway-service\test-rate-limit-api.ps1
```

**테스트 내용**:
- 15개 요청 전송 (Rate Limit = 10)
- 처음 10개: HTTP 401/200 (처리됨)
- 11번째 이후: HTTP 429 (Rate Limited!)

#### 옵션 2: Actuator 엔드포인트 제외 확인
```powershell
.\gateway-service\test-rate-limit-actuator.ps1
```

**테스트 내용**:
- Actuator 엔드포인트는 Rate Limiting에서 제외됨
- 11개 이상 요청해도 모두 200 OK

### 📊 예상 출력 (test-rate-limit-api.ps1)
```
============================================
Gateway Rate Limiting Test - API Endpoint
============================================

Endpoint: http://localhost:8080/member-service/members/1

Test: Sending 15 requests (Rate Limit = 10)
------------------------------------------------
  Request  1 : HTTP 401 (Unauthorized)
  Request  2 : HTTP 401 (Unauthorized)
  ...
  Request 10 : HTTP 401 (Unauthorized)
  Request 11 : HTTP 429 (RATE LIMITED!)
  Request 12 : HTTP 429 (RATE LIMITED!)
  Request 13 : HTTP 429 (RATE LIMITED!)
  Request 14 : HTTP 401 (Unauthorized)
  Request 15 : HTTP 401 (Unauthorized)

============================================
Results:
============================================
  HTTP 200 (OK):           0
  HTTP 401 (Unauthorized): 12
  HTTP 429 (Rate Limited): 3
  Other errors:            0

SUCCESS: Rate Limiting is working!
============================================
```

---

## 4️⃣ 수동 테스트 실행

자동화 스크립트 대신 개별 테스트를 직접 실행할 수 있습니다.

### 테스트 1: Rate Limit 초과 확인

```powershell
# 11개 요청 (10개까지 허용)
for ($i=1; $i -le 11; $i++) {
    Write-Host "Request $i"
    $response = curl -X GET http://localhost:8080/member-service/members/health -s -w "`n%{http_code}"
    $statusCode = ($response -split "`n")[-1]
    Write-Host "Status: $statusCode"
}
```

**예상 결과**: 처음 10개는 200, 11번째는 429

### 테스트 2: Rate Limit 리셋 확인

```powershell
# 10개 요청
for ($i=1; $i -le 10; $i++) {
    curl -s http://localhost:8080/member-service/members/health | Out-Null
}

# 11번째 요청 (429 예상)
curl -v http://localhost:8080/member-service/members/health 2>&1 | Select-String "429"

# 1초 대기
Start-Sleep -Seconds 2

# 다시 요청 (200 예상)
curl -v http://localhost:8080/member-service/members/health 2>&1 | Select-String "200"
```

### 테스트 3: IP별 Rate Limiting

```powershell
# IP 1에서 10개 요청
for ($i=1; $i -le 10; $i++) {
    curl -H "X-Forwarded-For: 192.168.1.100" http://localhost:8080/member-service/members/health -s
}

# IP 2에서 10개 요청 (독립적으로 동작)
for ($i=1; $i -le 10; $i++) {
    curl -H "X-Forwarded-For: 192.168.1.200" http://localhost:8080/member-service/members/health -s
}
```

### 테스트 4: 사용자별 Rate Limiting

```powershell
# 1. JWT 토큰 발급
$loginBody = @{
    username = "admin"
    password = "password123"
} | ConvertTo-Json

$response = curl -X POST http://localhost:8080/auth-service/auth/login `
    -H "Content-Type: application/json" `
    -d $loginBody -s | ConvertFrom-Json

$token = $response.accessToken

# 2. 인증된 사용자로 50개 요청
for ($i=1; $i -le 50; $i++) {
    Write-Host "Request $i"
    curl -H "Authorization: Bearer $token" http://localhost:8080/member-service/members/health -s
}

# 3. 51번째 요청 (429 예상)
curl -v -H "Authorization: Bearer $token" http://localhost:8080/member-service/members/health 2>&1 | Select-String "429"
```

---

## 5️⃣ 문제 해결

### ❌ "Connection refused" 오류

**원인**: 서비스가 시작되지 않았거나 포트가 다름

**해결**:
```powershell
# 포트 사용 확인
netstat -ano | findstr "8080"
netstat -ano | findstr "8888"

# 프로세스 강제 종료 (필요시)
Stop-Process -Id <PID> -Force
```

### ❌ "404 Not Found" 오류

**원인**: Member Service가 Discovery에 등록되지 않음

**해결**:
```powershell
# Discovery Dashboard 확인
# http://localhost:8761

# Gateway 로그 확인 (터미널 5)
# "Mapped [GET /member-service/**]" 메시지 확인

# 1분 대기 후 재시도
Start-Sleep -Seconds 60
```

### ❌ Rate Limiting이 동작하지 않음

**원인**: Gateway 설정 문제

**해결**:
```powershell
# Gateway 설정 확인
curl http://localhost:8888/gateway-service/default

# 출력에서 rate-limiting 필터 설정 확인
```

### ❌ JWT 토큰 발급 실패

**원인**: Auth Service DB 초기화 필요

**해결**:
```powershell
# Auth Service 재시작
# Ctrl+C로 종료 후 다시 시작
.\gradlew auth-service:bootRun

# 기본 사용자 확인 (admin/password123)
```

---

## 📊 Rate Limiting 설정값 확인

현재 설정은 `config-repo/gateway-service.yml`에서 확인:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: member-service
          filters:
            - name: RateLimiting
              args:
                limit-for-period: 10              # IP별 초당 10개
                user-limit-for-period: 50         # 사용자별 초당 50개
                limit-refresh-period-seconds: 1   # 1초마다 리셋
                user-based-rate-limit-enabled: true
```

---

## ✅ 성공 기준

모든 테스트가 통과하면:

- ✅ Rate Limit 초과 시 429 응답
- ✅ Rate Limit 리셋 후 정상 동작
- ✅ IP별 독립적 Rate Limiting
- ✅ 사용자별 높은 Rate Limit 적용

**다음 단계**: GATEWAY-SERVICE-GUIDE.md 문서화 및 4단계 진행

---

## 🎯 빠른 시작 (올인원)

**모든 서비스를 한 번에 시작하려면** (PowerShell 여러 창 열기):

```powershell
# PowerShell 창 1-5를 열고 각각 실행
Start-Process powershell -ArgumentList "-NoExit", "-Command", ".\gradlew config-service:bootRun"
Start-Sleep -Seconds 10
Start-Process powershell -ArgumentList "-NoExit", "-Command", ".\gradlew discovery-service:bootRun"
Start-Sleep -Seconds 15
Start-Process powershell -ArgumentList "-NoExit", "-Command", ".\gradlew auth-service:bootRun"
Start-Sleep -Seconds 10
Start-Process powershell -ArgumentList "-NoExit", "-Command", ".\gradlew member-service:bootRun"
Start-Sleep -Seconds 10
Start-Process powershell -ArgumentList "-NoExit", "-Command", ".\gradlew gateway-service:bootRun"

# 1분 대기 후 테스트 실행
Start-Sleep -Seconds 60
.\gateway-service\TEST-RATE-LIMITING.ps1
```

---

**테스트 준비 완료!** 🚀

