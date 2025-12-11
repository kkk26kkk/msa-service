package com.example.auth.controller;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.service.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 서비스의 REST API 컨트롤러
 * 
 * 주요 기능:
 * - 사용자 로그인 및 JWT 토큰 발급
 * - 헬스 체크 엔드포인트 제공
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 사용자 로그인 및 JWT 토큰 발급
     * 
     * 인증 프로세스:
     * 1. 클라이언트로부터 username/password를 받음
     * 2. AuthenticationManager를 통해 사용자 인증 수행 (AuthUserDetailsService 호출)
     * 3. 인증 성공 시 JWT 토큰 생성 (사용자명, 역할 정보 포함)
     * 4. 생성된 토큰을 클라이언트에 반환
     * 
     * @param request 로그인 요청 (username, password)
     * @return JWT 토큰 및 사용자 정보가 포함된 응답
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // 1단계: 사용자 인증 (username/password 검증)
        // AuthenticationManager는 내부적으로 AuthUserDetailsService를 호출하여
        // 데이터베이스에서 사용자 정보를 조회하고 비밀번호를 검증합니다.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        // 2단계: 인증 정보를 SecurityContext에 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 3단계: 인증된 사용자 정보를 기반으로 JWT 토큰 생성
        // 토큰에는 사용자명(subject)과 역할(roles) 정보가 포함됩니다.
        String token = jwtTokenProvider.generateToken(authentication);
        
        // 4단계: 사용자의 권한(역할) 정보 추출
        String[] roles = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .toArray(String[]::new);
        
        // 5단계: 응답 객체 생성 및 반환
        AuthResponse response = new AuthResponse(
                token,                                    // JWT 토큰
                "Bearer",                                 // 토큰 타입
                jwtTokenProvider.getValiditySeconds(),    // 토큰 유효 기간(초)
                authentication.getName(),                // 사용자명
                roles                                     // 사용자 역할 배열
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
