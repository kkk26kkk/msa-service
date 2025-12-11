package com.example.auth.config;

import com.example.auth.security.SecurityRoles;
import com.example.auth.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 애플리케이션 시작 시 초기 데이터를 생성하는 설정 클래스
 * 
 * 주요 기능:
 * - 기본 사용자 계정 생성 (admin, member)
 * 
 * 생성되는 사용자:
 * - admin: 관리자 권한 (ADMIN)
 * - member: 일반 사용자 권한 (USER)
 * 
 * 비밀번호: password123 (모든 사용자 동일)
 * 
 * 주의: 프로덕션 환경에서는 이 초기화를 비활성화하거나
 *       더 안전한 비밀번호 정책을 적용해야 합니다.
 */
@Configuration
public class DataInitializer {

    /**
     * 애플리케이션 시작 시 실행되는 CommandLineRunner 빈
     * 
     * 실행 시점: 모든 빈이 생성된 후, 애플리케이션이 완전히 시작되기 전
     * 
     * @param authService 사용자 등록 서비스
     * @return CommandLineRunner 인스턴스
     */
    @Bean
    CommandLineRunner initializeDefaultUser(AuthService authService) {
        return args -> {
            // 관리자 계정 생성 (역할: ADMIN)
            authService.registerUser("admin", "password123", SecurityRoles.ADMIN);
            
            // 일반 사용자 계정 생성 (역할: USER)
            authService.registerUser("member", "password123", SecurityRoles.USER);
        };
    }
}
