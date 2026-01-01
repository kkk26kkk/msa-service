package com.example.gateway.filter;

import com.example.gateway.security.JwtTokenValidator;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Rate Limiting 필터
 * 
 * 주요 기능:
 * - IP별 Rate Limiting
 * - 사용자별 Rate Limiting (JWT 토큰 기반)
 * - Rate Limit 초과 시 429 Too Many Requests 응답
 * 
 * Rate Limiting 전략:
 * - 기본 제한: 초당 10개 요청 (IP별)
 * - 인증된 사용자: 초당 50개 요청 (사용자별)
 * - 특정 엔드포인트: 별도 제한 설정 가능
 * 
 * 동작 흐름:
 * 1. 요청에서 IP 주소 또는 사용자 정보 추출
 * 2. 해당 키에 대한 RateLimiter 인스턴스 조회 또는 생성
 * 3. RateLimiter를 통해 요청 허용 여부 확인
 * 4. 허용되면 다음 필터로 전달, 초과 시 429 응답 반환
 */
@Slf4j
@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final JwtTokenValidator jwtTokenValidator;

    public RateLimitingFilter(
            RateLimiterRegistry rateLimiterRegistry,
            JwtTokenValidator jwtTokenValidator) {
        super(Config.class);
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public String name() {
        return "RateLimitingFilter";
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            
            // Rate Limiting 제외 경로 확인
            if (isExcludedPath(path)) {
                log.debug("RateLimitingFilter - Path {} is excluded from rate limiting", path);
                return chain.filter(exchange);
            }

            // Rate Limiting 키 결정 (IP 또는 사용자)
            String rateLimitKey = determineRateLimitKey(request, config);
            String rateLimiterName = config.getRateLimiterName(rateLimitKey);
            
            log.debug("RateLimitingFilter - Path: {}, Key: {}, RateLimiter: {}", 
                path, rateLimitKey, rateLimiterName);

            // RateLimiter 인스턴스 조회 또는 생성
            RateLimiter rateLimiter = getOrCreateRateLimiter(rateLimiterName, rateLimitKey, config);

            // Rate Limiting 적용 (Reactor 비동기 방식)
            return Mono.fromCallable(() -> {
                // RateLimiter가 허용하는지 확인
                if (rateLimiter.acquirePermission()) {
                    log.debug("RateLimitingFilter - Request allowed for key: {}", rateLimitKey);
                    return true;
                } else {
                    log.warn("RateLimitingFilter - Rate limit exceeded for key: {}", rateLimitKey);
                    return false;
                }
            })
            .subscribeOn(Schedulers.boundedElastic())  // 블로킹 작업을 별도 스레드에서 실행
            .flatMap(allowed -> {
                if (allowed) {
                    return chain.filter(exchange);
                } else {
                    return handleRateLimitExceeded(exchange, rateLimitKey);
                }
            })
            .onErrorResume(ex -> {
                log.error("RateLimitingFilter - Error during rate limiting for key: {}", rateLimitKey, ex);
                // 에러 발생 시 요청을 허용 (fail-open)
                return chain.filter(exchange);
            });
        };
    }

    /**
     * Rate Limiting에서 제외할 경로 확인
     */
    private boolean isExcludedPath(String path) {
        // Actuator 엔드포인트는 Rate Limiting 제외
        return path.startsWith("/actuator");
    }

    /**
     * Rate Limiting 키 결정
     * - 인증된 사용자가 있으면 사용자명 사용
     * - 없으면 IP 주소 사용
     */
    private String determineRateLimitKey(ServerHttpRequest request, Config config) {
        // 1. 인증된 사용자 확인 (JWT 토큰 기반)
        String authenticatedUser = getAuthenticatedUser(request);
        if (authenticatedUser != null && config.isUserBasedRateLimitEnabled()) {
            return "user:" + authenticatedUser;
        }

        // 2. IP 주소 사용
        String clientIp = extractClientIp(request);
        return "ip:" + clientIp;
    }

    /**
     * 인증된 사용자 추출
     */
    private String getAuthenticatedUser(ServerHttpRequest request) {
        try {
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtTokenValidator.isValid(token)) {
                    return jwtTokenValidator.parseClaims(token).getSubject();
                }
            }
        } catch (Exception ex) {
            log.debug("RateLimitingFilter - Failed to extract authenticated user", ex);
        }
        return null;
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String extractClientIp(ServerHttpRequest request) {
        // 1. X-Forwarded-For 헤더 확인
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

    /**
     * RateLimiter 인스턴스 조회 또는 생성
     */
    private RateLimiter getOrCreateRateLimiter(String name, String key, Config config) {
        // Registry에서 먼저 조회
        try {
            return rateLimiterRegistry.rateLimiter(name);
        } catch (Exception ex) {
            // Registry에 없으면 동적으로 생성
            log.debug("RateLimitingFilter - Creating new RateLimiter: {}", name);
            
            // 키에 따라 적절한 limitForPeriod 사용
            int limitForPeriod = config.getLimitForPeriod(key);
            
            RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofSeconds(config.getLimitRefreshPeriodSeconds()))
                .timeoutDuration(Duration.ofMillis(config.getTimeoutDurationMillis()))
                .build();

            return rateLimiterRegistry.rateLimiter(name, rateLimiterConfig);
        }
    }

    /**
     * Rate Limit 초과 시 처리
     */
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, String key) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("X-RateLimit-Exceeded", "true");
        response.getHeaders().add("X-RateLimit-Key", key);
        
        log.warn("RateLimitingFilter - Rate limit exceeded for key: {}", key);
        return response.setComplete();
    }

    /**
     * Rate Limiting 필터 설정
     */
    @Getter
    @Setter
    public static class Config {
        private int limitForPeriod = 10;  // 기본: 초당 10개 요청
        private int limitRefreshPeriodSeconds = 1;  // 1초마다 리프레시
        private long timeoutDurationMillis = 0;  // 타임아웃 없음
        private boolean userBasedRateLimitEnabled = true;  // 사용자별 Rate Limiting 활성화
        private int userLimitForPeriod = 50;  // 인증된 사용자: 초당 50개 요청

        /**
         * RateLimiter 이름 생성
         */
        public String getRateLimiterName(String key) {
            if (key.startsWith("user:")) {
                // 사용자별 RateLimiter
                return "user-rate-limiter";
            } else {
                // IP별 RateLimiter
                return "ip-rate-limiter";
            }
        }

        /**
         * 키에 따른 Rate Limit 값 반환
         */
        public int getLimitForPeriod(String key) {
            if (key.startsWith("user:")) {
                return userLimitForPeriod;
            } else {
                return limitForPeriod;
            }
        }
    }
}

