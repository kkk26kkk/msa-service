# Auth Service JWT Refresh Token 테스트 가이드

## 📋 개요

4단계(Auth Service JWT Refresh Token) 구현에 대한 포괄적인 테스트 코드입니다.

## 🧪 테스트 파일 목록

### 1. TokenServiceTest
**위치**: `auth-service/src/test/java/com/example/auth/service/TokenServiceTest.java`

**테스트 대상**: `TokenService` (Refresh Token 관리 서비스)

**테스트 시나리오** (총 10개):
- ✅ Refresh Token 저장 - 기존 토큰이 없는 경우
- ✅ Refresh Token 저장 - 기존 토큰이 있는 경우 삭제 후 저장
- ✅ Refresh Token 검증 - 유효한 토큰
- ✅ Refresh Token 검증 - 토큰이 존재하지 않는 경우
- ✅ Refresh Token 검증 - 만료된 토큰
- ✅ Refresh Token 검증 - revoked된 토큰은 조회되지 않음
- ✅ Refresh Token 삭제 - 토큰이 존재하는 경우
- ✅ Refresh Token 삭제 - 토큰이 존재하지 않는 경우
- ✅ 사용자명으로 모든 Refresh Token 삭제
- ✅ 사용자명으로 모든 Refresh Token revoke
- ✅ 만료된 토큰 정리

**테스트 방식**: 
- 단위 테스트 (Mockito 사용)
- `@ExtendWith(MockitoExtension.class)`

---

### 2. JwtTokenProviderTest
**위치**: `auth-service/src/test/java/com/example/auth/service/JwtTokenProviderTest.java`

**테스트 대상**: `JwtTokenProvider` (JWT 토큰 생성 및 파싱)

**테스트 시나리오** (총 11개):
- ✅ Access Token 생성 - 사용자명과 역할 정보 포함
- ✅ Refresh Token 생성 - 사용자명만 포함 (역할 정보 제외)
- ✅ Access Token과 Refresh Token의 만료 시간 차이 확인
- ✅ Token 유효성 검증 - 유효한 토큰
- ✅ Token 유효성 검증 - 잘못된 토큰
- ✅ Token에서 사용자명 추출
- ✅ Claims 파싱
- ✅ Access Token 유효 기간 조회
- ✅ Refresh Token 유효 기간 조회
- ✅ 여러 역할을 가진 사용자의 Access Token 생성
- ✅ Refresh Token에는 역할 정보가 없음 확인

**테스트 방식**: 
- 단위 테스트
- 실제 JWT 생성 및 파싱 테스트

---

### 3. RefreshTokenRepositoryTest
**위치**: `auth-service/src/test/java/com/example/auth/repository/RefreshTokenRepositoryTest.java`

**테스트 대상**: `RefreshTokenRepository` (Refresh Token DB 접근)

**테스트 시나리오** (총 10개):
- ✅ 토큰으로 RefreshToken 조회
- ✅ 존재하지 않는 토큰 조회
- ✅ 사용자명으로 RefreshToken 조회
- ✅ 사용자명으로 활성화된 RefreshToken 조회 - revoked=false
- ✅ 토큰으로 활성화된 RefreshToken 조회 - revoked=false
- ✅ revoked된 토큰은 findByTokenAndRevokedFalse로 조회되지 않음
- ✅ 만료된 토큰 삭제
- ✅ 사용자명으로 모든 RefreshToken 삭제
- ✅ 사용자명으로 모든 RefreshToken을 revoked 상태로 변경
- ✅ unique 제약조건 - 중복 토큰 저장 불가

**테스트 방식**: 
- Repository 레이어 테스트
- `@DataJpaTest` (H2 인메모리 DB 사용)
- `EntityManager`를 사용한 영속성 컨텍스트 관리

---

### 4. AuthControllerIntegrationTest
**위치**: `auth-service/src/test/java/com/example/auth/controller/AuthControllerIntegrationTest.java`

**테스트 대상**: `AuthController` (인증 API 엔드포인트)

