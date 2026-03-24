package com.example.auth.repository;

import com.example.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    /**
     * 토큰으로 RefreshToken 조회
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자명으로 RefreshToken 조회
     */
    Optional<RefreshToken> findByUsername(String username);

    /**
     * 사용자명으로 활성화된 RefreshToken 조회 (revoked = false)
     */
    Optional<RefreshToken> findByUsernameAndRevokedFalse(String username);

    /**
     * 토큰으로 활성화된 RefreshToken 조회 (revoked = false)
     */
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    /**
     * 만료된 토큰 삭제
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 사용자의 모든 RefreshToken 삭제
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.username = :username")
    void deleteByUsername(@Param("username") String username);

    /**
     * 사용자의 모든 RefreshToken을 revoked 상태로 변경
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.username = :username")
    void revokeByUsername(@Param("username") String username);
}

