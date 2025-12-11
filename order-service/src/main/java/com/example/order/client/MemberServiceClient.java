package com.example.order.client;

import com.example.order.config.FeignClientConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Member Service 연동을 위한 OpenFeign 클라이언트
 * 
 * Member Service의 API를 호출하여 회원 정보를 조회합니다.
 */
@FeignClient(
    configuration = FeignClientConfig.class,
    name = "member-service",
    url = "${member-service.url:http://localhost:8081}"
    // 주의: OpenFeign Fallback을 사용하지 않습니다.
    // 대신 Resilience4j의 @CircuitBreaker 어노테이션을 Service 레벨에서 사용하여
    // Circuit Breaker가 실패를 올바르게 카운트하고 Fallback을 처리합니다.
)
public interface MemberServiceClient {

    /**
     * 회원 ID로 회원 정보 조회
     */
    @GetMapping("/members/{id}")
    MemberDto getMemberById(@PathVariable("id") Long id);

    /**
     * 사용자명으로 회원 정보 조회
     */
    @GetMapping("/members/username/{username}")
    MemberDto getMemberByUsername(@PathVariable("username") String username);

    /**
     * 회원 서비스 헬스 체크
     *
     */
    @GetMapping("/members/health")
    HealthCheckResponse getHealthCheck();

    /**
     * Member Service 응답을 위한 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    class MemberDto {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String phoneNumber;
        private String status;
        private String statusDescription;
    }

    /**
     * 헬스 체크 응답을 위한 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    class HealthCheckResponse {
        private String status;
        private String service;
    }
}
