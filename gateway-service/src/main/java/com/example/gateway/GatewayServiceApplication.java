package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Service
 * 
 * 마이크로서비스들의 단일 진입점(Single Entry Point) 역할을 담당합니다.
 * 모든 클라이언트 요청을 받아 적절한 백엔드 서비스로 라우팅합니다.
 * 
 * 주요 기능:
 * - 라우팅 및 로드 밸런싱
 * - CORS 처리
 * - Circuit Breaker 패턴
 * - 요청/응답 로깅
 * 
 * 접속 URL: http://localhost:8080
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}


