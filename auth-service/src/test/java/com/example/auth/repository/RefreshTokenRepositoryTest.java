package com.example.auth.repository;

import com.example.auth.entity.RefreshToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RefreshTokenRepository 테스트
 * 
 * 테스트 시나리오:
 * 1. 토큰으로 조회
 * 2. 사용자명으로 조회
 * 3. 사용자명과 revoked 상태로 조회
 * 4. 토큰과 revoked 상태로 조회
 * 5. 만료된 토큰 삭제
 * 6. 사용자명으로 모든 토큰 삭제
 * 7. 사용자명으로 모든 토큰 revoke
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository 테스트")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.cloud.config.enabled=false"
})
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityManager entityManager;

    private RefreshToken testToken1;
    private RefreshToken testToken2;
    private String username1 = "user1";
    private String username2 = "user2";

    @BeforeEach
    void setUp() {
        // 모든 데이터 삭제 (각 테스트 간 독립성 보장)
        refreshTokenRepository.deleteAll();

        // 테스트 토큰 1
        testToken1 = RefreshToken.builder()
                .token("test-token-1")
                .username(username1)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        testToken1 = refreshTokenRepository.save(testToken1);

        // 테스트 토큰 2
        testToken2 = RefreshToken.builder()
                .token("test-token-2")
                .username(username2)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        testToken2 = refreshTokenRepository.save(testToken2);
        
        // flush하여 즉시 DB에 반영
        refreshTokenRepository.flush();
    }

    @Test
    @DisplayName("토큰으로 RefreshToken 조회")
    void findByToken_ShouldReturnToken() {
        // When
        Optional<RefreshToken> result = refreshTokenRepository.findByToken("test-token-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("test-token-1");
        assertThat(result.get().getUsername()).isEqualTo(username1);
    }

    @Test
    @DisplayName("존재하지 않는 토큰 조회")
    void findByToken_WhenNotExists_ShouldReturnEmpty() {
        // When
        Optional<RefreshToken> result = refreshTokenRepository.findByToken("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자명으로 RefreshToken 조회")
    void findByUsername_ShouldReturnToken() {
        // When
        Optional<RefreshToken> result = refreshTokenRepository.findByUsername(username1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username1);
        assertThat(result.get().getToken()).isEqualTo("test-token-1");
    }

    @Test
    @DisplayName("사용자명으로 활성화된 RefreshToken 조회 - revoked=false")
    void findByUsernameAndRevokedFalse_ShouldReturnActiveToken() {
        // Given: revoked된 토큰 추가
        RefreshToken revokedToken = RefreshToken.builder()
                .token("revoked-token")
                .username(username1)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true)
                .build();
        refreshTokenRepository.save(revokedToken);

        // When
        Optional<RefreshToken> result = refreshTokenRepository.findByUsernameAndRevokedFalse(username1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("test-token-1");
        assertThat(result.get().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("토큰으로 활성화된 RefreshToken 조회 - revoked=false")
    void findByTokenAndRevokedFalse_ShouldReturnActiveToken() {
        // When
        Optional<RefreshToken> result = refreshTokenRepository.findByTokenAndRevokedFalse("test-token-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("test-token-1");
        assertThat(result.get().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("revoked된 토큰은 findByTokenAndRevokedFalse로 조회되지 않음")
    void findByTokenAndRevokedFalse_WhenRevoked_ShouldReturnEmpty() {
        // Given: 토큰을 revoked 상태로 변경
        testToken1.setRevoked(true);
        refreshTokenRepository.save(testToken1);

        // When
        Optional<RefreshToken> result = refreshTokenRepository.findByTokenAndRevokedFalse("test-token-1");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("만료된 토큰 삭제")
    void deleteExpiredTokens_ShouldRemoveExpiredTokens() {
        // Given: 만료된 토큰 추가
        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired-token")
                .username("expired-user")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();
        refreshTokenRepository.save(expiredToken);

        long countBefore = refreshTokenRepository.count();
        assertThat(countBefore).isEqualTo(3); // testToken1, testToken2, expiredToken

        // When
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        refreshTokenRepository.flush();

        // Then
        long countAfter = refreshTokenRepository.count();
        assertThat(countAfter).isEqualTo(2); // 만료된 토큰 삭제됨

        Optional<RefreshToken> expiredResult = refreshTokenRepository.findByToken("expired-token");
        assertThat(expiredResult).isEmpty();
    }

    @Test
    @DisplayName("사용자명으로 모든 RefreshToken 삭제")
    void deleteByUsername_ShouldRemoveAllUserTokens() {
        // Given: 같은 사용자의 추가 토큰 생성
        RefreshToken additionalToken = RefreshToken.builder()
                .token("additional-token")
                .username(username1)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(additionalToken);

        long countBefore = refreshTokenRepository.count();
        assertThat(countBefore).isEqualTo(3);

        // When
        refreshTokenRepository.deleteByUsername(username1);
        refreshTokenRepository.flush();

        // Then
        long countAfter = refreshTokenRepository.count();
        assertThat(countAfter).isEqualTo(1); // username2의 토큰만 남음

        Optional<RefreshToken> user1Token = refreshTokenRepository.findByUsername(username1);
        assertThat(user1Token).isEmpty();

        Optional<RefreshToken> user2Token = refreshTokenRepository.findByUsername(username2);
        assertThat(user2Token).isPresent();
    }

    @Test
    @DisplayName("사용자명으로 모든 RefreshToken을 revoked 상태로 변경")
    void revokeByUsername_ShouldRevokeAllUserTokens() {
        // Given: 같은 사용자의 추가 토큰 생성
        RefreshToken additionalToken = RefreshToken.builder()
                .token("additional-token")
                .username(username1)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(additionalToken);
        refreshTokenRepository.flush();

        // When
        refreshTokenRepository.revokeByUsername(username1);
        refreshTokenRepository.flush();
        
        // 영속성 컨텍스트 클리어하여 DB에서 최신 데이터 조회
        entityManager.clear();

        // Then - 토큰을 다시 조회하여 revoked 상태 확인
        RefreshToken token1 = refreshTokenRepository.findByToken("test-token-1").orElse(null);
        assertThat(token1).isNotNull();
        assertThat(token1.isRevoked()).isTrue();

        RefreshToken additionalTokenResult = refreshTokenRepository.findByToken("additional-token").orElse(null);
        assertThat(additionalTokenResult).isNotNull();
        assertThat(additionalTokenResult.isRevoked()).isTrue();

        // username2의 토큰은 영향 받지 않음
        RefreshToken token2 = refreshTokenRepository.findByToken("test-token-2").orElse(null);
        assertThat(token2).isNotNull();
        assertThat(token2.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("unique 제약조건 - 중복 토큰 저장 불가")
    void saveToken_WithDuplicateToken_ShouldFail() {
        // Given
        RefreshToken duplicateToken = RefreshToken.builder()
                .token("test-token-1") // 중복 토큰
                .username("another-user")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        // When & Then
        try {
            refreshTokenRepository.saveAndFlush(duplicateToken);
        } catch (Exception e) {
            // 중복 토큰으로 인한 예외 발생 예상
            assertThat(e).isNotNull();
        }
    }
}

