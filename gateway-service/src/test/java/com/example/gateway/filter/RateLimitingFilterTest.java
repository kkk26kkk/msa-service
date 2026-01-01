package com.example.gateway.filter;

import com.example.gateway.security.JwtTokenValidator;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RateLimitingFilter 통합 테스트
 * 
 * SERVICE-ENHANCEMENT-PLAN.md의 3단계 테스트 시나리오를 기반으로 작성:
 * 1. Rate Limit 초과 시 429 응답 확인
 * 2. Rate Limit 리셋 후 정상 동작 확인
 * 3. IP별 Rate Limiting 동작 확인
 * 4. 사용자별 Rate Limiting 동작 확인
 */
@DisplayName("RateLimitingFilter 통합 테스트")
class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;
    private RateLimiterRegistry rateLimiterRegistry;
    private JwtTokenValidator jwtTokenValidator;
    private GatewayFilterChain mockChain;

    @BeforeEach
    void setUp() {
        // 각 테스트마다 새로운 RateLimiterRegistry 생성 (테스트 격리)
        rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        
        // JwtTokenValidator Mock 생성
        jwtTokenValidator = mock(JwtTokenValidator.class);
        
        // RateLimitingFilter 생성
        rateLimitingFilter = new RateLimitingFilter(rateLimiterRegistry, jwtTokenValidator);
        
        // GatewayFilterChain Mock 생성
        mockChain = mock(GatewayFilterChain.class);
        when(mockChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Rate Limit 초과 시 429 Too Many Requests 응답")
    void rateLimitExceeded_ShouldReturn429() {
        // Given: Rate Limit 설정 (초당 2개 요청)
        // RateLimiter를 미리 생성하여 Registry에 등록
        io.github.resilience4j.ratelimiter.RateLimiterConfig rateLimiterConfig = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        
        rateLimiterRegistry.rateLimiter("ip-rate-limiter", rateLimiterConfig);
        
        RateLimitingFilter.Config config = new RateLimitingFilter.Config();
        config.setLimitForPeriod(2);
        config.setLimitRefreshPeriodSeconds(1);
        config.setUserBasedRateLimitEnabled(false);

        var filter = rateLimitingFilter.apply(config);
        
        // Mock 요청 생성 (같은 IP)
        String clientIp = "192.168.1.100";

        // When & Then: 3개의 요청을 연속으로 보냄 (Rate Limit = 2)
        // 처음 2개는 허용
        var exchange1 = createMockExchange(clientIp, null);
        filter.filter(exchange1, mockChain).block();
        assertThat(exchange1.getResponse().getStatusCode())
            .as("First request should be allowed")
            .isNull();

        var exchange2 = createMockExchange(clientIp, null);
        filter.filter(exchange2, mockChain).block();
        assertThat(exchange2.getResponse().getStatusCode())
            .as("Second request should be allowed")
            .isNull();

        // 3번째는 429 응답
        var exchange3 = createMockExchange(clientIp, null);
        filter.filter(exchange3, mockChain).block();
        
        assertThat(exchange3.getResponse().getStatusCode())
            .as("Third request should be rate limited")
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange3.getResponse().getHeaders().getFirst("X-RateLimit-Exceeded")).isEqualTo("true");
        assertThat(exchange3.getResponse().getHeaders().getFirst("X-RateLimit-Key")).contains("ip:192.168.1.100");
    }

    @Test
    @DisplayName("Rate Limit 리셋 후 정상 동작")
    void rateLimitReset_ShouldAllowRequests() throws InterruptedException {
        // Given: Rate Limit 설정 (초당 2개 요청, 1초 리프레시)
        RateLimitingFilter.Config config = new RateLimitingFilter.Config();
        config.setLimitForPeriod(2);
        config.setLimitRefreshPeriodSeconds(1);
        config.setUserBasedRateLimitEnabled(false);

        var filter = rateLimitingFilter.apply(config);
        String clientIp = "192.168.1.101";

        // When: 처음 2개 요청
        var exchange1 = createMockExchange(clientIp, null);
        var exchange2 = createMockExchange(clientIp, null);
        
        StepVerifier.create(filter.filter(exchange1, mockChain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange2, mockChain)).verifyComplete();

        // 1초 대기 (Rate Limit 리셋)
        Thread.sleep(1100);

        // 다시 요청
        var exchange3 = createMockExchange(clientIp, null);
        StepVerifier.create(filter.filter(exchange3, mockChain)).verifyComplete();

        // Then: 리셋 후 요청은 정상 처리
        assertThat(exchange3.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("IP별 Rate Limiting - 다른 IP는 독립적으로 제한")
    void ipBasedRateLimiting_DifferentIps_ShouldBeIndependent() {
        // Given: Rate Limit 설정 (초당 2개 요청)
        RateLimitingFilter.Config config = new RateLimitingFilter.Config();
        config.setLimitForPeriod(2);
        config.setUserBasedRateLimitEnabled(false);

        var filter = rateLimitingFilter.apply(config);

        // 서로 다른 IP
        String ip1 = "192.168.1.100";
        String ip2 = "192.168.1.200";

        // When: 각 IP에서 2개씩 요청
        var exchange1 = createMockExchange(ip1, null);
        var exchange2 = createMockExchange(ip1, null);
        var exchange3 = createMockExchange(ip2, null);
        var exchange4 = createMockExchange(ip2, null);

        StepVerifier.create(filter.filter(exchange1, mockChain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange2, mockChain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange3, mockChain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange4, mockChain)).verifyComplete();

        // Then: 모두 정상 처리 (각 IP는 독립적)
        assertThat(exchange1.getResponse().getStatusCode()).isNull();
        assertThat(exchange2.getResponse().getStatusCode()).isNull();
        assertThat(exchange3.getResponse().getStatusCode()).isNull();
        assertThat(exchange4.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("사용자별 Rate Limiting - 인증된 사용자는 더 높은 제한")
    void userBasedRateLimiting_AuthenticatedUser_ShouldHaveHigherLimit() {
        // Given: Rate Limit 설정
        // User RateLimiter를 미리 생성하여 Registry에 등록
        io.github.resilience4j.ratelimiter.RateLimiterConfig userRateLimiterConfig = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        
        rateLimiterRegistry.rateLimiter("user-rate-limiter", userRateLimiterConfig);
        
        RateLimitingFilter.Config config = new RateLimitingFilter.Config();
        config.setLimitForPeriod(2);  // IP: 초당 2개
        config.setUserLimitForPeriod(5);  // User: 초당 5개
        config.setUserBasedRateLimitEnabled(true);

        // JWT 토큰 Mock 설정
        when(jwtTokenValidator.isValid(anyString())).thenReturn(true);
        when(jwtTokenValidator.parseClaims(anyString())).thenReturn(
            io.jsonwebtoken.Jwts.claims().setSubject("testuser").build()
        );

        var filter = rateLimitingFilter.apply(config);

        String clientIp = "192.168.1.100";
        String jwtToken = "valid.jwt.token";

        // When & Then: 인증된 사용자로 5개 요청
        for (int i = 0; i < 5; i++) {
            var exchange = createMockExchange(clientIp, jwtToken);
            filter.filter(exchange, mockChain).block();
            assertThat(exchange.getResponse().getStatusCode())
                .as("Request %d should be allowed", i + 1)
                .isNull();
        }

        // 6번째 요청은 차단
        var exchange6 = createMockExchange(clientIp, jwtToken);
        filter.filter(exchange6, mockChain).block();
        assertThat(exchange6.getResponse().getStatusCode())
            .as("6th request should be rate limited")
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange6.getResponse().getHeaders().getFirst("X-RateLimit-Key")).contains("user:testuser");
    }

    @Test
    @DisplayName("Actuator 엔드포인트는 Rate Limiting 제외")
    void actuatorEndpoint_ShouldBypassRateLimiting() {
        // Given: Rate Limit 설정
        RateLimitingFilter.Config config = new RateLimitingFilter.Config();
        config.setLimitForPeriod(1);  // 초당 1개 (매우 작게 설정)

        var filter = rateLimitingFilter.apply(config);

        // When: Actuator 엔드포인트로 여러 요청
        var exchange1 = createMockExchangeWithPath("192.168.1.100", null, "/actuator/health");
        var exchange2 = createMockExchangeWithPath("192.168.1.100", null, "/actuator/health");
        var exchange3 = createMockExchangeWithPath("192.168.1.100", null, "/actuator/health");

        StepVerifier.create(filter.filter(exchange1, mockChain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange2, mockChain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange3, mockChain)).verifyComplete();

        // Then: 모두 정상 처리 (Rate Limiting 제외)
        assertThat(exchange1.getResponse().getStatusCode()).isNull();
        assertThat(exchange2.getResponse().getStatusCode()).isNull();
        assertThat(exchange3.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더의 IP 주소 사용")
    void xForwardedForHeader_ShouldBeUsedForRateLimiting() {
        // Given: Rate Limit 설정
        // IP RateLimiter를 미리 생성하여 Registry에 등록
        io.github.resilience4j.ratelimiter.RateLimiterConfig rateLimiterConfig = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        
        rateLimiterRegistry.rateLimiter("ip-rate-limiter", rateLimiterConfig);
        
        RateLimitingFilter.Config config = new RateLimitingFilter.Config();
        config.setLimitForPeriod(2);
        config.setUserBasedRateLimitEnabled(false);

        var filter = rateLimitingFilter.apply(config);

        // X-Forwarded-For 헤더로 IP 전달
        String realClientIp = "203.0.113.100";

        // When & Then: 같은 X-Forwarded-For IP로 3개 요청
        // 처음 2개는 허용
        var exchange1 = createMockExchangeWithForwardedFor("127.0.0.1", realClientIp, null);
        filter.filter(exchange1, mockChain).block();
        assertThat(exchange1.getResponse().getStatusCode())
            .as("First request should be allowed")
            .isNull();

        var exchange2 = createMockExchangeWithForwardedFor("127.0.0.1", realClientIp, null);
        filter.filter(exchange2, mockChain).block();
        assertThat(exchange2.getResponse().getStatusCode())
            .as("Second request should be allowed")
            .isNull();

        // 3번째는 차단
        var exchange3 = createMockExchangeWithForwardedFor("127.0.0.1", realClientIp, null);
        filter.filter(exchange3, mockChain).block();
        assertThat(exchange3.getResponse().getStatusCode())
            .as("Third request should be rate limited")
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange3.getResponse().getHeaders().getFirst("X-RateLimit-Key")).contains("ip:203.0.113.100");
    }

    @Test
    @DisplayName("Rate Limiting 에러 발생 시 fail-open (요청 허용)")
    void rateLimitingError_ShouldFailOpen() {
        // Given: JwtTokenValidator가 예외를 던지도록 설정
        when(jwtTokenValidator.isValid(anyString())).thenThrow(new RuntimeException("JWT validation failed"));

        RateLimitingFilter.Config config = new RateLimitingFilter.Config();
        var filter = rateLimitingFilter.apply(config);

        // When: 요청 처리
        var exchange = createMockExchange("192.168.1.100", "invalid.token");
        StepVerifier.create(filter.filter(exchange, mockChain)).verifyComplete();

        // Then: 에러가 발생해도 요청은 허용 (fail-open)
        // (실제로는 mockChain.filter()가 호출되어야 함)
        verify(mockChain, atLeastOnce()).filter(any(ServerWebExchange.class));
    }

    // ===== 헬퍼 메서드 =====

    private MockServerWebExchange createMockExchange(String clientIp, String jwtToken) {
        return createMockExchangeWithPath(clientIp, jwtToken, "/api/test");
    }

    private MockServerWebExchange createMockExchangeWithPath(String clientIp, String jwtToken, String path) {
        var requestBuilder = MockServerHttpRequest.get(path)
            .remoteAddress(new InetSocketAddress(clientIp, 8080));

        if (jwtToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
        }

        return MockServerWebExchange.from(requestBuilder.build());
    }

    private MockServerWebExchange createMockExchangeWithForwardedFor(String remoteIp, String forwardedFor, String jwtToken) {
        var requestBuilder = MockServerHttpRequest.get("/api/test")
            .remoteAddress(new InetSocketAddress(remoteIp, 8080))
            .header("X-Forwarded-For", forwardedFor);

        if (jwtToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
        }

        return MockServerWebExchange.from(requestBuilder.build());
    }
}

