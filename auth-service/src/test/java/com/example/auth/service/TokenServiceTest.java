package com.example.auth.service;

import com.example.auth.entity.RefreshToken;
import com.example.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TokenService 단위 테스트
 * 
 * 테스트 시나리오:
 * 1. Refresh Token 저장 - 정상 케이스
 * 2. Refresh Token 저장 - 기존 토큰이 있는 경우
 * 3. Refresh Token 검증 - 유효한 토큰
 * 4. Refresh Token 검증 - 토큰이 없는 경우
 * 5. Refresh Token 검증 - 만료된 토큰
 * 6. Refresh Token 검증 - revoked된 토큰
 * 7. Refresh Token 삭제
 * 8. 사용자명으로 모든 토큰 삭제
 * 9. 사용자명으로 모든 토큰 revoke
 * 10. 만료된 토큰 정리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 테스트")
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private TokenService tokenService;

    private String testToken;
    private String testUsername;
    private LocalDateTime futureTime;
    private LocalDateTime pastTime;

    @BeforeEach
    void setUp() {
        testToken = "test-refresh-token";
        testUsername = "testuser";
        futureTime = LocalDateTime.now().plusDays(7);
        pastTime = LocalDateTime.now().minusDays(1);
    }

    @Test
    @DisplayName("Refresh Token 저장 - 기존 토큰이 없는 경우")
    void saveRefreshToken_WhenNoExistingToken_ShouldSaveSuccessfully() {
        // GivenJwtTokenProviderTest.java
        when(refreshTokenRepository.findByUsername(testUsername)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // When
        RefreshToken result = tokenService.saveRefreshToken(testToken, testUsername, futureTime);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(testToken);
        assertThat(result.getUsername()).isEqualTo(testUsername);
        assertThat(result.getExpiresAt()).isEqualTo(futureTime);
        assertThat(result.isRevoked()).isFalse();
        
        verify(refreshTokenRepository, times(1)).findByUsername(testUsername);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

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
        
        when(refreshTokenRepository.findByUsername(testUsername)).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(2L);
            return token;
        });

        // When
        RefreshToken result = tokenService.saveRefreshToken(testToken, testUsername, futureTime);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(testToken);
        assertThat(result.getUsername()).isEqualTo(testUsername);
        
        verify(refreshTokenRepository, times(1)).findByUsername(testUsername);
        verify(refreshTokenRepository, times(1)).delete(existingToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh Token 검증 - 유효한 토큰")
    void validateRefreshToken_WhenValidToken_ShouldReturnToken() {
        // Given
        RefreshToken validToken = RefreshToken.builder()
                .id(1L)
                .token(testToken)
                .username(testUsername)
                .expiresAt(futureTime)
                .revoked(false)
                .build();
        
        when(refreshTokenRepository.findByTokenAndRevokedFalse(testToken))
                .thenReturn(Optional.of(validToken));

        // When
        Optional<RefreshToken> result = tokenService.validateRefreshToken(testToken);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo(testToken);
        assertThat(result.get().getUsername()).isEqualTo(testUsername);
        assertThat(result.get().isRevoked()).isFalse();
        
        verify(refreshTokenRepository, times(1)).findByTokenAndRevokedFalse(testToken);
    }

    @Test
    @DisplayName("Refresh Token 검증 - 토큰이 존재하지 않는 경우")
    void validateRefreshToken_WhenTokenNotFound_ShouldReturnEmpty() {
        // Given
        when(refreshTokenRepository.findByTokenAndRevokedFalse(testToken))
                .thenReturn(Optional.empty());

        // When
        Optional<RefreshToken> result = tokenService.validateRefreshToken(testToken);

        // Then
        assertThat(result).isEmpty();
        verify(refreshTokenRepository, times(1)).findByTokenAndRevokedFalse(testToken);
    }

    @Test
    @DisplayName("Refresh Token 검증 - 만료된 토큰")
    void validateRefreshToken_WhenExpiredToken_ShouldReturnEmpty() {
        // Given
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token(testToken)
                .username(testUsername)
                .expiresAt(pastTime)  // 만료된 시간
                .revoked(false)
                .build();
        
        when(refreshTokenRepository.findByTokenAndRevokedFalse(testToken))
                .thenReturn(Optional.of(expiredToken));

        // When
        Optional<RefreshToken> result = tokenService.validateRefreshToken(testToken);

        // Then
        assertThat(result).isEmpty();
        verify(refreshTokenRepository, times(1)).findByTokenAndRevokedFalse(testToken);
    }

    @Test
    @DisplayName("Refresh Token 검증 - revoked된 토큰은 조회되지 않음")
    void validateRefreshToken_WhenRevokedToken_ShouldReturnEmpty() {
        // Given
        // findByTokenAndRevokedFalse는 revoked=true인 토큰을 반환하지 않음
        when(refreshTokenRepository.findByTokenAndRevokedFalse(testToken))
                .thenReturn(Optional.empty());

        // When
        Optional<RefreshToken> result = tokenService.validateRefreshToken(testToken);

        // Then
        assertThat(result).isEmpty();
        verify(refreshTokenRepository, times(1)).findByTokenAndRevokedFalse(testToken);
    }

    @Test
    @DisplayName("Refresh Token 삭제 - 토큰이 존재하는 경우")
    void deleteRefreshToken_WhenTokenExists_ShouldDelete() {
        // Given
        RefreshToken existingToken = RefreshToken.builder()
                .id(1L)
                .token(testToken)
                .username(testUsername)
                .expiresAt(futureTime)
                .build();
        
        when(refreshTokenRepository.findByToken(testToken)).thenReturn(Optional.of(existingToken));

        // When
        tokenService.deleteRefreshToken(testToken);

        // Then
        verify(refreshTokenRepository, times(1)).findByToken(testToken);
        verify(refreshTokenRepository, times(1)).delete(existingToken);
    }

    @Test
    @DisplayName("Refresh Token 삭제 - 토큰이 존재하지 않는 경우")
    void deleteRefreshToken_WhenTokenNotExists_ShouldDoNothing() {
        // Given
        when(refreshTokenRepository.findByToken(testToken)).thenReturn(Optional.empty());

        // When
        tokenService.deleteRefreshToken(testToken);

        // Then
        verify(refreshTokenRepository, times(1)).findByToken(testToken);
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("사용자명으로 모든 Refresh Token 삭제")
    void deleteRefreshTokensByUsername_ShouldCallRepository() {
        // Given
        // Repository 메서드는 void이므로 doNothing 사용
        doNothing().when(refreshTokenRepository).deleteByUsername(testUsername);

        // When
        tokenService.deleteRefreshTokensByUsername(testUsername);

        // Then
        verify(refreshTokenRepository, times(1)).deleteByUsername(testUsername);
    }

    @Test
    @DisplayName("사용자명으로 모든 Refresh Token revoke")
    void revokeRefreshTokensByUsername_ShouldCallRepository() {
        // Given
        doNothing().when(refreshTokenRepository).revokeByUsername(testUsername);

        // When
        tokenService.revokeRefreshTokensByUsername(testUsername);

        // Then
        verify(refreshTokenRepository, times(1)).revokeByUsername(testUsername);
    }

    @Test
    @DisplayName("만료된 토큰 정리")
    void cleanupExpiredTokens_ShouldCallRepository() {
        // Given
        doNothing().when(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));

        // When
        tokenService.cleanupExpiredTokens();

        // Then
        verify(refreshTokenRepository, times(1)).deleteExpiredTokens(any(LocalDateTime.class));
    }
}


