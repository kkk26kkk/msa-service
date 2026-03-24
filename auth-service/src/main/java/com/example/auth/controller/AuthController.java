    package com.example.auth.controller;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.entity.RefreshToken;
import com.example.auth.service.AuthUserDetailsService;
import com.example.auth.service.JwtTokenProvider;
import com.example.auth.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 인증 서비스의 REST API 컨트롤러
 * 
 * 주요 기능:
 * - 사용자 로그인 및 JWT 토큰 발급 (Access Token + Refresh Token)
 * - 토큰 갱신 API
 * - 로그아웃 API
 * - 헬스 체크 엔드포인트 제공
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final AuthUserDetailsService authUserDetailsService;

    public AuthController(AuthenticationManager authenticationManager, 
                         JwtTokenProvider jwtTokenProvider,
                         TokenService tokenService,
                         AuthUserDetailsService authUserDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenService = tokenService;
        this.authUserDetailsService = authUserDetailsService;
    }

    /**
     * 사용자 로그인 및 JWT 토큰 발급
     * 
     * 인증 프로세스:
     * 1. 클라이언트로부터 username/password를 받음
     * 2. AuthenticationManager를 통해 사용자 인증 수행 (AuthUserDetailsService 호출)
     * 3. 인증 성공 시 Access Token과 Refresh Token 생성
     * 4. Refresh Token을 데이터베이스에 저장
     * 5. 생성된 토큰들을 클라이언트에 반환
     * 
     * @param request 로그인 요청 (username, password)
     * @return Access Token, Refresh Token 및 사용자 정보가 포함된 응답
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // 1단계: 사용자 인증 (username/password 검증)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        // 2단계: 인증 정보를 SecurityContext에 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 3단계: Access Token 생성
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        
        // 4단계: Refresh Token 생성
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication.getName());
        
        // 5단계: Refresh Token 만료 시간 계산 및 저장
        Claims refreshTokenClaims = jwtTokenProvider.parseClaims(refreshToken);
        LocalDateTime expiresAt = refreshTokenClaims.getExpiration().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
        tokenService.saveRefreshToken(refreshToken, authentication.getName(), expiresAt);
        
        // 6단계: 사용자의 권한(역할) 정보 추출
        String[] roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toArray(String[]::new);
        
        // 7단계: 응답 객체 생성 및 반환
        AuthResponse response = new AuthResponse(
                accessToken,                                    // Access Token
                refreshToken,                                   // Refresh Token
                "Bearer",                                       // 토큰 타입
                jwtTokenProvider.getAccessTokenValiditySeconds(), // Access Token 유효 기간(초)
                authentication.getName(),                       // 사용자명
                roles                                           // 사용자 역할 배열
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 토큰 갱신 API
     * 
     * Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급합니다.
     * 
     * @param request Refresh Token 요청
     * @return 새로운 Access Token, Refresh Token 및 사용자 정보가 포함된 응답
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        // 1단계: Refresh Token 검증
        Optional<RefreshToken> refreshTokenOpt = tokenService.validateRefreshToken(request.getRefreshToken());
        
        if (refreshTokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired refresh token");
        }
        
        RefreshToken refreshTokenEntity = refreshTokenOpt.get();
        String username = refreshTokenEntity.getUsername();
        
        // 2단계: 사용자 정보 조회
        UserDetails userDetails = authUserDetailsService.loadUserByUsername(username);
        
        // 3단계: 인증 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        
        // 4단계: 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
        
        // 5단계: 새로운 Refresh Token 생성 (기존 토큰 무효화)
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);
        
        // 6단계: 새로운 Refresh Token 저장 (기존 토큰은 자동으로 삭제됨)
        Claims newRefreshTokenClaims = jwtTokenProvider.parseClaims(newRefreshToken);
        LocalDateTime expiresAt = newRefreshTokenClaims.getExpiration().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
        tokenService.saveRefreshToken(newRefreshToken, username, expiresAt);
        
        // 7단계: 사용자 역할 정보 추출
        String[] roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toArray(String[]::new);
        
        // 8단계: 응답 생성
        AuthResponse response = new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenProvider.getAccessTokenValiditySeconds(),
                username,
                roles
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃 API
     * 
     * Refresh Token을 무효화하여 로그아웃을 처리합니다.
     * 
     * @param request Refresh Token 요청
     * @return 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        // Refresh Token 삭제
        tokenService.deleteRefreshToken(request.getRefreshToken());
        
        return ResponseEntity.ok("Logout successful");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
