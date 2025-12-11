package com.example.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Circuit Breaker Fallback Controller
 * 
 * 백엔드 서비스가 응답하지 않을 때 실행되는 Fallback 핸들러입니다.
 */
@Slf4j
@RestController
public class FallbackController {

    @GetMapping("/fallback/member-service")
    public ResponseEntity<Map<String, Object>> memberServiceFallback() {
        log.warn("Member Service fallback triggered - service is unavailable");
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Member Service is currently unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "member-service");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/fallback/order-service")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        log.warn("Order Service fallback triggered - service is unavailable");
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Order Service is currently unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "order-service");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/fallback/auth-service")
    public ResponseEntity<Map<String, Object>> authServiceFallback() {
        log.warn("Auth Service fallback triggered - service is unavailable");
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Auth Service is currently unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "auth-service");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("Gateway health check requested");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "gateway-service");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
