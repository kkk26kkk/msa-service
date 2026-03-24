package com.example.auth.service;

import com.example.auth.entity.RefreshToken;
import com.example.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token 관리 서비스
 * 
 * 주요 기능:
 * - Refresh Token 저장
 * - Refresh Token 검증
 * - Refresh Token 삭제 (로그아웃)
 * - 만료된 토큰 정리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Refresh Token 저장
     * 
     * 기존 토큰이 있으면 삭제하고 새 토큰을 저장합니다.
     * 
     * @param token Refresh Token 문자열
     * @param username 사용자명
     * @param expiresAt 만료 시간
     * @return 저장된 RefreshToken 엔티티
     */
    @Transactional
    public RefreshToken saveRefreshToken(String token, String username, LocalDateTime expiresAt) {
        // 기존 토큰이 있으면 삭제
        refreshTokenRepository.findByUsername(username)
                .ifPresent(existingToken -> {
                    log.debug("Deleting existing refresh token for user: {}", username);
                    refreshTokenRepository.delete(existingToken);
                });

        // 새 토큰 저장
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .username(username)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.debug("Saved refresh token for user: {}", username);
        return saved;
    }

    /**
     * Refresh Token 검증
     * 
     * @param token 검증할 Refresh Token
     * @return 검증 성공 시 RefreshToken 엔티티, 실패 시 Optional.empty()
     */
    @Transactional(readOnly = true)
    public Optional<RefreshToken> validateRefreshToken(String token) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenAndRevokedFalse(token);
        
        if (tokenOpt.isEmpty()) {
            log.debug("Refresh token not found or revoked: {}", token);
            return Optional.empty();
        }

        RefreshToken refreshToken = tokenOpt.get();
        
        // 만료 시간 확인
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.debug("Refresh token expired: {}", token);
            return Optional.empty();
        }

        return Optional.of(refreshToken);
    }

    /**
     * Refresh Token 삭제 (로그아웃)
     * 
     * @param token 삭제할 Refresh Token
     */
    @Transactional
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshTokenRepository.delete(refreshToken);
                    log.debug("Deleted refresh token for user: {}", refreshToken.getUsername());
                });
    }

    /**
     * 사용자의 모든 Refresh Token 삭제 (로그아웃)
     * 
     * @param username 사용자명
     */
    @Transactional
    public void deleteRefreshTokensByUsername(String username) {
        refreshTokenRepository.deleteByUsername(username);
        log.debug("Deleted all refresh tokens for user: {}", username);
    }

    /**
     * 사용자의 모든 Refresh Token을 revoked 상태로 변경
     * 
     * @param username 사용자명
     */
    @Transactional
    public void revokeRefreshTokensByUsername(String username) {
        refreshTokenRepository.revokeByUsername(username);
        log.debug("Revoked all refresh tokens for user: {}", username);
    }

    /**
     * 만료된 토큰 정리
     */
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteExpiredTokens(now);
        log.debug("Cleaned up expired refresh tokens");
    }
}

