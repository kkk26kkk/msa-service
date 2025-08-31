package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Order Service Application
 * 
 * 주문 관리를 담당하는 마이크로서비스입니다.
 * 
 * 주요 기능:
 * - 주문 등록, 조회, 수정, 삭제 (CRUD)
 * - OpenFeign을 통한 Member Service 연동
 * - Eureka Discovery Service에 등록
 * - Config Server에서 설정 관리
 * - H2 인메모리 데이터베이스 사용
 * 
 * 접속 URL: http://localhost:8082
 * H2 Console: http://localhost:8082/h2-console
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}


