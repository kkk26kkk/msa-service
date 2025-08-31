package com.example.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Configuration Service
 * 
 * 중앙화된 설정 관리를 담당하는 Spring Cloud Config 서버입니다.
 * 모든 마이크로서비스들의 설정을 중앙에서 관리하고 배포합니다.
 * 
 * 접속 URL: http://localhost:8888
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}


