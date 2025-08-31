package com.example.order.controller;

import com.example.order.client.MemberServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 테스트 컨트롤러
 * 
 * OpenFeign과 Circuit Breaker 동작을 테스트하기 위한 컨트롤러입니다.
 */
@RestController
@RequestMapping("/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    private final MemberServiceClient memberServiceClient;

    public TestController(MemberServiceClient memberServiceClient) {
        this.memberServiceClient = memberServiceClient;
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
     */
    @GetMapping("/circuit-breaker-status")
    public ResponseEntity<Map<String, String>> getCircuitBreakerStatus() {
        log.info("Checking Circuit Breaker status");
        
        // 여러 번 호출해서 Circuit Breaker 상태 확인
        String status = "UNKNOWN";
        
        try {
            memberServiceClient.getMemberById(1L);
            status = "CLOSED"; // 정상 상태
        } catch (Exception e) {
            if (e.getMessage().contains("circuit breaker is open") || 
                e.getMessage().contains("circuit breaker is half open")) {
                status = "OPEN"; // Circuit Breaker 열림
            } else {
                status = "FALLBACK"; // Fallback 동작
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "circuitBreakerStatus", status,
            "service", "member-service"
        ));
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