**테스트 시나리오** (총 12개):
1. ✅ **로그인 - 정상 케이스**: Access Token과 Refresh Token 발급
2. ✅ **로그인 - 잘못된 비밀번호**: 401 Unauthorized
3. ✅ **로그인 - 존재하지 않는 사용자**: 401 Unauthorized
4. ✅ **토큰 갱신 - 유효한 Refresh Token**: 새로운 토큰 발급
5. ✅ **토큰 갱신 - 만료된 Refresh Token**: 401 Unauthorized
6. ✅ **토큰 갱신 - 존재하지 않는 Refresh Token**: 401 Unauthorized
7. ✅ **토큰 갱신 - revoked된 Refresh Token**: 401 Unauthorized
8. ✅ **로그아웃 - 정상 케이스**: Refresh Token 삭제
9. ✅ **로그아웃 - 존재하지 않는 토큰**: 정상 처리
10. ✅ **헬스 체크 - 정상 응답**
11. ✅ **로그인 - 빈 username**: 400 Bad Request
12. ✅ **토큰 갱신 - 빈 refreshToken**: 400 Bad Request

**테스트 방식**: 
- 통합 테스트
- `@SpringBootTest + @AutoConfigureMockMvc`
- `MockMvc`를 사용한 HTTP 요청/응답 테스트
- 실제 DB와 Spring Security 설정 포함

---

### 5. AuthServiceApplicationTests
**위치**: `auth-service/src/test/java/com/example/auth/AuthServiceApplicationTests.java`

**테스트 대상**: Spring Boot Application Context

**테스트 시나리오**:
- ✅ 컨텍스트 로딩 테스트

**테스트 방식**: 
- Spring Boot 기본 컨텍스트 테스트

---

## 📊 테스트 통계

| 테스트 파일 | 테스트 수 | 상태 |
|-----------|---------|------|
| TokenServiceTest | 10 | ✅ 통과 |
| JwtTokenProviderTest | 11 | ✅ 통과 |
| RefreshTokenRepositoryTest | 10 | ✅ 통과 |
| AuthControllerIntegrationTest | 12 | ✅ 통과 |
| AuthServiceApplicationTests | 1 | ✅ 통과 |
| **총계** | **44** | **✅ 모두 통과** |

---

## 🚀 테스트 실행 방법

### 전체 테스트 실행
```powershell
.\gradlew auth-service:test
```

### 특정 테스트 클래스 실행
```powershell
# TokenService 테스트만 실행
.\gradlew auth-service:test --tests TokenServiceTest

# JwtTokenProvider 테스트만 실행
.\gradlew auth-service:test --tests JwtTokenProviderTest

# RefreshTokenRepository 테스트만 실행
.\gradlew auth-service:test --tests RefreshTokenRepositoryTest

# AuthController 통합 테스트만 실행
.\gradlew auth-service:test --tests AuthControllerIntegrationTest
```

### 특정 테스트 메서드 실행
```powershell
.\gradlew auth-service:test --tests "AuthControllerIntegrationTest.login_WithValidCredentials_ShouldReturnTokens"
```

### 테스트 캐시 무시하고 재실행
```powershell
.\gradlew auth-service:test --rerun-tasks
```

---

## 🔧 테스트 설정

### application-test.yml
**위치**: `auth-service/src/test/resources/application-test.yml`

**주요 설정**:
```yaml
spring:
  cloud:
    config:
      enabled: false  # Config Server 비활성화
  datasource:
    url: jdbc:h2:mem:testdb  # H2 인메모리 DB
  jpa:
    hibernate:
      ddl-auto: create-drop  # 테스트 종료 시 스키마 삭제

security:
  jwt:
    secret: test-secret-key...
    access-token-validity-seconds: 900
    refresh-token-validity-seconds: 604800

eureka:
  client:
    enabled: false  # Eureka 비활성화
```

---

## 🎯 테스트 커버리지

### 주요 커버리지 항목

#### 1. TokenService
- ✅ Refresh Token 저장 로직
- ✅ 기존 토큰 삭제 및 교체 로직
- ✅ Refresh Token 검증 (만료, revoked 확인)
- ✅ Refresh Token 삭제 및 revoke 로직
- ✅ 만료된 토큰 정리 로직

#### 2. JwtTokenProvider
- ✅ Access Token 생성 (역할 포함)
- ✅ Refresh Token 생성 (역할 제외)
- ✅ JWT Claims 파싱
- ✅ 토큰 유효성 검증
- ✅ 토큰 타입 구분 (access vs refresh)

#### 3. RefreshTokenRepository
- ✅ CRUD 기본 연산
- ✅ 커스텀 쿼리 메서드
- ✅ `@Modifying` 쿼리 (DELETE, UPDATE)
- ✅ Unique 제약조건
- ✅ revoked 상태 필터링

