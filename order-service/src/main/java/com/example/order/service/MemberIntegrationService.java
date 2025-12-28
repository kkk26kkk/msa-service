package com.example.order.service;

import com.example.order.client.MemberServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Member Service 통합 서비스
 * 
 * Member Service와의 통신을 담당하며, Circuit Breaker 및 Fallback 처리를 수행합니다.
 * 
 * 별도 서비스로 분리한 이유:
 * - Spring AOP의 @CircuitBreaker 어노테이션이 작동하려면 프록시를 통한 호출이 필요합니다.
 * - 같은 클래스 내에서 직접 호출하면 프록시를 통하지 않아 Circuit Breaker가 작동하지 않습니다.
 * - 별도 서비스로 분리하면 순환 참조 문제 없이 프록시를 통한 호출이 보장됩니다.
 */
@Service
public class MemberIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(MemberIntegrationService.class);
    
    private final MemberServiceClient memberServiceClient;

    public MemberIntegrationService(MemberServiceClient memberServiceClient) {
        this.memberServiceClient = memberServiceClient;
    }

    /**
     * 회원 정보 검증 (Retry 적용)
     * 
     * Resilience4j의 @Retry 어노테이션을 사용합니다.
     * - 일시적인 네트워크 오류 시 자동 재시도 (최대 3회, 지수 백오프)
     * - Circuit Breaker는 별도로 추가 가능
     * 
     * @param memberId 회원 ID
     * @return 회원 정보 DTO
     */
    @Retry(name = "member-service")
    public MemberServiceClient.MemberDto validateMember(Long memberId) {
        log.debug("Validating member with ID: {}", memberId);
        return memberServiceClient.getMemberById(memberId);
    }

    /**
     * 회원명 조회 (Retry 적용)
     * 
     * Resilience4j의 @Retry 어노테이션을 사용합니다.
     * 
     * @param memberId 회원 ID
     * @return 회원명
     */
    @Retry(name = "member-service")
    public String getMemberName(Long memberId) {
        log.debug("Getting member name for ID: {}", memberId);
        MemberServiceClient.MemberDto member = memberServiceClient.getMemberById(memberId);
        return member.getFullName();
    }
}

