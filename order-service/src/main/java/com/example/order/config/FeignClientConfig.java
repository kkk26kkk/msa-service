package com.example.order.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * OpenFeign 클라이언트 설정 클래스
 * 
 * 주요 기능:
 * - OpenFeign을 통한 서비스 간 통신 시 JWT 토큰 전달
 * 
 * 동작 원리:
 * - Order Service가 Member Service를 호출할 때 (OpenFeign 사용)
 * - 현재 요청의 Authorization 헤더에 있는 JWT 토큰을 자동으로 추출
 * - 추출한 JWT 토큰을 OpenFeign 요청의 Authorization 헤더에 추가
 * - Member Service는 전달받은 JWT 토큰을 검증하여 인증 수행
 * 
 * 사용 시나리오:
 * 1. 클라이언트 → Gateway (JWT 토큰 포함)
 * 2. Gateway → Order Service (JWT 토큰 포함)
 * 3. Order Service → Member Service (OpenFeign, JWT 토큰 자동 전달)
 * 4. Member Service는 JWT 토큰을 검증하여 인증 수행
 */
@Configuration
public class FeignClientConfig {

    /**
     * OpenFeign 요청 인터셉터 빈 등록
     * 
     * 이 인터셉터는 OpenFeign을 사용한 모든 HTTP 요청 전에 실행됩니다.
     * 현재 요청의 Authorization 헤더를 OpenFeign 요청에 자동으로 추가합니다.
     * 
     * 동작 과정:
     * 1. RequestContextHolder에서 현재 요청 정보 추출
     * 2. 현재 요청의 Authorization 헤더 확인
     * 3. Authorization 헤더가 있으면 OpenFeign 요청에 추가
     * 
     * @return RequestInterceptor 인스턴스
     */
    @Bean
    public RequestInterceptor authorizationHeaderInterceptor() {
        return template -> {
            // 현재 요청의 RequestAttributes 추출
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            
            // ServletRequestAttributes로 변환 가능한지 확인
            if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
                // 현재 요청의 Authorization 헤더 추출
                String authorization = servletRequestAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                
                // Authorization 헤더가 있으면 OpenFeign 요청에 추가
                // 이를 통해 Order Service → Member Service 호출 시
                // JWT 토큰이 자동으로 전달됩니다.
                if (StringUtils.hasText(authorization)) {
                    template.header(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
        };
    }
}
