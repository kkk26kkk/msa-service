package com.example.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

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
                String clientIp = extractClientIp(request);
                
                log.info("[GATEWAY] {} {} -> Client IP: {}", 
                    request.getMethod(), 
                    request.getURI(), 
                    clientIp);
                
                return chain.filter(exchange);
            };
        }

        /**
         * 클라이언트 IP 주소 추출
         * 
         * 우선순위:
         * 1. X-Forwarded-For 헤더 (프록시/로드밸런서 환경)
         * 2. X-Real-IP 헤더 (Nginx 등)
         * 3. RemoteAddress (직접 연결)
         * 
         * IPv6 localhost는 IPv4로 변환 (::1 또는 0:0:0:0:0:0:0:1 → 127.0.0.1)
         */
        private String extractClientIp(ServerHttpRequest request) {
            // 1. X-Forwarded-For 헤더 확인 (첫 번째 IP만 추출)
            String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                String firstIp = forwardedFor.split(",")[0].trim();
                return normalizeIp(firstIp);
            }

            // 2. X-Real-IP 헤더 확인
            String realIp = request.getHeaders().getFirst("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) {
                return normalizeIp(realIp);
            }

            // 3. RemoteAddress 사용
            var remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                String remoteIp = remoteAddress.getAddress().getHostAddress();
                return normalizeIp(remoteIp);
            }

            return "unknown";
        }

        /**
         * IP 주소 정규화
         * - IPv6 localhost를 IPv4로 변환
         * - ::1 또는 0:0:0:0:0:0:0:1 → 127.0.0.1
         */
        private String normalizeIp(String ip) {
            if (ip == null || ip.isEmpty()) {
                return "unknown";
            }

            // IPv6 localhost를 IPv4로 변환
            if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                return "127.0.0.1";
            }

            return ip;
        }

        public static class Config {
            // 필터 설정이 필요한 경우 여기에 추가
        }
    }
}
