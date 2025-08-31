package com.example.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Member Service Application
 * 
 * 회원 관리를 담당하는 마이크로서비스입니다.
 * 
 * 주요 기능:
 * - 회원 등록, 조회, 수정, 삭제 (CRUD)
 * - Eureka Discovery Service에 등록
 * - Config Server에서 설정 관리
 * - H2 인메모리 데이터베이스 사용
 * 
 * 접속 URL: http://localhost:8081
 * H2 Console: http://localhost:8081/h2-console
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MemberServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberServiceApplication.class, args);
    }
}


