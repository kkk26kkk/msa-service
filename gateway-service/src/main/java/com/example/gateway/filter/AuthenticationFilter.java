package com.example.gateway.filter;

import com.example.gateway.security.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Gateway에서 JWT 토큰 검증을 수행하는 필터
 * 
 * 주요 기능:
 * - 요청 경로에 따라 인증 필요 여부 판단
 * - JWT 토큰 검증 (서명, 만료 시간 확인)
 * - 검증된 사용자 정보를 헤더에 추가하여 하위 서비스로 전달
 * 
 * 동작 흐름:
 * 1. 요청 경로가 화이트리스트에 있는지 확인 (인증 불필요 경로)
 * 2. Authorization 헤더에서 JWT 토큰 추출
 * 3. JWT 토큰 유효성 검증
 * 4. 검증 성공 시 사용자 정보를 헤더에 추가하여 다음 필터로 전달
 * 5. 검증 실패 시 401 Unauthorized 응답 반환
 * 
 * 하위 서비스로 전달되는 헤더:
 * - X-Authenticated-User: 인증된 사용자명
 * - X-User-Roles: 사용자 역할 목록 (쉼표로 구분)
 */
@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    /**
     * 인증 없이 접근 가능한 경로 목록
     * 
     * - /api/auth: 인증 서비스 API (로그인 등) - 원본 경로
     * - /auth: 인증 서비스 API (RewritePath 후 경로)
     * - /auth-service: 인증 서비스 직접 접근
     * - /actuator: Spring Boot Actuator 엔드포인트
     * - /auth/health: 인증 서비스 헬스 체크
     */
    private static final List<String> WHITELIST_PATH_PREFIXES = List.of(
            "/api/auth",      // 원본 경로 (Gateway를 통한 접근)
            "/auth",          // RewritePath 후 경로
            "/auth-service",  // 인증 서비스 직접 접근
            "/actuator",     // Spring Boot Actuator 엔드포인트
            "/auth/health"   // 인증 서비스 헬스 체크
    );

    private final JwtTokenValidator jwtTokenValidator;

    public AuthenticationFilter(JwtTokenValidator jwtTokenValidator) {
        super(Config.class);
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public String name() {
        return "AuthenticationFilter";
    }

    /**
     * Gateway 필터 적용
     * 
     * 필터 처리 순서:
     * 1. 화이트리스트 경로 확인 → 인증 불필요 시 바로 통과
     * 2. Authorization 헤더 확인 → 없거나 형식이 잘못되면 401 반환
     * 3. JWT 토큰 추출 및 검증 → 유효하지 않으면 401 반환
     * 4. 토큰에서 사용자 정보 추출 → 헤더에 추가하여 하위 서비스로 전달
     * 
     * @param config 필터 설정 (현재는 사용하지 않음)
     * @return GatewayFilter 인스턴스
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // 1단계: 화이트리스트 경로 확인 (인증 불필요)
            boolean whitelisted = isWhitelisted(path);
            log.info("AuthenticationFilter - Path: {}, Whitelisted: {}, Method: {}", path, whitelisted, request.getMethod());
            
            if (whitelisted || isOptionsRequest(request)) {
                log.info("AuthenticationFilter - Path {} is whitelisted or OPTIONS request, skipping authentication", path);
                return chain.filter(exchange);
            }

            // 2단계: Authorization 헤더 확인
            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader == null || !authorizationHeader.toLowerCase(Locale.ENGLISH).startsWith("bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                return handleUnauthorized(exchange);
            }

            // 3단계: JWT 토큰 추출 (Bearer 접두사 제거)
            String token = authorizationHeader.substring(7);
            
            // 4단계: JWT 토큰 유효성 검증
            if (!jwtTokenValidator.isValid(token)) {
                log.warn("Invalid JWT token for path: {}", path);
                return handleUnauthorized(exchange);
            }

            // 5단계: 토큰에서 사용자 정보 추출
            Claims claims = jwtTokenValidator.parseClaims(token);
            List<?> roleClaims = claims.get("roles", List.class);
            String roles = roleClaims == null
                    ? ""
                    : roleClaims.stream().map(Object::toString).collect(Collectors.joining(","));
            
            // 6단계: 사용자 정보를 헤더에 추가하여 하위 서비스로 전달
            // 하위 서비스(member-service, order-service)는 이 헤더를 통해
            // 인증된 사용자 정보를 확인할 수 있습니다.
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Authenticated-User", claims.getSubject())  // 사용자명
                    .header("X-User-Roles", roles)                        // 역할 목록
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isOptionsRequest(ServerHttpRequest request) {
        return HttpMethod.OPTIONS.equals(request.getMethod());
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    public static class Config {
    }
}
