package com.example.member.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 캐시 설정
 * 
 * Caffeine Cache를 사용한 로컬 캐시 설정입니다.
 * 
 * 캐시 전략:
 * - Cache-Aside 패턴 사용
 * - TTL (Time To Live): 5분
 * - 최대 캐시 크기: 1000개
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine Cache Manager 빈 등록
     * 
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "members",           // 회원 정보 캐시
            "memberByUsername",  // 사용자명으로 조회한 회원 캐시
            "activeMemberCount"  // 활성 회원 수 캐시
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)                    // 최대 캐시 크기: 1000개
            .expireAfterWrite(5, TimeUnit.MINUTES) // TTL: 5분
            .recordStats()                        // 캐시 통계 수집
        );
        
        return cacheManager;
    }
}