#### 4. AuthController
- ✅ POST /auth/login - 로그인 및 토큰 발급
- ✅ POST /auth/refresh - 토큰 갱신
- ✅ POST /auth/logout - 로그아웃 (토큰 무효화)
- ✅ GET /auth/health - 헬스 체크
- ✅ 유효성 검증 (`@Valid`)
- ✅ 에러 핸들링 (401, 400)

---

## 💡 주요 테스트 패턴 및 기법

### 1. 단위 테스트 (Unit Test)
- **Mockito를 사용한 의존성 모킹**
- `@Mock`으로 의존성 주입
- `@InjectMocks`로 테스트 대상 생성
- `when().thenReturn()` 패턴

### 2. Repository 테스트
- `@DataJpaTest`로 JPA 레이어만 테스트
- H2 인메모리 DB 사용
- `EntityManager`로 영속성 컨텍스트 관리
- 트랜잭션 자동 롤백

### 3. 통합 테스트 (Integration Test)
- `@SpringBootTest`로 전체 컨텍스트 로드
- `@AutoConfigureMockMvc`로 MockMvc 자동 설정
- `@Transactional`로 각 테스트 후 롤백
- 실제 HTTP 요청/응답 시뮬레이션

### 4. 테스트 격리 (Test Isolation)
- `@BeforeEach`에서 데이터 초기화
- `refreshTokenRepository.deleteAll()`로 데이터 정리
- `Thread.sleep(1000)`으로 JWT 토큰 생성 시간 차이 보장

---

## ⚠️ 주의사항

### 1. JWT 토큰 생성 시간 이슈
- JWT의 `iat` (Issued At) 클레임은 **초 단위**
- 동일한 초에 생성된 토큰은 동일한 값을 가짐
- 테스트에서 `Thread.sleep(1000)` 사용하여 해결

### 2. Config Server 및 Eureka 비활성화
- 테스트 환경에서는 외부 서비스 비활성화 필수
- `spring.cloud.config.enabled=false`
- `eureka.client.enabled=false`

### 3. 영속성 컨텍스트 관리
- `@Modifying` 쿼리 후 `flush()` 필요
- UPDATE 쿼리 후 `entityManager.clear()` 필요

---

## 📝 테스트 코드 예시

### TokenService 단위 테스트
```java
@Test
@DisplayName("Refresh Token 저장 - 기존 토큰이 있는 경우 삭제 후 저장")
void saveRefreshToken_WhenExistingToken_ShouldDeleteAndSave() {
    // Given
    RefreshToken existingToken = RefreshToken.builder()
            .id(1L)
            .token("old-token")
            .username(testUsername)
            .expiresAt(futureTime)
            .build();
    
    when(refreshTokenRepository.findByUsername(testUsername))
            .thenReturn(Optional.of(existingToken));
    when(refreshTokenRepository.save(any(RefreshToken.class)))
            .thenAnswer(invocation -> {
                RefreshToken token = invocation.getArgument(0);
                token.setId(2L);
                return token;
            });

    // When
    RefreshToken result = tokenService.saveRefreshToken(testToken, testUsername, futureTime);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getToken()).isEqualTo(testToken);
    
    verify(refreshTokenRepository, times(1)).delete(existingToken);
    verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
}
```

### AuthController 통합 테스트
```java
@Test
@DisplayName("로그인 - 정상 케이스: Access Token과 Refresh Token 발급")
void login_WithValidCredentials_ShouldReturnTokens() throws Exception {
    // Given
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername(testUser.getUsername());
    loginRequest.setPassword(testPassword);

    // When & Then
    MvcResult result = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn();

    // Refresh Token이 데이터베이스에 저장되었는지 확인
    AuthResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            AuthResponse.class
    );
    
    RefreshToken savedToken = refreshTokenRepository
            .findByUsername(testUser.getUsername()).orElse(null);
    assertThat(savedToken).isNotNull();
    assertThat(savedToken.getToken()).isEqualTo(response.getRefreshToken());
}
```

---

## ✅ 완료 확인

4단계 구현에 대한 포괄적인 테스트 코드가 완성되었습니다:

- ✅ 44개 테스트 모두 통과
- ✅ 단위 테스트, Repository 테스트, 통합 테스트 포함
- ✅ 주요 기능 및 엣지 케이스 커버
- ✅ 테스트 격리 및 재현성 보장
- ✅ 실제 HTTP 요청/응답 시뮬레이션

---

## 📚 참고 자료

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [AssertJ Documentation](https://assertj.github.io/doc/)


