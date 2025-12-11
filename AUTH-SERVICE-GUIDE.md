# 🔐 2단계: Auth Service 상세 가이드

이 문서는 MSA 프로젝트의 **Auth Service**에 대한 상세한 설명입니다. JWT 기반 인증/인가를 구현한 서비스입니다.

---

## 📍 목차

1. [Auth Service 개요](#1-auth-service-개요)
2. [코드 구조 분석](#2-코드-구조-분석)
3. [인증 흐름 상세 분석](#3-인증-흐름-상세-분석)
4. [JWT 토큰 구조](#4-jwt-토큰-구조)
5. [보안 설정](#5-보안-설정)
6. [실습 가이드](#6-실습-가이드)

---

## 1. Auth Service 개요

### 1.1 역할

**Auth Service**는 JWT(JSON Web Token) 기반 인증/인가를 담당하는 서비스입니다.

**주요 기능**:
- 사용자 로그인 및 JWT 토큰 발급
- 사용자 등록 및 관리
- 비밀번호 암호화 (BCrypt)
- 역할 기반 권한 관리 (ROLE_ADMIN, ROLE_USER)

### 1.2 기술 스택

- **Spring Boot**: 웹 애플리케이션 프레임워크
- **Spring Security**: 인증 및 권한 관리
- **JWT (jjwt)**: 토큰 생성 및 검증
- **Spring Data JPA**: 데이터베이스 접근
- **H2 Database**: 인메모리 데이터베이스 (개발용)
- **BCrypt**: 비밀번호 암호화

### 1.3 서비스 포트

- **포트**: 8083
- **접속 URL**: http://localhost:8083

---

## 2. 코드 구조 분석

### 2.1 프로젝트 구조

```
auth-service/
├── src/main/java/com/example/auth/
│   ├── AuthServiceApplication.java      # 애플리케이션 진입점
│   ├── config/
│   │   ├── SecurityConfig.java          # Spring Security 설정
│   │   ├── JwtProperties.java          # JWT 설정 속성
│   │   └── DataInitializer.java        # 초기 데이터 생성
│   ├── controller/
│   │   ├── AuthController.java         # REST API 엔드포인트
│   │   └── GlobalExceptionHandler.java # 전역 예외 처리
│   ├── dto/
│   │   ├── LoginRequest.java           # 로그인 요청 DTO
│   │   └── AuthResponse.java           # 인증 응답 DTO
│   ├── entity/
│   │   └── AuthUser.java               # 사용자 엔티티
│   ├── repository/
│   │   └── AuthUserRepository.java     # 사용자 리포지토리
│   ├── security/
│   │   └── SecurityRoles.java          # 역할 상수 정의
│   └── service/
│       ├── AuthService.java            # 사용자 등록 서비스
│       ├── AuthUserDetailsService.java # Spring Security UserDetailsService
│       └── JwtTokenProvider.java       # JWT 토큰 생성/검증
```

### 2.2 메인 애플리케이션 클래스

```java
@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

**핵심 어노테이션**:
- `@SpringBootApplication`: Spring Boot 애플리케이션
- `@EnableDiscoveryClient`: Eureka Discovery Service에 등록

### 2.3 엔티티: AuthUser

```java
@Entity
@Table(name = "auth_users")
public class AuthUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;  // BCrypt로 암호화된 비밀번호

    @Column(nullable = false, length = 200)
    private String roles;     // 쉼표로 구분된 역할 문자열 (예: "ADMIN,USER")

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

**핵심 필드**:
- `username`: 사용자명 (고유값)
- `password`: 암호화된 비밀번호 (BCrypt)
- `roles`: 역할 문자열 (예: "ADMIN,USER")

**역할 저장 형식**:
- 데이터베이스에는 "ROLE_" 접두사 없이 저장 (예: "ADMIN,USER")
- Spring Security에서 사용할 때 "ROLE_" 접두사 자동 추가

### 2.4 JWT 토큰 제공자: JwtTokenProvider

```java
@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;  // JWT 서명용 비밀키
    private final long validitySeconds; // 토큰 유효 기간

    public JwtTokenProvider(JwtProperties properties) {
        // 설정 파일의 secret 문자열을 HMAC SHA-256 알고리즘용 SecretKey로 변환
        this.secretKey = Keys.hmacShaKeyFor(
            properties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
        this.validitySeconds = properties.getAccessTokenValiditySeconds();
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(validitySeconds);

        // 사용자의 권한(역할) 목록을 문자열 리스트로 변환
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // JWT 토큰 빌드 및 생성
        return Jwts.builder()
                .subject(authentication.getName())        // 사용자명
                .claim("roles", roles)                   // 역할 목록
                .issuedAt(Date.from(now))                 // 발급 시간
                .expiration(Date.from(expiry))            // 만료 시간
                .signWith(secretKey)                      // 서명
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)                    // 서명 검증
                .build()
                .parseSignedClaims(token)                // 토큰 파싱
                .getPayload();                            // Claims 추출
    }
}
```

**주요 메서드**:
- `generateToken()`: 인증된 사용자 정보로 JWT 토큰 생성
- `parseClaims()`: JWT 토큰을 파싱하여 Claims 추출

**JWT 토큰에 포함되는 정보**:
- `subject`: 사용자명 (username)
- `roles`: 사용자 역할 배열 (예: ["ROLE_ADMIN", "ROLE_USER"])
- `issuedAt`: 토큰 발급 시간
- `expiration`: 토큰 만료 시간

### 2.5 사용자 등록 서비스: AuthService

```java
@Service
public class AuthService {
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthUser registerUser(String username, String password, String... roles) {
        // 기존 사용자가 있으면 반환
        if (authUserRepository.existsByUsername(username)) {
            return authUserRepository.findByUsername(username).orElseThrow();
        }
        
        // 역할 문자열 정규화
        String roleValue = (roles == null || roles.length == 0)
                ? SecurityRoles.USER  // 기본값 "USER"
                : Arrays.stream(roles)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                    .map(String::toUpperCase)
                    .distinct()
                    .collect(Collectors.joining(","));
        
        // AuthUser 객체 생성 및 저장
        AuthUser user = AuthUser.builder()
                .username(username)
                .password(passwordEncoder.encode(password))  // BCrypt 암호화
                .roles(roleValue)
                .build();
        return authUserRepository.save(user);
    }
}
```

**역할 정규화 규칙**:
1. "ROLE_" 접두사 제거
2. 대문자로 변환
3. 중복 제거
4. 쉼표로 연결

**예시**:
- 입력: `["ADMIN", "ROLE_USER"]` → 저장: `"ADMIN,USER"`
- 입력: `null` → 저장: `"USER"`

### 2.6 Spring Security UserDetailsService: AuthUserDetailsService

```java
@Service
public class AuthUserDetailsService implements UserDetailsService {
    private final AuthUserRepository authUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return authUserRepository.findByUsername(username)
                .map(user -> new User(
                        user.getUsername(),              // 사용자명
                        user.getPassword(),              // 암호화된 비밀번호
                        mapRoles(user.getRoles())        // 권한 목록
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private List<SimpleGrantedAuthority> mapRoles(String roles) {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
```

**역할 매핑 규칙**:
- 데이터베이스: `"ADMIN,USER"` (접두사 없음)
- Spring Security: `[ROLE_ADMIN, ROLE_USER]` (접두사 자동 추가)

### 2.7 보안 설정: SecurityConfig

```java
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)  // CSRF 비활성화
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 경로
                .requestMatchers("/auth/login", "/auth/health", "/actuator/**", "/h2-console/**")
                    .permitAll()
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated())
            // Stateless 세션 정책 (JWT 기반 인증)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**핵심 설정**:
- **CSRF 비활성화**: REST API이므로 불필요
- **permitAll 경로**: `/auth/login`, `/auth/health`, `/actuator/**`, `/h2-console/**`
- **Stateless 세션**: JWT 기반 인증이므로 세션 사용 안 함
- **BCryptPasswordEncoder**: 비밀번호 암호화

### 2.8 REST API 컨트롤러: AuthController

```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // 1단계: 사용자 인증
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(), 
                    request.getPassword()
                )
        );
        
        // 2단계: SecurityContext에 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 3단계: JWT 토큰 생성
        String token = jwtTokenProvider.generateToken(authentication);
        
        // 4단계: 사용자 권한 추출
        String[] roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toArray(String[]::new);
        
        // 5단계: 응답 생성
        AuthResponse response = new AuthResponse(
                token,                                    // JWT 토큰
                "Bearer",                                 // 토큰 타입
                jwtTokenProvider.getValiditySeconds(),    // 유효 기간
                authentication.getName(),                // 사용자명
                roles                                     // 역할 배열
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```

**API 엔드포인트**:
- `POST /auth/login`: 로그인 및 JWT 토큰 발급
- `GET /auth/health`: 헬스 체크

### 2.9 초기 데이터 생성: DataInitializer

```java
@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initializeDefaultUser(AuthService authService) {
        return args -> {
            // 관리자 계정 생성
            authService.registerUser("admin", "password123", SecurityRoles.ADMIN);
            
            // 일반 사용자 계정 생성
            authService.registerUser("member", "password123", SecurityRoles.USER);
        };
    }
}
```

**생성되는 기본 사용자**:
- `admin` / `password123` (역할: ADMIN)
- `member` / `password123` (역할: USER)

**중요**: 
- Auth Service는 **DataInitializer.java**를 통해 초기 사용자를 생성합니다.
- 비밀번호는 **BCrypt**로 암호화되어 저장됩니다.
- Member Service의 `data.sql` 파일과는 별개입니다 (Member Service는 회원 정보용, Auth Service는 인증용).

---

## 3. 인증 흐름 상세 분석

### 3.1 로그인 프로세스

```
1. 클라이언트 요청
   POST /auth/login
   {
     "username": "admin",
     "password": "password123"
   }
   ↓

2. AuthController.login() 호출
   ↓

3. AuthenticationManager.authenticate() 호출
   ↓

4. AuthUserDetailsService.loadUserByUsername() 호출
   - 데이터베이스에서 사용자 조회
   - UserDetails 객체 생성 (사용자명, 암호화된 비밀번호, 권한 목록)
   ↓

5. Spring Security가 비밀번호 검증
   - 입력된 비밀번호와 암호화된 비밀번호 비교 (BCrypt)
   ↓

6. 인증 성공 시 Authentication 객체 생성
   ↓

7. JwtTokenProvider.generateToken() 호출
   - 사용자명, 역할 정보를 포함한 JWT 토큰 생성
   ↓

8. AuthResponse 생성 및 반환
   {
     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "tokenType": "Bearer",
     "expiresIn": 1800,
     "username": "admin",
     "roles": ["ROLE_ADMIN"]
   }
```

### 3.2 인증 실패 시나리오

#### 시나리오 1: 잘못된 사용자명
```
1. AuthUserDetailsService.loadUserByUsername() 호출
   ↓
2. 데이터베이스에서 사용자 조회 실패
   ↓
3. UsernameNotFoundException 발생
   ↓
4. 401 Unauthorized 응답
```

#### 시나리오 2: 잘못된 비밀번호
```
1. AuthUserDetailsService.loadUserByUsername() 성공
   ↓
2. Spring Security가 비밀번호 검증 실패
   ↓
3. BadCredentialsException 발생
   ↓
4. GlobalExceptionHandler가 처리
   ↓
5. 401 Unauthorized 응답
   {
     "error": "invalid_credentials",
     "message": "Username or password is incorrect",
     "timestamp": "2024-01-01T12:00:00"
   }
```

---

## 4. JWT 토큰 구조

### 4.1 JWT 토큰 구성

JWT 토큰은 세 부분으로 구성됩니다:

```
Header.Payload.Signature
```

### 4.2 Header (헤더)

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

- `alg`: 서명 알고리즘 (HMAC SHA-256)
- `typ`: 토큰 타입 (JWT)

### 4.3 Payload (페이로드)

```json
{
  "sub": "admin",
  "roles": ["ROLE_ADMIN"],
  "iat": 1704067200,
  "exp": 1704069000
}
```

- `sub`: 사용자명 (subject)
- `roles`: 사용자 역할 배열
- `iat`: 토큰 발급 시간 (issued at)
- `exp`: 토큰 만료 시간 (expiration)

### 4.4 Signature (서명)

```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secretKey
)
```

서명은 SecretKey를 사용하여 생성되며, 토큰의 무결성을 보장합니다.

### 4.5 JWT 토큰 예시

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwiaWF0IjoxNzA0MDY3MjAwLCJleHAiOjE3MDQwNjkwMDB9.signature
```

**디코딩 방법**:
1. https://jwt.io 접속
2. 토큰을 입력하면 자동으로 디코딩됨
3. Header, Payload, Signature 확인 가능

---

## 5. 보안 설정

### 5.1 설정 파일 구조

#### bootstrap.yml (Config Server 연결)
```yaml
spring:
  application:
    name: auth-service
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true
```

#### auth-service.yml (Config Server에서 로드)
```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:h2:mem:authdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop

security:
  jwt:
    secret: change-me-please-change-me-please-32
    access-token-validity-seconds: 1800
```

### 5.2 JWT 설정 속성: JwtProperties

```java
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    private String secret;  // JWT 서명용 비밀키
    private long accessTokenValiditySeconds;  // 토큰 유효 기간 (초)
}
```

**중요 사항**:
- `secret`: 최소 32자 이상의 문자열 권장
- 프로덕션 환경에서는 환경 변수나 시크릿 관리 시스템 사용

### 5.3 보안 정책

| 항목 | 설정 | 설명 |
|------|------|------|
| CSRF | 비활성화 | REST API이므로 불필요 |
| 세션 | Stateless | JWT 기반 인증이므로 세션 사용 안 함 |
| 비밀번호 암호화 | BCrypt | 단방향 해시 알고리즘 |
| 토큰 유효 기간 | 1800초 (30분) | 설정 파일에서 변경 가능 |

---

## 6. 실습 가이드

### 6.1 Auth Service 실행

```bash
./gradlew auth-service:bootRun
```

### 6.2 기본 사용자 확인

애플리케이션 시작 시 다음 사용자가 자동 생성됩니다:

| 사용자명 | 비밀번호 | 역할 |
|---------|---------|------|
| admin | password123 | ADMIN |
| member | password123 | USER |

### 6.3 로그인 테스트

#### 6.3.1 cURL 사용

```bash
curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'
```

#### 6.3.2 예상 응답

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "username": "admin",
  "roles": ["ROLE_ADMIN"]
}
```

### 6.4 JWT 토큰 검증

1. https://jwt.io 접속
2. 발급받은 `accessToken` 값을 **ENCODED** 섹션에 입력
3. **VERIFY SIGNATURE** 섹션의 **SECRET** 필드에 다음 값을 입력:
   ```
   change-me-please-change-me-please-32
   ```
4. **Encoding Format**은 **UTF-8**로 설정되어 있는지 확인
5. Payload 섹션에서 다음 정보 확인:
   - `sub`: 사용자명
   - `roles`: 역할 배열
   - `iat`: 발급 시간
   - `exp`: 만료 시간
6. Signature가 검증되면 "Signature Verified" 메시지가 표시됩니다.

**중요**: SECRET 필드에 정확한 값을 입력하지 않으면 "Invalid Signature" 오류가 발생합니다.

### 6.5 헬스 체크

```bash
curl http://localhost:8083/auth/health
```

**예상 응답**: `OK`

### 6.6 인증 실패 테스트

#### 잘못된 비밀번호
```bash
curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "wrongpassword"
  }'
```

**예상 응답**:
```json
{
  "error": "invalid_credentials",
  "message": "Username or password is incorrect",
  "timestamp": "2024-01-01T12:00:00"
}
```

#### 존재하지 않는 사용자
```bash
curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "nonexistent",
    "password": "password123"
  }'
```

**예상 응답**: 401 Unauthorized

### 6.7 데이터베이스 확인 (H2 Console)

1. http://localhost:8083/h2-console 접속
2. JDBC URL: `jdbc:h2:mem:authdb`
3. 사용자명: `sa`
4. 비밀번호: (비워두기)
5. `AUTH_USERS` 테이블 확인

---

## 7. 핵심 개념 정리

### 7.1 JWT (JSON Web Token)

| 개념 | 설명 |
|------|------|
| **Header** | 토큰 타입 및 서명 알고리즘 |
| **Payload** | 사용자 정보 및 클레임 |
| **Signature** | 서명 (토큰 무결성 보장) |
| **SecretKey** | 서명 생성/검증에 사용되는 비밀키 |

### 7.2 Spring Security

| 개념 | 설명 |
|------|------|
| **AuthenticationManager** | 사용자 인증을 담당하는 매니저 |
| **UserDetailsService** | 사용자 정보를 조회하는 서비스 |
| **PasswordEncoder** | 비밀번호 암호화/검증 |
| **SecurityFilterChain** | 보안 필터 체인 설정 |

### 7.3 역할 관리

| 저장 형식 | 사용 위치 |
|----------|----------|
| 데이터베이스 | `"ADMIN,USER"` (접두사 없음) |
| Spring Security | `[ROLE_ADMIN, ROLE_USER]` (접두사 자동 추가) |
| JWT 토큰 | `["ROLE_ADMIN", "ROLE_USER"]` (접두사 포함) |

---

## 8. 다음 단계

Auth Service를 이해했다면, 다음 단계로 진행하세요:

1. **Member Service**: JWT 토큰을 검증하여 인증된 사용자만 접근 가능하도록 설정
2. **Order Service**: JWT 토큰을 검증하고, 다른 서비스 호출 시 토큰 전파
3. **Gateway Service**: JWT 토큰 검증 필터 구현

---

## 9. 실습 체크리스트

- [ ] Auth Service 실행
- [ ] 기본 사용자 계정 확인 (admin, member)
- [ ] 로그인 API 호출하여 JWT 토큰 발급
- [ ] JWT 토큰을 jwt.io에서 디코딩하여 내용 확인
- [ ] 잘못된 비밀번호로 로그인 시도하여 에러 응답 확인
- [ ] H2 Console에서 사용자 데이터 확인
- [ ] 헬스 체크 API 호출
