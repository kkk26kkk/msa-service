package com.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 인증 필터 (향후 확장용)
 * 
 * 현재는 단순 로깅만 수행하지만, 
 * JWT 토큰 검증, 사용자 인증 등의 로직을 추가할 수 있습니다.
 */
@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 현재는 모든 요청을 허용 (개발 단계)
            // 향후 JWT 토큰 검증 로직 추가 가능
            log.debug("Authentication filter processing request: {} {}", 
                request.getMethod(), request.getURI());
            
            if (!isAuthValid(request)) {
                log.warn("Unauthorized request: {} {}", request.getMethod(), request.getURI());
                return handleUnauthorized(exchange);
            }
            
            return chain.filter(exchange);
        };
    }

    private boolean isAuthValid(ServerHttpRequest request) {
        // 개발 단계에서는 모든 요청 허용
        // 실제 환경에서는 JWT 토큰 검증 로직 구현
        return true;
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    public static class Config {
        // 인증 필터 설정이 필요한 경우 여기에 추가
    }
}
