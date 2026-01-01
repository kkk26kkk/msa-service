package com.example.gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * RateLimiter 설정
 * 
 * Resilience4j RateLimiter의 설정은 application.yml에서 관리됩니다.
 * Spring Boot의 자동 설정이 RateLimiterRegistry를 자동으로 생성합니다.
 * 
 * 설정 파일 위치:
 * - config-service/src/main/resources/config-repo/gateway-service.yml
 * 
 * 설정 항목:
 * - resilience4j.ratelimiter.configs: RateLimiter 기본 설정
 * - resilience4j.ratelimiter.instances: RateLimiter 인스턴스 설정
 */
@Configuration
public class RateLimiterConfig {
    // Spring Boot의 자동 설정을 사용하므로 별도 빈 등록 불필요
}

