# MSA Service Project

Spring Boot 기반의 마이크로서비스 아키텍처(MSA) 프로젝트입니다.

## 🏗️ 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway (8080)                       │
│                    Spring Cloud Gateway                         │
│                  ┌─────────────────────────┐                    │
│                  │   Circuit Breaker       │                    │
│                  │   Load Balancing        │                    │
│                  │   CORS Configuration    │                    │
│                  └─────────────────────────┘                    │
└─────────────────────┬─────────────────┬──────────┬──────────────┘
                      │                 │          │
       ┌──────────────▼──────────────┐  │          │
       │     Member Service (8081)   │  │          │
       │   - 회원 관리 CRUD          │  │          │
       │   - Spring Data JPA        │  │          │
       │   - H2 Database            │  │          │
       └─────────────────────────────┘  │          │
                                        │          │
                        ┌───────────────▼──────────────┐
                        │     Order Service (8082)     │
                        │   - 주문 관리 CRUD           │
                        │   - OpenFeign Client         │
                        │   - Circuit Breaker          │
                        │   - Member Service 연동      │
                        └──────────────────────────────┘
                                        │
                        ┌───────────────▼──────────────┐
                        │     Auth Service (8083)      │
                        │   - JWT 인증/인가           │
                        │   - Spring Security          │
                        │   - 사용자 로그인/회원가입   │
                        │   - 역할 기반 권한 관리      │
                        └──────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   Infrastructure Services                       │
