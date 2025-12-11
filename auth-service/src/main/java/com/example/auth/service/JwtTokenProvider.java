package com.example.auth.service;

import com.example.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 토큰 생성 및 파싱을 담당하는 컴포넌트
 * 
 * 주요 기능:
 * - JWT 토큰 생성: 사용자 인증 정보를 기반으로 JWT 토큰 생성
 * - JWT 토큰 파싱: 토큰에서 Claims(클레임) 추출
 * 
 * JWT 토큰 구조:
 * - Header: 토큰 타입 및 서명 알고리즘 정보
 * - Payload: 사용자 정보 (subject, roles, issuedAt, expiration 등)
 * - Signature: SecretKey를 사용한 서명 (토큰 무결성 보장)
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;  // JWT 서명에 사용되는 비밀키 (HMAC SHA-256)
    private final long validitySeconds; // 토큰 유효 기간 (초 단위)

    /**
     * JWT 설정 정보를 받아 SecretKey와 유효 기간을 초기화
     * 
     * @param properties JWT 설정 속성 (secret, accessTokenValiditySeconds)
     */
    public JwtTokenProvider(JwtProperties properties) {
        // 설정 파일의 secret 문자열을 HMAC SHA-256 알고리즘용 SecretKey로 변환
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.validitySeconds = properties.getAccessTokenValiditySeconds();
    }

    /**
     * 인증된 사용자 정보를 기반으로 JWT 토큰을 생성
     * 
     * 토큰에 포함되는 정보:
     * - subject: 사용자명 (username)
     * - roles: 사용자 역할 목록 (예: ["ROLE_ADMIN", "ROLE_USER"])
     * - issuedAt: 토큰 발급 시간
     * - expiration: 토큰 만료 시간
     * 
     * @param authentication Spring Security의 인증 객체
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(validitySeconds);

        // 사용자의 권한(역할) 목록을 문자열 리스트로 변환
        // 예: [ROLE_ADMIN, ROLE_USER] -> ["ROLE_ADMIN", "ROLE_USER"]
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // JWT 토큰 빌드 및 생성
        return Jwts.builder()
                .subject(authentication.getName())        // 사용자명 (토큰의 주체)
                .claim("roles", roles)                   // 사용자 역할 목록 (커스텀 클레임)
                .issuedAt(Date.from(now))                 // 토큰 발급 시간
                .expiration(Date.from(expiry))            // 토큰 만료 시간
                .signWith(secretKey)                      // SecretKey로 서명
                .compact();                               // 최종 JWT 문자열 생성
    }

    /**
     * JWT 토큰을 파싱하여 Claims(클레임) 추출
     * 
     * 이 메서드는 토큰의 서명을 검증하고, 만료 시간을 확인합니다.
     * 검증 실패 시 예외가 발생합니다.
     * 
     * @param token 파싱할 JWT 토큰 문자열
     * @return 토큰에서 추출한 Claims 객체 (subject, roles, issuedAt, expiration 등)
     * @throws Exception 토큰이 유효하지 않거나 만료된 경우
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)                    // SecretKey로 서명 검증
                .build()
                .parseSignedClaims(token)                // 서명된 토큰 파싱
                .getPayload();                            // Payload(Claims) 추출
    }

    public long getValiditySeconds() {
        return validitySeconds;
    }
}
