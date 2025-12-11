# 👥 3단계: Member Service 상세 가이드

이 문서는 MSA 프로젝트의 **Member Service**에 대한 상세한 설명입니다. Spring Data JPA를 활용한 CRUD 서비스이며, JWT 토큰 기반 인증을 통합한 서비스입니다.

---

## 📍 목차

1. [Member Service 개요](#1-member-service-개요)
2. [코드 구조 분석](#2-코드-구조-분석)
3. [JWT 인증 통합](#3-jwt-인증-통합)
4. [API 엔드포인트](#4-api-엔드포인트)
5. [데이터베이스 구조](#5-데이터베이스-구조)
6. [예외 처리](#6-예외-처리)
7. [실습 가이드](#7-실습-가이드)

---

## 1. Member Service 개요

### 1.1 역할

**Member Service**는 회원 관리를 담당하는 마이크로서비스입니다.

**주요 기능**:
- 회원 등록, 조회, 수정, 삭제 (CRUD)
- 페이징 및 정렬 지원
- 상태별 회원 조회 (ACTIVE, INACTIVE, SUSPENDED)
- 이름으로 회원 검색
- JWT 토큰 기반 인증 및 역할 기반 권한 관리

### 1.2 기술 스택

- **Spring Boot**: 웹 애플리케이션 프레임워크
- **Spring Data JPA**: 데이터베이스 접근
- **Spring Security**: JWT 토큰 기반 인증
- **H2 Database**: 인메모리 데이터베이스 (개발용)
- **Bean Validation**: 데이터 검증
- **Lombok**: 보일러플레이트 코드 감소

### 1.3 서비스 포트

- **포트**: 8081
- **접속 URL**: http://localhost:8081
- **H2 Console**: http://localhost:8081/h2-console

---

## 2. 코드 구조 분석

### 2.1 프로젝트 구조

```
member-service/
├── src/main/java/com/example/member/
│   ├── MemberServiceApplication.java      # 애플리케이션 진입점
│   ├── config/
│   │   └── SecurityConfig.java          # Spring Security 설정
│   ├── controller/
│   │   └── MemberController.java        # REST API 엔드포인트
│   ├── dto/
│   │   └── MemberDto.java               # 데이터 전송 객체
│   ├── entity/
│   │   └── Member.java                  # 회원 엔티티
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # 전역 예외 처리
│   │   ├── MemberNotFoundException.java
│   │   └── DuplicateMemberException.java
│   ├── repository/
│   │   └── MemberRepository.java       # JPA 리포지토리
│   ├── security/
│   │   └── SecurityRoles.java          # 역할 상수 정의
│   └── service/
│       └── MemberService.java          # 비즈니스 로직
```

### 2.2 엔티티: Member

```java
@Entity
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 50, message = "사용자명은 3-50자 사이여야 합니다")
    private String username;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다")
    private String password;

    @Column(unique = true, nullable = false, length = 100)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Column(length = 100)
    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
    private String fullName;

    @Column(length = 20)
    @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum MemberStatus {
        ACTIVE("활성"),
        INACTIVE("비활성"),
        SUSPENDED("정지");
    }
}
```

**핵심 필드**:
- `id`: 회원 ID (자동 생성)
- `username`: 사용자명 (고유값)
- `password`: 비밀번호
- `email`: 이메일 (고유값)
- `fullName`: 이름
- `phoneNumber`: 전화번호
- `status`: 회원 상태 (ACTIVE, INACTIVE, SUSPENDED)
- `createdAt`: 생성 시간 (자동 생성)
- `updatedAt`: 수정 시간 (자동 업데이트)

**Bean Validation 어노테이션**:
- `@NotBlank`: 필수 필드 검증
- `@Size`: 길이 제한 검증
- `@Email`: 이메일 형식 검증

### 2.3 리포지토리: MemberRepository

```java
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 사용자명으로 회원 조회
    Optional<Member> findByUsername(String username);

    // 이메일로 회원 조회
    Optional<Member> findByEmail(String email);

    // 사용자명 존재 여부 확인
    boolean existsByUsername(String username);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    // 상태별 회원 조회
    List<Member> findByStatus(Member.MemberStatus status);

    // 상태별 회원 페이징 조회
    Page<Member> findByStatus(Member.MemberStatus status, Pageable pageable);

    // 이름으로 회원 검색 (부분 일치)
    @Query("SELECT m FROM Member m WHERE m.fullName LIKE %:name%")
    List<Member> findByFullNameContaining(@Param("name") String name);

    // 사용자명 또는 이메일로 회원 검색
    @Query("SELECT m FROM Member m WHERE m.username = :keyword OR m.email = :keyword")
    Optional<Member> findByUsernameOrEmail(@Param("keyword") String keyword);

    // 활성 회원 수 조회
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE'")
    long countActiveMembers();
}
```

**Spring Data JPA 메서드 네이밍 규칙**:
- `findBy...`: 조회 메서드
- `existsBy...`: 존재 여부 확인 메서드
- `countBy...`: 개수 조회 메서드

**커스텀 쿼리**:
- `@Query`: JPQL 쿼리 작성
- `@Param`: 파라미터 바인딩

### 2.4 서비스: MemberService

```java
@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;

    // 회원 생성
    @Transactional
    public MemberDto.Response createMember(MemberDto.CreateRequest request) {
        // 중복 검사
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateMemberException("이미 존재하는 사용자명입니다");
        }
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateMemberException("이미 존재하는 이메일입니다");
        }

        // 엔터티 생성 및 저장
        Member member = request.toEntity();
        Member savedMember = memberRepository.save(member);
        return MemberDto.Response.from(savedMember);
    }

    // ID로 회원 조회
    public MemberDto.Response getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다. ID: " + id));
        return MemberDto.Response.from(member);
    }

    // 회원 페이징 조회
    public Page<MemberDto.Summary> getMembers(Pageable pageable) {
        Page<Member> memberPage = memberRepository.findAll(pageable);
        return memberPage.map(MemberDto.Summary::from);
    }

    // 회원 정보 수정
    @Transactional
    public MemberDto.Response updateMember(Long id, MemberDto.UpdateRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다. ID: " + id));

        // 수정 가능한 필드 업데이트
        if (request.getFullName() != null) {
            member.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            member.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getStatus() != null) {
            member.setStatus(request.getStatus());
        }

        Member updatedMember = memberRepository.save(member);
        return MemberDto.Response.from(updatedMember);
    }

    // 회원 삭제
    @Transactional
    public void deleteMember(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new MemberNotFoundException("회원을 찾을 수 없습니다. ID: " + id);
        }
        memberRepository.deleteById(id);
    }
}
```

**트랜잭션 관리**:
- `@Transactional(readOnly = true)`: 클래스 레벨에서 읽기 전용 트랜잭션 설정
- `@Transactional`: 쓰기 작업에만 명시적으로 트랜잭션 설정

### 2.5 DTO: MemberDto

```java
public class MemberDto {
    // 회원 생성 요청 DTO
    public static class CreateRequest {
        @NotBlank(message = "사용자명은 필수입니다")
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 6)
        private String password;

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        private String fullName;
        private String phoneNumber;

        // DTO를 Entity로 변환
        public Member toEntity() {
            return Member.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .fullName(fullName)
                    .phoneNumber(phoneNumber)
                    .status(Member.MemberStatus.ACTIVE)
                    .build();
        }
    }

    // 회원 응답 DTO
    public static class Response {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String phoneNumber;
        private Member.MemberStatus status;
        private String statusDescription;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // Entity를 DTO로 변환
        public static Response from(Member member) {
            return Response.builder()
                    .id(member.getId())
                    .username(member.getUsername())
                    .email(member.getEmail())
                    .fullName(member.getFullName())
                    .phoneNumber(member.getPhoneNumber())
                    .status(member.getStatus())
                    .statusDescription(member.getStatus().getDescription())
                    .createdAt(member.getCreatedAt())
                    .updatedAt(member.getUpdatedAt())
                    .build();
        }
    }

    // 회원 요약 DTO (목록 조회용)
    public static class Summary {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private Member.MemberStatus status;
        private LocalDateTime createdAt;

        public static Summary from(Member member) {
            return Summary.builder()
                    .id(member.getId())
                    .username(member.getUsername())
                    .email(member.getEmail())
                    .fullName(member.getFullName())
                    .status(member.getStatus())
                    .createdAt(member.getCreatedAt())
                    .build();
        }
    }
}
```

**DTO 패턴**:
- `CreateRequest`: 회원 생성 요청
- `UpdateRequest`: 회원 수정 요청
- `Response`: 회원 상세 응답
- `Summary`: 회원 목록 응답 (간소화된 정보)

---

## 3. JWT 인증 통합

### 3.1 SecurityConfig 분석

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final String secret;

    public SecurityConfig(@Value("${security.jwt.secret}") String secret) {
        this.secret = secret;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 경로
                .requestMatchers("/actuator/**", "/h2-console/**", "/members/health")
                    .permitAll()
                // 나머지 모든 요청은 JWT 토큰 인증 필요
                .anyRequest().authenticated())
            // Stateless 세션 정책
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // OAuth2 Resource Server 설정
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        );
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RolesClaimConverter());
        return converter;
    }

    // JWT의 "roles" 클레임을 GrantedAuthority로 변환
    private static class RolesClaimConverter 
            implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) jwt.getClaim("roles");
            
            if (roles == null || roles.isEmpty()) {
                return List.of();
            }
            
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role))
                    .collect(Collectors.toList());
        }
    }
}
```

**핵심 설정**:
- `JwtDecoder`: JWT 토큰 검증 (auth-service와 동일한 secret 사용)
- `JwtAuthenticationConverter`: JWT의 "roles" 클레임을 Spring Security 권한으로 변환
- `permitAll()`: 헬스체크, H2 콘솔 등은 인증 불필요

### 3.2 역할 기반 권한 관리

```java
@RestController
@RequestMapping("/members")
public class MemberController {
    // 회원 생성 (ADMIN만 가능)
    @PostMapping
    @PreAuthorize("hasRole(T(com.example.member.security.SecurityRoles).ADMIN)")
    public ResponseEntity<MemberDto.Response> createMember(...) {
        // ...
    }

