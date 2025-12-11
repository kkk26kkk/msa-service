package com.example.order.service;

import com.example.order.client.MemberServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
     * 회원 정보 검증 (Circuit Breaker 및 Fallback 적용)
     * 
     * Resilience4j의 @CircuitBreaker 어노테이션을 사용하여 Circuit Breaker 패턴을 적용합니다.
     * Member Service 장애 시 자동으로 Fallback 메서드가 실행됩니다.
     * 
     * @param memberId 회원 ID
     * @return 회원 정보 DTO
     */
    @CircuitBreaker(name = "member-service", fallbackMethod = "validateMemberFallback")
    public MemberServiceClient.MemberDto validateMember(Long memberId) {
        log.debug("Validating member with ID: {}", memberId);
        return memberServiceClient.getMemberById(memberId);
    }

    /**
     * 회원 정보 검증 Fallback 메서드
     * 
     * Member Service 장애 시 @CircuitBreaker 어노테이션에 의해 자동으로 실행되는 Fallback 메서드입니다.
     * Circuit Breaker가 실패를 카운트하고, Fallback 데이터를 반환하여 주문 처리를 계속 진행합니다.
     * 
     * @param memberId 회원 ID
     * @param e 발생한 예외
     * @return Fallback 회원 정보 DTO
     * @see #validateMember(Long)
     */
    @SuppressWarnings("unused") // @CircuitBreaker 어노테이션에 의해 리플렉션으로 호출됨
    public MemberServiceClient.MemberDto validateMemberFallback(Long memberId, Exception e) {
        log.error("Member Service unavailable. Using fallback for memberId: {}", memberId, e);
        
        return new MemberServiceClient.MemberDto(
            memberId,
            "unknown-user-" + memberId,
            "unknown@example.com",
            "알 수 없는 사용자",
            "000-0000-0000",
            "UNKNOWN",
            "서비스 일시 중단"
        );
    }

    /**
     * 회원명 조회 (Circuit Breaker 및 Fallback 적용)
     * 
     * Resilience4j의 @CircuitBreaker 어노테이션을 사용하여 Circuit Breaker 패턴을 적용합니다.
     * Member Service 장애 시 자동으로 Fallback 메서드가 실행됩니다.
     * 
     * @param memberId 회원 ID
     * @return 회원명
     */
    @CircuitBreaker(name = "member-service", fallbackMethod = "getMemberNameFallback")
    public String getMemberName(Long memberId) {
        log.debug("Getting member name for ID: {}", memberId);
        MemberServiceClient.MemberDto member = memberServiceClient.getMemberById(memberId);
        return member.getFullName();
    }

    /**
     * 회원명 조회 Fallback 메서드
     * 
     * Member Service 장애 시 @CircuitBreaker 어노테이션에 의해 자동으로 실행되는 Fallback 메서드입니다.
     * 
     * @param memberId 회원 ID
     * @param e 발생한 예외
     * @return Fallback 회원명
     * @see #getMemberName(Long)
     */
    @SuppressWarnings("unused") // @CircuitBreaker 어노테이션에 의해 리플렉션으로 호출됨
    public String getMemberNameFallback(Long memberId, Exception e) {
        log.warn("Member Service unavailable. Using fallback for member name, memberId: {}", memberId, e);
        return "알 수 없는 사용자";
    }
}