├─────────────────────────────────┬───────────────────────────────┤
│  Discovery Service (8761)       │  Config Service (8888)        │
│  - Netflix Eureka Server        │  - Spring Cloud Config        │
│  - Service Registry             │  - 중앙 설정 관리             │
│  - Service Discovery            │  - Environment별 설정         │
└─────────────────────────────────┴───────────────────────────────┘
```

## 📋 서비스 구성

### 1. **Discovery Service** (Port: 8761)
- **역할**: 서비스 레지스트리 및 디스커버리
- **기술 스택**: Netflix Eureka Server
- **핵심 기능**:
  - 마이크로서비스 등록 및 관리
  - 서비스 인스턴스 상태 모니터링
  - 서비스 간 통신을 위한 서비스 위치 제공

### 2. **Config Service** (Port: 8888)
- **역할**: 중앙 집중식 설정 관리
- **기술 스택**: Spring Cloud Config Server
- **핵심 기능**:
  - 모든 마이크로서비스의 설정 파일 중앙 관리
  - 환경별(dev, test, prod) 설정 분리
  - 설정 변경 시 동적 갱신 지원
  - Native 프로파일을 통한 로컬 파일 시스템 설정 관리

### 3. **Gateway Service** (Port: 8080)
- **역할**: API 게이트웨이 및 라우팅
- **기술 스택**: Spring Cloud Gateway, Resilience4j
- **핵심 기능**:
  - 단일 진입점을 통한 API 라우팅
  - 로드 밸런싱 및 서비스 디스커버리 연동
  - Circuit Breaker 필터를 통한 장애 허용성 (Member, Order, Auth Service)
  - JWT 기반 인증 및 인가 필터
  - CORS 설정 및 보안 정책 적용
  - 요청/응답 로깅 및 모니터링 (클라이언트 IP 추출 포함)

### 4. **Member Service** (Port: 8081)
- **역할**: 회원 관리 서비스
- **기술 스택**: Spring Boot, Spring Data JPA, H2 Database
- **핵심 기능**:
  - 회원 정보 CRUD 작업
  - 회원 상태 관리 (ACTIVE, INACTIVE, SUSPENDED)
  - 페이징 및 정렬 지원
  - Bean Validation을 통한 데이터 검증
  - 중복 회원 검증 및 예외 처리

### 5. **Order Service** (Port: 8082)
- **역할**: 주문 관리 서비스
- **기술 스택**: Spring Boot, Spring Data JPA, OpenFeign, Resilience4j
- **핵심 기능**:
  - 주문 정보 CRUD 작업
  - OpenFeign을 통한 Member Service 연동
  - Resilience4j `@CircuitBreaker` 어노테이션을 통한 Circuit Breaker 패턴 적용
  - `MemberIntegrationService`를 통한 서비스 간 통신 및 Fallback 처리
  - 주문 상태 관리 (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
  - Fallback 메커니즘을 통한 서비스 장애 대응 (실패율 임계값: 50%)

### 6. **Auth Service** (Port: 8083)
- **역할**: 인증 및 권한 관리 서비스
- **기술 스택**: Spring Boot, Spring Security, JWT, Spring Data JPA, H2 Database
- **핵심 기능**:
  - JWT 기반 인증 및 인가
  - 사용자 로그인 및 토큰 발급
  - 사용자 회원가입 및 관리
  - BCryptPasswordEncoder를 통한 비밀번호 암호화
  - 역할 기반 권한 관리 (ADMIN, USER)
  - Spring Security를 통한 보안 설정

## 🚀 기술 스택

### Core Framework
- **Spring Boot 3.3.5**
- **Spring Cloud 2023.0.3**
- **Java 21**

### Infrastructure
- **Netflix Eureka** - 서비스 디스커버리
- **Spring Cloud Config** - 설정 관리
- **Spring Cloud Gateway** - API 게이트웨이

### Security
- **Spring Security** - 인증 및 인가 프레임워크
- **JWT (JSON Web Token)** - 토큰 기반 인증
- **BCryptPasswordEncoder** - 비밀번호 암호화

### Communication
- **OpenFeign** - 선언적 REST 클라이언트
- **Resilience4j** - Circuit Breaker

### Data & Persistence
- **Spring Data JPA** - 데이터 접근 계층
- **H2 Database** - 인메모리 데이터베이스
- **Bean Validation** - 데이터 검증

### Build & Deployment
- **Gradle Multi-Module** - 빌드 도구
- **Gradle Wrapper** - 빌드 환경 통일

## 🛠️ 빌드 및 실행

### 1. 프로젝트 빌드
```bash
./gradlew build
```

### 2. 서비스 실행 순서
1. **Discovery Service**
   ```bash
   ./gradlew discovery-service:bootRun
   ```
   
2. **Config Service**
   ```bash
   ./gradlew config-service:bootRun
   ```
   
3. **Gateway Service**
   ```bash
   ./gradlew gateway-service:bootRun
   ```
   
4. **Member Service**
   ```bash
   ./gradlew member-service:bootRun
   ```
   
5. **Order Service**
   ```bash
   ./gradlew order-service:bootRun
   ```
   
6. **Auth Service**
   ```bash
   ./gradlew auth-service:bootRun
   ```

### 3. 서비스 확인
- Discovery Service: http://localhost:8761
- Config Service: http://localhost:8888
- Gateway Service: http://localhost:8080
- Member Service: http://localhost:8081
- Order Service: http://localhost:8082
- Auth Service: http://localhost:8083

## 📡 API 엔드포인트

### Member Service (via Gateway)
- `GET /api/members` - 회원 목록 조회
- `GET /api/members/{id}` - 회원 상세 조회
- `POST /api/members` - 회원 생성
- `PUT /api/members/{id}` - 회원 정보 수정
- `DELETE /api/members/{id}` - 회원 삭제

### Order Service (via Gateway)
- `GET /api/orders` - 주문 목록 조회
- `GET /api/orders/{id}` - 주문 상세 조회
- `POST /api/orders` - 주문 생성
- `PUT /api/orders/{id}` - 주문 정보 수정
- `DELETE /api/orders/{id}` - 주문 삭제

### Auth Service (via Gateway)
- `POST /api/auth/login` - 사용자 로그인 및 JWT 토큰 발급
- `GET /api/auth/health` - 서비스 상태 확인

### Auth Service (직접 접근)
- `POST /auth/login` - 사용자 로그인 및 JWT 토큰 발급
- `GET /auth/health` - 서비스 상태 확인

### 테스트 엔드포인트 (Order Service 직접 접근)
- `GET /test/member/{id}` - Member Service 연동 테스트
- `GET /test/circuit-breaker/status` - Circuit Breaker 상태 확인

### 모니터링 엔드포인트
- `GET /actuator/health` - 서비스 상태 확인 (모든 서비스)
- `GET /actuator/circuitbreakers` - Circuit Breaker 상태 (Order Service, Gateway Service)
- `GET /actuator/circuitbreakerevents/{name}` - Circuit Breaker 이벤트 (Order Service, Gateway Service)
- `GET /actuator/gateway/routes` - Gateway 라우팅 규칙 (Gateway Service만)

## 🔄 서비스 간 통신

### OpenFeign을 통한 서비스 간 통신
Order Service는 OpenFeign을 사용하여 Member Service와 통신합니다:

```java
@FeignClient(
    name = "member-service",
    url = "${member-service.url:http://localhost:8081}"
    // 주의: OpenFeign Fallback을 사용하지 않습니다.
    // 대신 Resilience4j의 @CircuitBreaker 어노테이션을 Service 레벨에서 사용합니다.
)
public interface MemberServiceClient {
    @GetMapping("/members/{id}")
    MemberDto getMemberById(@PathVariable("id") Long id);
}
```

### Circuit Breaker 및 Fallback
Resilience4j의 `@CircuitBreaker` 어노테이션을 사용하여 Circuit Breaker 패턴을 적용합니다:

```java
@Service
public class MemberIntegrationService {
    @CircuitBreaker(name = "member-service", fallbackMethod = "validateMemberFallback")
    public MemberDto validateMember(Long memberId) {
        return memberServiceClient.getMemberById(memberId);
    }
    
