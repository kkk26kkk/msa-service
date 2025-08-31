package com.example.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Member Service 연동 실패 시 Fallback 처리
 * 
 * Circuit Breaker 패턴을 통해 Member Service 장애 시 대체 응답을 제공합니다.
 */
@Component
public class MemberServiceClientFallback implements MemberServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceClientFallback.class);

    @Override
    public MemberDto getMemberById(Long id) {
        log.warn("Member Service is unavailable. Using fallback for getMemberById: {}", id);
        
        return new MemberDto(
            id,
            "unknown-user",
            "unknown@example.com",
            "알 수 없는 사용자",
            "000-0000-0000",
            "UNKNOWN",
            "서비스 일시 중단"
        );
    }

    @Override
    public MemberDto getMemberByUsername(String username) {
        log.warn("Member Service is unavailable. Using fallback for getMemberByUsername: {}", username);
        
        return new MemberDto(
            -1L,
            username,
            "unknown@example.com",
            "알 수 없는 사용자",
            "000-0000-0000",
            "UNKNOWN",
            "서비스 일시 중단"
        );
    }

    @Override
    public HealthCheckResponse getHealthCheck() {
        log.warn("Member Service is unavailable. Using fallback for health check");
        
        return new HealthCheckResponse("DOWN", "member-service-fallback");
    }
}


