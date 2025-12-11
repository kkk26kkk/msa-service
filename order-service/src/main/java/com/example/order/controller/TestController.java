package com.example.order.controller;

import com.example.order.client.MemberServiceClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 테스트 컨트롤러
 * 
 * OpenFeign과 Circuit Breaker 동작을 테스트하기 위한 컨트롤러입니다.
 */
@RestController
@RequestMapping("/test")
@PreAuthorize("hasRole(T(com.example.order.security.SecurityRoles).ADMIN)")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    private final MemberServiceClient memberServiceClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public TestController(MemberServiceClient memberServiceClient, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.memberServiceClient = memberServiceClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Member Service 직접 호출 테스트
     */
    @GetMapping("/member/{id}")
    public ResponseEntity<MemberServiceClient.MemberDto> testMemberService(@PathVariable Long id) {
        log.info("Testing direct call to Member Service for ID: {}", id);
        
        try {
            MemberServiceClient.MemberDto member = memberServiceClient.getMemberById(id);
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            log.error("Error calling Member Service", e);
            throw e;
        }
    }

    /**
     * Member Service 연결 상태 확인
     */
    @GetMapping("/member-health")
    public ResponseEntity<Map<String, Object>> testMemberServiceHealth() {
        log.info("Testing Member Service health check");
        
        try {
            MemberServiceClient.HealthCheckResponse health = memberServiceClient.getHealthCheck();
            return ResponseEntity.ok(Map.of(
                "memberServiceStatus", health.getStatus(),
                "memberServiceName", health.getService(),
                "connectionStatus", "SUCCESS"
            ));
        } catch (Exception e) {
            log.error("Member Service health check failed", e);
            return ResponseEntity.ok(Map.of(
                "memberServiceStatus", "DOWN",
                "memberServiceName", "member-service",
                "connectionStatus", "FAILED",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Circuit Breaker 상태 확인
     * 
     * Resilience4j의 CircuitBreakerRegistry를 사용하여 실제 Circuit Breaker 상태를 확인합니다.
     * 
     * Circuit Breaker 상태:
     * - CLOSED: 정상 상태 (요청이 통과됨)
     * - OPEN: Circuit Breaker가 열림 (요청이 차단되고 Fallback 실행)
     * - HALF_OPEN: Circuit Breaker가 반열림 상태 (테스트 요청 허용)
     * - DISABLED: Circuit Breaker가 비활성화됨
     * 
     * @return Circuit Breaker 상태 정보
     */
    @GetMapping("/circuit-breaker-status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        log.info("Checking Circuit Breaker status");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // CircuitBreakerRegistry에서 "member-service" Circuit Breaker 인스턴스 조회
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("member-service");
            
            if (circuitBreaker != null) {
                // Circuit Breaker 상태 정보 수집
                CircuitBreaker.State state = circuitBreaker.getState();
                CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
                
                result.put("circuitBreakerStatus", state.name()); // CLOSED, OPEN, HALF_OPEN, DISABLED
                result.put("service", "member-service");
                result.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
                result.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
                result.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
                result.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
                result.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());
                
                // 상태별 설명 추가
                String statusDescription = switch (state) {
                    case CLOSED -> "정상 상태 - 모든 요청이 통과됩니다";
                    case OPEN -> "Circuit Breaker 열림 - 요청이 차단되고 Fallback이 실행됩니다";
                    case HALF_OPEN -> "반열림 상태 - 제한된 요청만 허용하여 서비스 복구를 테스트합니다";
                    case DISABLED -> "비활성화됨 - Circuit Breaker가 작동하지 않습니다";
                    default -> "알 수 없는 상태: " + state;
                };
                result.put("statusDescription", statusDescription);
                
                log.info("Circuit Breaker state: {}, failure rate: {}%", state, metrics.getFailureRate());
            } else {
                result.put("circuitBreakerStatus", "NOT_FOUND");
                result.put("service", "member-service");
                result.put("message", "Circuit Breaker 인스턴스를 찾을 수 없습니다");
            }
        } catch (Exception e) {
            log.error("Error checking Circuit Breaker status", e);
            result.put("circuitBreakerStatus", "ERROR");
            result.put("service", "member-service");
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Fallback 강제 테스트
     */
    @PostMapping("/force-fallback")
    public ResponseEntity<Map<String, Object>> forceFallbackTest() {
        log.info("Force testing fallback mechanism");
        
        // 존재하지 않는 회원 ID로 여러 번 호출하여 Circuit Breaker 동작 유도
        for (int i = 0; i < 6; i++) {
            try {
                memberServiceClient.getMemberById(999L);
            } catch (Exception e) {
                log.debug("Fallback test call {}: {}", i + 1, e.getMessage());
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Fallback test completed",
            "note", "Check logs for fallback behavior"
        ));
    }
}