    public MemberDto validateMemberFallback(Long memberId, Exception e) {
        // Fallback 처리: "알 수 없는 사용자" 반환
        return new MemberDto(/* fallback data */);
    }
}
```

**Circuit Breaker 설정**:
- 실패율 임계값: 50% (50% 이상 실패 시 OPEN 상태로 전환)
- 슬라이딩 윈도우 크기: 10
- 최소 호출 횟수: 5
- OPEN 상태 유지 시간: 10초

**Fallback 동작**:
- Member Service 장애 시 자동으로 Fallback 메서드 실행
- "알 수 없는 사용자"로 처리하여 주문 처리를 계속 진행
- Circuit Breaker가 실패를 올바르게 카운트하여 메트릭 수집
- 서비스 복구 시 자동으로 정상 통신 재개

## 🏥 모니터링 및 관리

### Actuator 엔드포인트
각 서비스는 Spring Boot Actuator를 통한 모니터링 기능을 제공합니다:
- Health Check: `/actuator/health`
- Gateway Routes: `/actuator/gateway/routes` (Gateway Service만)
- Circuit Breaker 상태: `/actuator/circuitbreakers` (Order Service, Gateway Service)
- Circuit Breaker 이벤트: `/actuator/circuitbreakerevents/{name}` (Order Service, Gateway Service)

### Eureka Dashboard
서비스 등록 상태는 Eureka Dashboard에서 확인할 수 있습니다:
- URL: http://localhost:8761

## 📁 프로젝트 구조

```
msa-service/
├── discovery-service/          # Eureka Server
├── config-service/             # Config Server
│   └── src/main/resources/config-repo/  # 설정 파일들
├── gateway-service/            # API Gateway
├── member-service/             # 회원 관리 서비스
├── order-service/              # 주문 관리 서비스
├── auth-service/               # 인증 및 권한 관리 서비스
├── build.gradle               # 루트 빌드 스크립트
├── settings.gradle            # 멀티 모듈 설정
└── README.md                  # 프로젝트 문서
```

## 🧪 테스트

각 서비스는 단위 테스트, 통합 테스트를 포함하고 있습니다:

### 테스트 실행
```bash
./gradlew test
```

### 수동 테스트
API 테스트 파일들이 제공됩니다:
- `member-service/api-test.http`
- `order-service/api-test.http`

## 🔧 개발 환경 설정

### 필수 요구사항
- Java 21 이상
- Gradle 8.x 이상 (Wrapper 포함)

### IDE 설정
- Lombok 플러그인 설치 권장 (현재는 수동 구현으로 대체)
- Spring Boot DevTools를 통한 자동 재시작 지원

## 📚 추가 문서

- [FULL-SYSTEM-TEST.md](FULL-SYSTEM-TEST.md) - 전체 시스템 테스트 가이드
- [order-service/fallback-test-guide.md](order-service/fallback-test-guide.md) - Fallback 테스트 가이드
- [order-service/test-scenarios.md](order-service/test-scenarios.md) - 테스트 시나리오