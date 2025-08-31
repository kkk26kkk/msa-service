package com.example.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Discovery Service
 * 
 * 마이크로서비스들의 서비스 디스커버리를 담당하는 Eureka 서버입니다.
 * 모든 마이크로서비스들이 이 서버에 등록되고, 서로를 찾을 수 있게 해줍니다.
 * 
 * 접속 URL: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}


