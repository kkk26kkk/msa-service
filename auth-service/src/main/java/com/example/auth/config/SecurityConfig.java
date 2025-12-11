package com.example.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * Spring Security 설정 클래스
 * 
 * 주요 설정:
 * - 인증이 필요 없는 경로 설정 (로그인, 헬스체크, H2 콘솔 등)
 * - Stateless 세션 정책 (JWT 기반 인증이므로 세션 사용 안 함)
 * - 비밀번호 암호화를 위한 BCryptPasswordEncoder 설정
 * - AuthenticationManager 빈 등록 (로그인 시 사용)
 */
@Configuration
@EnableMethodSecurity  // 메서드 레벨 보안 활성화 (@PreAuthorize 등 사용 가능)
@EnableConfigurationProperties(JwtProperties.class)  // JWT 설정 속성 활성화
public class SecurityConfig {

    /**
     * Spring Security 필터 체인 설정
     * 
     * 보안 정책:
     * - CSRF 비활성화: REST API이므로 CSRF 보호 불필요
     * - 인증 불필요 경로: /auth/login, /actuator/**, /h2-console/**
     * - 나머지 모든 요청: 인증 필요
     * - Stateless 세션: JWT 기반 인증이므로 세션을 사용하지 않음
     * 
     * @param http HttpSecurity 객체
     * @return 설정된 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)  // CSRF 보호 비활성화 (REST API)
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 경로
                .requestMatchers("/auth/login", "/auth/health", "/actuator/**", "/h2-console/**").permitAll()
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated())
            // Stateless 세션 정책: JWT 기반 인증이므로 세션을 생성하지 않음
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // H2 콘솔을 위한 프레임 옵션 설정
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * AuthenticationManager 빈 등록
     * 
     * AuthenticationManager는 사용자 인증을 담당합니다.
     * AuthController의 login 메서드에서 사용되며,
     * 내부적으로 AuthUserDetailsService를 호출하여 사용자 정보를 조회하고
     * 비밀번호를 검증합니다.
     * 
     * @param configuration AuthenticationConfiguration 객체
     * @return AuthenticationManager 인스턴스
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈 등록
     * 
     * BCryptPasswordEncoder는 BCrypt 해시 알고리즘을 사용하여
     * 비밀번호를 안전하게 암호화합니다.
     * 
     * 사용 위치:
     * - DataInitializer: 초기 사용자 생성 시 비밀번호 암호화
     * - AuthService: 사용자 등록 시 비밀번호 암호화
     * - AuthUserDetailsService: 로그인 시 비밀번호 검증
     * 
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