    // 회원 목록 조회 (ADMIN, USER 모두 가능)
    @GetMapping
    @PreAuthorize("hasAnyRole(T(com.example.member.security.SecurityRoles).ADMIN, T(com.example.member.security.SecurityRoles).USER)")
    public ResponseEntity<Page<MemberDto.Summary>> getMembers(...) {
        // ...
    }
}
```

**권한 설정**:
- `@PreAuthorize("hasRole('ADMIN')")`: ADMIN 역할만 접근 가능
- `@PreAuthorize("hasAnyRole('ADMIN', 'USER')")`: ADMIN 또는 USER 역할 접근 가능

### 3.3 JWT 토큰 검증 흐름

```
1. 클라이언트 요청
   GET /members
   Authorization: Bearer {JWT_TOKEN}
   ↓

2. SecurityFilterChain이 요청 가로채기
   ↓

3. JwtDecoder가 토큰 검증
   - SecretKey로 서명 검증
   - 만료 시간 확인
   ↓

4. JwtAuthenticationConverter가 "roles" 클레임 추출
   - JWT: {"roles": ["ROLE_ADMIN"]}
   - Spring Security: [ROLE_ADMIN]
   ↓

5. @PreAuthorize로 권한 확인
   ↓

6. 권한이 있으면 요청 처리, 없으면 403 Forbidden
```

---

## 4. API 엔드포인트

### 4.1 API 목록

| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | `/members` | 회원 생성 | ADMIN |
| GET | `/members` | 회원 목록 조회 (페이징) | ADMIN, USER |
| GET | `/members/all` | 모든 회원 조회 | ADMIN, USER |
| GET | `/members/{id}` | ID로 회원 조회 | ADMIN, USER |
| GET | `/members/username/{username}` | 사용자명으로 회원 조회 | ADMIN, USER |
| GET | `/members/status/{status}` | 상태별 회원 조회 | ADMIN, USER |
| GET | `/members/search?name=홍길동` | 이름으로 회원 검색 | ADMIN, USER |
| PUT | `/members/{id}` | 회원 정보 수정 | ADMIN |
| DELETE | `/members/{id}` | 회원 삭제 | ADMIN |
| GET | `/members/stats/active-count` | 활성 회원 수 조회 | ADMIN, USER |
| GET | `/members/health` | 헬스 체크 | 인증 불필요 |

### 4.2 API 예시

#### 회원 생성
```http
POST /members
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "email": "newuser@example.com",
  "fullName": "신규사용자",
  "phoneNumber": "010-9999-8888"
}
```

**응답**:
```json
{
  "id": 1,
  "username": "newuser",
  "email": "newuser@example.com",
  "fullName": "신규사용자",
  "phoneNumber": "010-9999-8888",
  "status": "ACTIVE",
  "statusDescription": "활성",
  "createdAt": "2024-01-01 12:00:00",
  "updatedAt": "2024-01-01 12:00:00"
}
```

#### 회원 목록 조회 (페이징)
```http
GET /members?page=0&size=10&sort=id,desc
Authorization: Bearer {JWT_TOKEN}
```

**응답**:
```json
{
  "content": [
    {
      "id": 1,
      "username": "user1",
      "email": "user1@example.com",
      "fullName": "홍길동",
      "status": "ACTIVE",
      "createdAt": "2024-01-01 12:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

## 5. 데이터베이스 구조

### 5.1 테이블 스키마

```sql
CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    phone_number VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5.2 초기 데이터

애플리케이션 시작 시 `data.sql` 파일이 실행되어 다음 데이터가 자동 생성됩니다:

- `admin` / `admin123` (ACTIVE)
- `user1` / `password123` (ACTIVE)
- `user2` / `password123` (ACTIVE)
- `user3` / `password123` (INACTIVE)
- `user4` / `password123` (ACTIVE)

**중요**: 
- Member Service의 `data.sql`은 **회원 정보(Member 엔티티)**를 위한 초기 데이터입니다.
- **Auth Service의 인증 계정과는 별개**입니다.
- Auth Service 로그인은 `DataInitializer.java`에서 생성된 계정을 사용합니다:
  - `admin` / `password123` (역할: ADMIN)
  - `member` / `password123` (역할: USER)

---

## 6. 예외 처리

### 6.1 예외 클래스

```java
// 회원을 찾을 수 없을 때
public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(String message) {
        super(message);
    }
}

// 중복된 회원 정보일 때
public class DuplicateMemberException extends RuntimeException {
    public DuplicateMemberException(String message) {
        super(message);
    }
}
```

### 6.2 전역 예외 처리

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFoundException(
            MemberNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Member Not Found")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(DuplicateMemberException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateMemberException(
            DuplicateMemberException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Duplicate Member")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("이 작업을 수행할 권한이 없습니다")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("입력 데이터가 유효하지 않습니다")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
```

**예외 처리 흐름**:
1. 비즈니스 로직에서 예외 발생
2. `@RestControllerAdvice`가 예외를 가로채기
3. 적절한 HTTP 상태 코드와 에러 메시지 반환

---

## 7. 실습 가이드

### 7.1 Member Service 실행

```bash
./gradlew member-service:bootRun
```

### 7.2 JWT 토큰 발급

먼저 Auth Service에서 JWT 토큰을 발급받습니다:

```bash
curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'
```

**응답에서 `accessToken` 값을 복사합니다.**

### 7.3 회원 목록 조회

```bash
curl -X GET http://localhost:8081/members \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 7.4 회원 생성

```bash
curl -X POST http://localhost:8081/members \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "email": "newuser@example.com",
    "fullName": "신규사용자",
    "phoneNumber": "010-9999-8888"
  }'
```

### 7.5 회원 조회

```bash
curl -X GET http://localhost:8081/members/1 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 7.6 회원 수정

```bash
curl -X PUT http://localhost:8081/members/1 \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "수정된이름",
    "phoneNumber": "010-1111-2222",
    "status": "INACTIVE"
  }'
```

### 7.7 회원 삭제

```bash
curl -X DELETE http://localhost:8081/members/1 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 7.8 페이징 조회

```bash
curl -X GET "http://localhost:8081/members?page=0&size=5&sort=id,desc" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 7.9 상태별 조회

```bash
curl -X GET http://localhost:8081/members/status/ACTIVE \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 7.10 이름으로 검색

```bash
curl -X GET "http://localhost:8081/members/search?name=홍길동" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 7.11 인증 실패 테스트

JWT 토큰 없이 요청:

```bash
curl -X GET http://localhost:8081/members
```

**예상 응답**: 401 Unauthorized

잘못된 JWT 토큰으로 요청:

```bash
curl -X GET http://localhost:8081/members \
  -H "Authorization: Bearer invalid_token"
```

**예상 응답**: 401 Unauthorized

### 7.12 권한 부족 테스트

USER 역할로 회원 생성 시도:

```bash
# member 계정으로 로그인하여 토큰 발급
curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "member",
    "password": "password123"
  }'

# USER 토큰으로 회원 생성 시도
curl -X POST http://localhost:8081/members \
  -H "Authorization: Bearer {USER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  }'
```

**예상 응답**: 403 Forbidden

```json
{
  "status": 403,
  "error": "Access Denied",
  "message": "이 작업을 수행할 권한이 없습니다",
  "timestamp": "2024-01-01T12:00:00"
}
```

### 7.13 H2 Console 확인

1. http://localhost:8081/h2-console 접속
2. JDBC URL: `jdbc:h2:mem:memberdb`
3. 사용자명: `sa`
4. 비밀번호: (비워두기)
5. `MEMBERS` 테이블 확인

---

## 8. 핵심 개념 정리

### 8.1 Spring Data JPA

| 개념 | 설명 |
|------|------|
| **JpaRepository** | 기본 CRUD 메서드 제공 |
| **메서드 네이밍** | `findBy...`, `existsBy...` 등으로 쿼리 자동 생성 |
| **@Query** | 커스텀 JPQL 쿼리 작성 |
| **Pageable** | 페이징 및 정렬 지원 |

### 8.2 JWT 인증 통합

| 개념 | 설명 |
|------|------|
| **JwtDecoder** | JWT 토큰 검증 |
| **JwtAuthenticationConverter** | JWT 클레임을 Spring Security 권한으로 변환 |
| **@PreAuthorize** | 메서드 레벨 권한 검사 |
| **OAuth2 Resource Server** | JWT 토큰 기반 인증 |

### 8.3 Bean Validation

| 어노테이션 | 설명 |
|-----------|------|
| `@NotBlank` | 필수 필드 (null, 빈 문자열, 공백 불가) |
| `@Size` | 길이 제한 |
| `@Email` | 이메일 형식 검증 |

---

## 9. 다음 단계

Member Service를 이해했다면, 다음 단계로 진행하세요:

1. **Order Service**: OpenFeign을 통한 서비스 간 통신 학습
2. **Gateway Service**: API Gateway의 라우팅 및 인증 필터 학습

---

## 10. 실습 체크리스트

- [ ] Member Service 실행
- [ ] Auth Service에서 JWT 토큰 발급
- [ ] JWT 토큰으로 회원 목록 조회
- [ ] 회원 생성 (ADMIN 권한 필요)
- [ ] 회원 조회, 수정, 삭제 테스트
- [ ] 페이징 조회 테스트
- [ ] 상태별 조회 테스트
- [ ] 이름으로 검색 테스트
- [ ] 인증 실패 시나리오 테스트
- [ ] 권한 부족 시나리오 테스트
- [ ] H2 Console에서 데이터 확인
