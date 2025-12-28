package com.example.order.service;

import com.example.order.OrderServiceApplication;
import com.example.order.client.MemberServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * MemberIntegrationService 재시도 메커니즘 테스트
 * 
 * @MockBean을 사용하여 MemberServiceClient를 완전히 Mock으로 대체합니다.
 * 이를 통해 실제 네트워크 호출 없이 Resilience4j 재시도 메커니즘을 테스트합니다.
 * 
 * 참고: 이 테스트는 @Retry 메커니즘만 테스트합니다.
 * Circuit Breaker와 함께 사용하면 Fallback이 재시도를 방해할 수 있습니다.
 */
@SpringBootTest(
    classes = OrderServiceApplication.class,
    properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "eureka.client.enabled=false"
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@DisplayName("MemberIntegrationService 재시도 메커니즘 테스트")
class MemberIntegrationServiceRetryTest {

    private static final Logger log = LoggerFactory.getLogger(MemberIntegrationServiceRetryTest.class);

    @Autowired
    private MemberIntegrationService memberIntegrationService;

    /**
     * MemberServiceClient를 Mock으로 대체
     * 
     * @MockBean은 Spring 컨텍스트의 실제 빈을 Mock으로 완전히 대체합니다.
     * 따라서 OpenFeign이 생성한 실제 Feign 클라이언트가 아닌 이 Mock이 사용됩니다.
     */
    @MockBean
    private MemberServiceClient memberServiceClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        // Mock 리셋
        reset(memberServiceClient);
        
        // Circuit Breaker 리셋 (OPEN 상태를 CLOSED로 전환)
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("member-service");
        circuitBreaker.reset();
        
        log.debug("=== 테스트 초기화 완료: Circuit Breaker 상태 = {} ===", circuitBreaker.getState());
    }

    @Test
    @DisplayName("재시도 성공 - 일시적인 네트워크 오류 후 복구")
    void retry_Success_AfterTemporaryFailure() {
        // Given: 첫 번째 호출은 ConnectException, 두 번째 호출은 성공
        MemberServiceClient.MemberDto successResponse = new MemberServiceClient.MemberDto(
            1L, "testuser", "test@example.com", "테스트 사용자", "010-1234-5678", "ACTIVE", "정상"
        );

        ResourceAccessException exception = new ResourceAccessException(
            "Connection refused", 
            new ConnectException("Connection refused")
        );

        when(memberServiceClient.getMemberById(1L))
            .thenThrow(exception)
            .thenReturn(successResponse);

        // When: 회원 정보 검증 호출
        log.debug("=== 재시도 성공 테스트 시작 ===");
        MemberServiceClient.MemberDto result = memberIntegrationService.validateMember(1L);

        // Then: 재시도 후 성공
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");

        // 재시도가 발생했는지 확인 (첫 번째 실패 + 재시도 1회 = 총 2회 호출)
        verify(memberServiceClient, times(2)).getMemberById(1L);
        log.debug("=== 재시도 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("재시도 실패 - 모든 재시도 실패 후 예외 발생")
    void retry_Failure_AllRetriesFailed() {
        // Given: 모든 호출이 ConnectException 발생 (ResourceAccessException으로 래핑)
        ResourceAccessException exception = new ResourceAccessException(
            "Connection refused", 
            new ConnectException("Connection refused")
        );
        
        when(memberServiceClient.getMemberById(1L))
            .thenThrow(exception);

        // When & Then: 회원 정보 검증 호출 시 예외 발생
        log.debug("=== 재시도 실패 테스트 시작 ===");
        
        try {
            memberIntegrationService.validateMember(1L);
        } catch (ResourceAccessException e) {
            // 모든 재시도 후 예외 발생
            assertThat(e.getMessage()).contains("Connection refused");
        }

        // maxAttempts: 3은 총 시도 횟수 (초기 시도 1회 + 재시도 2회 = 총 3회)
        verify(memberServiceClient, times(3)).getMemberById(1L);
        log.debug("=== 재시도 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("재시도 무시 - 4xx 클라이언트 오류는 재시도하지 않음")
    void retry_Ignored_ClientError_NoRetry() {
        // Given: 4xx 클라이언트 오류 발생
        // HttpClientErrorException.NotFound는 생성자가 private이므로
        // HttpClientErrorException을 사용하여 404 오류를 시뮬레이션
        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.NOT_FOUND;
        org.springframework.web.client.HttpClientErrorException notFoundException =
            org.springframework.web.client.HttpClientErrorException.create(
                status,
                "Member not found",
                org.springframework.http.HttpHeaders.EMPTY,
                new byte[0],
                java.nio.charset.StandardCharsets.UTF_8
            );

        when(memberServiceClient.getMemberById(999L))
            .thenThrow(notFoundException);

        // When: 회원 정보 검증 호출
        log.debug("=== 재시도 무시 테스트 시작 ===");
        try {
            memberIntegrationService.validateMember(999L);
        } catch (Exception e) {
            // Then: 재시도 없이 즉시 예외 발생
            assertThat(e).isInstanceOf(org.springframework.web.client.HttpClientErrorException.class);
            org.springframework.web.client.HttpClientErrorException httpException = 
                (org.springframework.web.client.HttpClientErrorException) e;
            assertThat(httpException.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
            
            // 재시도 없이 1회만 호출
            verify(memberServiceClient, times(1)).getMemberById(999L);
            log.debug("=== 재시도 무시 테스트 완료 ===");
        }
    }

    @Test
    @DisplayName("재시도 - SocketTimeoutException 발생 시 재시도")
    void retry_OnSocketTimeout() {
        // Given: SocketTimeoutException 발생 (ResourceAccessException으로 래핑, 재시도 대상 예외)
        MemberServiceClient.MemberDto successResponse = new MemberServiceClient.MemberDto(
            1L, "testuser", "test@example.com", "테스트 사용자", "010-1234-5678", "ACTIVE", "정상"
        );
        
        ResourceAccessException exception = new ResourceAccessException(
            "Read timed out", 
            new SocketTimeoutException("Read timed out")
        );

        when(memberServiceClient.getMemberById(1L))
            .thenThrow(exception)
            .thenReturn(successResponse);

        // When: 회원 정보 검증 호출
        log.debug("=== SocketTimeoutException 재시도 테스트 시작 ===");
        MemberServiceClient.MemberDto result = memberIntegrationService.validateMember(1L);

        // Then: 재시도 후 성공
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        // 재시도가 발생했는지 확인 (최소 2회 호출)
        verify(memberServiceClient, atLeast(2)).getMemberById(1L);
        log.debug("=== SocketTimeoutException 재시도 테스트 완료 ===");
    }

    @Test
    @DisplayName("회원명 조회 - 재시도 동작 확인")
    void getMemberName_RetryBehavior() {
        // Given: 첫 번째 호출은 실패, 두 번째 호출은 성공
        MemberServiceClient.MemberDto successResponse = new MemberServiceClient.MemberDto(
            1L, "testuser", "test@example.com", "테스트 사용자", "010-1234-5678", "ACTIVE", "정상"
        );

        ResourceAccessException exception = new ResourceAccessException(
            "Connection refused", 
            new ConnectException("Connection refused")
        );

        when(memberServiceClient.getMemberById(1L))
            .thenThrow(exception)
            .thenReturn(successResponse);

        // When: 회원명 조회 호출
        log.debug("=== 회원명 조회 재시도 테스트 시작 ===");
        String memberName = memberIntegrationService.getMemberName(1L);

        // Then: 재시도 후 성공
        assertThat(memberName).isEqualTo("테스트 사용자");

        // 재시도가 발생했는지 확인
        verify(memberServiceClient, times(2)).getMemberById(1L);
        log.debug("=== 회원명 조회 재시도 테스트 완료 ===");
    }
}

