package com.example.auth.service;

import com.example.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider 테스트
 * 
 * 테스트 시나리오:
 * 1. Access Token 생성
 * 2. Refresh Token 생성
 * 3. Access Token 검증
 * 4. Refresh Token에는 역할 정보가 없음 확인
 * 5. Token 만료 시간 확인
 * 6. Claims 파싱
 */
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String testUsername = "testuser";
    private String testSecret = "test-secret-key-for-jwt-token-generation-minimum-256-bits-required-for-hs256-algorithm";

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(testSecret);
        properties.setAccessTokenValiditySeconds(900L); // 15분
        properties.setRefreshTokenValiditySeconds(604800L); // 7일

        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("Access Token 생성 - 사용자명과 역할 정보 포함")
    void generateAccessToken_ShouldIncludeUsernameAndRoles() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUsername,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // When
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);

        // Then
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();

        // Claims 확인
        Claims claims = jwtTokenProvider.parseClaims(accessToken);
        assertThat(claims.getSubject()).isEqualTo(testUsername);
        assertThat(claims.get("roles")).isNotNull();
        assertThat(claims.get("type")).isEqualTo("access");
    }

    @Test
    @DisplayName("Refresh Token 생성 - 사용자명만 포함 (역할 정보 제외)")
    void generateRefreshToken_ShouldIncludeOnlyUsername() {
        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUsername);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();

        // Claims 확인 - 역할 정보가 없어야 함
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        assertThat(claims.getSubject()).isEqualTo(testUsername);
        assertThat(claims.get("roles")).isNull(); // Refresh Token에는 역할 정보 없음
        assertThat(claims.get("type")).isEqualTo("refresh");
    }

    @Test
    @DisplayName("Access Token과 Refresh Token의 만료 시간 차이 확인")
    void tokenExpiryTimes_ShouldBeDifferent() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUsername,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // When
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUsername);

        // Then
        Claims accessClaims = jwtTokenProvider.parseClaims(accessToken);
        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);

        // Refresh Token의 만료 시간이 Access Token보다 훨씬 길어야 함
        long accessExpiry = accessClaims.getExpiration().getTime();
        long refreshExpiry = refreshClaims.getExpiration().getTime();

        assertThat(refreshExpiry).isGreaterThan(accessExpiry);
    }

    @Test
    @DisplayName("Token 유효성 검증 - 유효한 토큰")
    void parseClaims_WithValidToken_ShouldNotThrowException() {
        // Given
        String token = jwtTokenProvider.generateRefreshToken(testUsername);

        // When & Then - 예외가 발생하지 않아야 함
        Claims claims = jwtTokenProvider.parseClaims(token);
        
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(testUsername);
    }

    @Test
    @DisplayName("Token 유효성 검증 - 잘못된 토큰")
    void parseClaims_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            jwtTokenProvider.parseClaims(invalidToken);
        });
    }

    @Test
    @DisplayName("Token에서 사용자명 추출")
    void parseClaims_ShouldExtractUsername() {
        // Given
        String token = jwtTokenProvider.generateRefreshToken(testUsername);

        // When
        Claims claims = jwtTokenProvider.parseClaims(token);
        String extractedUsername = claims.getSubject();

        // Then
        assertThat(extractedUsername).isEqualTo(testUsername);
    }

    @Test
    @DisplayName("Claims 파싱")
    void parseClaims_ShouldReturnValidClaims() {
        // Given
        String token = jwtTokenProvider.generateRefreshToken(testUsername);

        // When
        Claims claims = jwtTokenProvider.parseClaims(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(testUsername);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("Access Token 유효 기간 조회")
    void getAccessTokenValiditySeconds_ShouldReturnCorrectValue() {
        // When
        long validity = jwtTokenProvider.getAccessTokenValiditySeconds();

        // Then
        assertThat(validity).isEqualTo(900L); // 15분
    }

    @Test
    @DisplayName("Refresh Token 유효 기간 조회")
    void getRefreshTokenValiditySeconds_ShouldReturnCorrectValue() {
        // When
        long validity = jwtTokenProvider.getRefreshTokenValiditySeconds();

        // Then
        assertThat(validity).isEqualTo(604800L); // 7일
    }

    @Test
    @DisplayName("여러 역할을 가진 사용자의 Access Token 생성")
    void generateAccessToken_WithMultipleRoles_ShouldIncludeAllRoles() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUsername,
                "password",
                java.util.Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        );

        // When
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);

        // Then
        assertThat(accessToken).isNotNull();
        
        Claims claims = jwtTokenProvider.parseClaims(accessToken);
        assertThat(claims.get("roles")).isNotNull();
    }
}

