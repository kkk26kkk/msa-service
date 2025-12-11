package com.example.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Service의 Spring Security 설정 클래스
 * 
 * 주요 기능:
 * - JWT 토큰 기반 인증 설정
 * - 인증이 필요 없는 경로 설정 (헬스체크, H2 콘솔 등)
 * - Stateless 세션 정책 (JWT 기반 인증이므로 세션 사용 안 함)
 * 
 * 인증 흐름:
 * 1. Gateway에서 JWT 토큰을 검증하고 사용자 정보를 헤더에 추가
 * 2. Order Service는 요청의 Authorization 헤더에서 JWT 토큰을 추출
 * 3. JwtDecoder를 사용하여 토큰을 검증하고 사용자 정보를 추출
 * 4. 인증 성공 시 요청 처리, 실패 시 401 Unauthorized 반환
 * 
 * 주의: auth-service와 동일한 JWT secret을 사용해야 합니다.
 */
@Configuration
@EnableMethodSecurity  // 메서드 레벨 보안 활성화 (@PreAuthorize 등 사용 가능)
@ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    private final String secret;  // JWT 서명에 사용되는 비밀키

    /**
     * 설정 파일에서 JWT secret을 주입받음
     * 
     * @param secret JWT 서명에 사용되는 비밀키 문자열
     */
    public SecurityConfig(@Value("${security.jwt.secret}") String secret) {
        this.secret = secret;
    }

    /**
     * Spring Security 필터 체인 설정
     * 
     * 보안 정책:
     * - CSRF 비활성화: REST API이므로 CSRF 보호 불필요
     * - 인증 불필요 경로: /actuator/**, /h2-console/**, /orders/health
     * - 나머지 모든 요청: JWT 토큰 인증 필요
     * - Stateless 세션: JWT 기반 인증이므로 세션을 사용하지 않음
     * - OAuth2 Resource Server: JWT 토큰을 사용한 인증 활성화
     * 
     * @param http HttpSecurity 객체
     * @return 설정된 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)  // CSRF 보호 비활성화 (REST API)
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 경로
                .requestMatchers("/actuator/**", "/h2-console/**", "/orders/health").permitAll()
                // 나머지 모든 요청은 JWT 토큰 인증 필요
                .anyRequest().authenticated())
            // Stateless 세션 정책: JWT 기반 인증이므로 세션을 생성하지 않음
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // OAuth2 Resource Server 설정: JWT 토큰을 사용한 인증
            // Authorization 헤더의 Bearer 토큰을 자동으로 검증합니다.
            // JWT의 "roles" 클레임을 Spring Security의 GrantedAuthority로 변환합니다.
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        // H2 콘솔을 위한 프레임 옵션 설정
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * JWT 토큰을 디코딩하는 JwtDecoder 빈 등록
     * 
     * JwtDecoder는 Authorization 헤더의 Bearer 토큰을 검증하고
     * 사용자 정보를 추출하는 데 사용됩니다.
     * 
     * 주의: auth-service의 JwtTokenProvider와 동일한 secret을 사용해야 합니다.
     *      설정 파일: order-service.yml 또는 config-service의 order-service.yml
     * 
     * @return NimbusJwtDecoder 인스턴스
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // SecretKey 생성 (HMAC SHA-256 알고리즘 사용)
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        // NimbusJwtDecoder 생성 및 반환
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * JWT 토큰의 "roles" 클레임을 Spring Security의 GrantedAuthority로 변환하는 컨버터
     * 
     * Auth Service에서 발급한 JWT 토큰에는 "roles" 클레임에 역할 목록이 포함되어 있습니다.
     * 예: {"roles": ["ROLE_ADMIN", "ROLE_USER"]}
     * 
     * 이 컨버터는 JWT의 "roles" 클레임을 읽어서 각 역할을 SimpleGrantedAuthority로 변환합니다.
     * 
     * @return JwtAuthenticationConverter 인스턴스
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RolesClaimConverter());
        return converter;
    }

    /**
     * JWT의 "roles" 클레임을 GrantedAuthority로 변환하는 컨버터
     */
    private static class RolesClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
            // JWT의 "roles" 클레임 추출
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) jwt.getClaim("roles");
            
            if (roles == null || roles.isEmpty()) {
                return List.of();
            }
            
            // 각 역할을 SimpleGrantedAuthority로 변환
            // JWT에 이미 "ROLE_" 접두사가 포함되어 있으므로 그대로 사용
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role))
                    .collect(Collectors.toList());
        }
    }
}
