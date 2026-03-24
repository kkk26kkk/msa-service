package com.example.auth.controller;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.entity.RefreshToken;
import com.example.auth.entity.AuthUser;
import com.example.auth.repository.RefreshTokenRepository;
import com.example.auth.repository.AuthUserRepository;
import com.example.auth.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트
 * 
 * 테스트 시나리오:
 * 1. 로그인 - 정상 케이스
 * 2. 로그인 - 잘못된 비밀번호
 * 3. 로그인 - 존재하지 않는 사용자
 * 4. 토큰 갱신 - 유효한 Refresh Token
 * 5. 토큰 갱신 - 만료된 Refresh Token
 * 6. 토큰 갱신 - 존재하지 않는 Refresh Token
 * 7. 토큰 갱신 - revoked된 Refresh Token
 * 8. 로그아웃 - 정상 케이스
 * 9. 로그아웃 - 존재하지 않는 토큰
 * 10. 헬스 체크
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 통합 테스트")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.cloud.config.enabled=false"
})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthUserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private AuthUser testUser;
    private String testPassword = "password123";

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        
        // 테스트 사용자 생성
        testUser = AuthUser.builder()
                .username("testuser")
                .password(passwordEncoder.encode(testPassword))
                .roles("ROLE_USER")
                .build();
        userRepository.save(testUser);
    }

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
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.roles").isArray())
                .andReturn();

        // Refresh Token이 데이터베이스에 저장되었는지 확인
        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );
        
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getRefreshToken()).isNotEmpty();
        
        RefreshToken savedToken = refreshTokenRepository.findByUsername(testUser.getUsername()).orElse(null);
        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getToken()).isEqualTo(response.getRefreshToken());
        assertThat(savedToken.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("로그인 - 잘못된 비밀번호: 401 Unauthorized")
    void login_WithInvalidPassword_ShouldReturnUnauthorized() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUser.getUsername());
        loginRequest.setPassword("wrongpassword");

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인 - 존재하지 않는 사용자: 401 Unauthorized")
    void login_WithNonExistentUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistent");
        loginRequest.setPassword(testPassword);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 갱신 - 유효한 Refresh Token: 새로운 토큰 발급")
    void refresh_WithValidRefreshToken_ShouldReturnNewTokens() throws Exception {
        // Given: 로그인하여 Refresh Token 발급
        String refreshToken = performLoginAndGetRefreshToken();

        // 새로운 JWT 토큰이 생성되도록 1초 대기 (iat 클레임이 초 단위)
        Thread.sleep(1000);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        // When & Then
        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andReturn();

        // 새로운 Refresh Token이 발급되었는지 확인
        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );
        
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getRefreshToken()).isNotEmpty();
        assertThat(response.getRefreshToken()).isNotEqualTo(refreshToken); // 새로운 토큰

        // 기존 토큰은 삭제되고 새 토큰이 저장되었는지 확인
        RefreshToken savedToken = refreshTokenRepository.findByUsername(testUser.getUsername()).orElse(null);
        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getToken()).isEqualTo(response.getRefreshToken());
    }

    @Test
    @DisplayName("토큰 갱신 - 만료된 Refresh Token: 401 Unauthorized")
    void refresh_WithExpiredRefreshToken_ShouldReturnUnauthorized() throws Exception {
        // Given: 만료된 Refresh Token 생성
        String expiredToken = jwtTokenProvider.generateRefreshToken(testUser.getUsername());
        RefreshToken expiredRefreshToken = RefreshToken.builder()
                .token(expiredToken)
                .username(testUser.getUsername())
                .expiresAt(LocalDateTime.now().minusDays(1)) // 만료된 시간
                .revoked(false)
                .build();
        refreshTokenRepository.save(expiredRefreshToken);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(expiredToken);

        // When & Then
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or expired refresh token"));
    }

    @Test
    @DisplayName("토큰 갱신 - 존재하지 않는 Refresh Token: 401 Unauthorized")
    void refresh_WithNonExistentRefreshToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("non-existent-token");

        // When & Then
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or expired refresh token"));
    }

    @Test
    @DisplayName("토큰 갱신 - revoked된 Refresh Token: 401 Unauthorized")
    void refresh_WithRevokedRefreshToken_ShouldReturnUnauthorized() throws Exception {
        // Given: revoked된 Refresh Token 생성
        String revokedToken = jwtTokenProvider.generateRefreshToken(testUser.getUsername());
        RefreshToken revokedRefreshToken = RefreshToken.builder()
                .token(revokedToken)
                .username(testUser.getUsername())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true) // revoked 상태
                .build();
        refreshTokenRepository.save(revokedRefreshToken);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(revokedToken);

        // When & Then
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or expired refresh token"));
    }

    @Test
    @DisplayName("로그아웃 - 정상 케이스: Refresh Token 삭제")
    void logout_WithValidRefreshToken_ShouldDeleteToken() throws Exception {
        // Given: 로그인하여 Refresh Token 발급
        String refreshToken = performLoginAndGetRefreshToken();

        RefreshTokenRequest logoutRequest = new RefreshTokenRequest();
        logoutRequest.setRefreshToken(refreshToken);

        // When & Then
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Logout successful"));

        // Refresh Token이 삭제되었는지 확인
        RefreshToken deletedToken = refreshTokenRepository.findByToken(refreshToken).orElse(null);
        assertThat(deletedToken).isNull();
    }

    @Test
    @DisplayName("로그아웃 - 존재하지 않는 토큰: 정상 처리")
    void logout_WithNonExistentToken_ShouldReturnOk() throws Exception {
        // Given
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest();
        logoutRequest.setRefreshToken("non-existent-token");

        // When & Then
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Logout successful"));
    }

    @Test
    @DisplayName("헬스 체크 - 정상 응답")
    void health_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    @DisplayName("로그인 - 빈 username: 400 Bad Request")
    void login_WithEmptyUsername_ShouldReturnBadRequest() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("");
        loginRequest.setPassword(testPassword);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 갱신 - 빈 refreshToken: 400 Bad Request")
    void refresh_WithEmptyRefreshToken_ShouldReturnBadRequest() throws Exception {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("");

        // When & Then
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ===== 헬퍼 메서드 =====

    /**
     * 로그인을 수행하고 Refresh Token을 반환하는 헬퍼 메서드
     */
    private String performLoginAndGetRefreshToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUser.getUsername());
        loginRequest.setPassword(testPassword);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );

        return response.getRefreshToken();
    }
}

