package com.example.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Gateway 설정 및 커스텀 필터
 */
@Configuration
public class GatewayConfig {

    /**
     * 요청 로깅 필터
     */
    @Slf4j
    @Component
    public static class RequestLoggingGatewayFilterFactory 
            extends AbstractGatewayFilterFactory<RequestLoggingGatewayFilterFactory.Config> {

        public RequestLoggingGatewayFilterFactory() {
            super(Config.class);
        }

        @Override
        public GatewayFilter apply(Config config) {
            return (exchange, chain) -> {
                ServerHttpRequest request = exchange.getRequest();
                String clientIp = request.getHeaders().getFirst("X-Forwarded-For");
                if (clientIp == null) {
                    clientIp = request.getRemoteAddress() != null ? 
                        request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
                }
                
                log.info("[GATEWAY] {} {} -> Client IP: {}", 
                    request.getMethod(), 
                    request.getURI(), 
                    clientIp);
                
                return chain.filter(exchange);
            };
        }

        public static class Config {
            // 필터 설정이 필요한 경우 여기에 추가
        }
    }
}
