package com.example.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 토큰 검증을 담당하는 컴포넌트
 * 
 * 주요 기능:
 * - JWT 토큰 유효성 검증 (서명 검증, 만료 시간 확인)
 * - JWT 토큰에서 Claims(클레임) 추출
 * 
 * 사용 위치:
 * - AuthenticationFilter: Gateway에서 요청 시 JWT 토큰 검증
 * 
 * 검증 항목:
 * - 토큰 서명 검증: SecretKey를 사용한 서명이 올바른지 확인
 * - 토큰 만료 시간 확인: 토큰이 만료되지 않았는지 확인
 * - 토큰 형식 검증: JWT 형식이 올바른지 확인
 */
@Component
public class JwtTokenValidator {

    private final SecretKey secretKey;  // JWT 서명 검증에 사용되는 비밀키

    /**
     * 설정 파일에서 JWT secret을 읽어 SecretKey 생성
     * 
     * 주의: auth-service와 동일한 secret을 사용해야 합니다.
     *      설정 파일: gateway-service.yml 또는 config-service의 gateway-service.yml
     * 
     * @param secret JWT 서명에 사용되는 비밀키 문자열
     */
    public JwtTokenValidator(@Value("${security.jwt.secret}") String secret) {
        // 설정 파일의 secret 문자열을 HMAC SHA-256 알고리즘용 SecretKey로 변환
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWT 토큰의 유효성을 검증
     * 
     * 검증 항목:
     * - 토큰 서명 검증
     * - 토큰 만료 시간 확인
     * - 토큰 형식 검증
     * 
     * @param token 검증할 JWT 토큰 문자열
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValid(String token) {
        try {
            // parseClaims()가 성공하면 토큰이 유효함
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            // 예외 발생 시 토큰이 유효하지 않음
            return false;
        }
    }

    /**
     * JWT 토큰을 파싱하여 Claims(클레임) 추출
     * 
     * 이 메서드는 토큰의 서명을 검증하고, 만료 시간을 확인합니다.
     * 검증 실패 시 예외가 발생합니다.
     * 
     * 추출 가능한 정보:
     * - subject: 사용자명
     * - roles: 사용자 역할 목록
     * - issuedAt: 토큰 발급 시간
     * - expiration: 토큰 만료 시간
     * 
     * @param token 파싱할 JWT 토큰 문자열
     * @return 토큰에서 추출한 Claims 객체
     * @throws Exception 토큰이 유효하지 않거나 만료된 경우
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)                    // SecretKey로 서명 검증
                .build()
                .parseSignedClaims(token)                // 서명된 토큰 파싱
                .getPayload();                            // Payload(Claims) 추출
    }
}
