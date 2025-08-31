package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Member Service 연동을 위한 OpenFeign 클라이언트
 * 
 * Member Service의 API를 호출하여 회원 정보를 조회합니다.
 */
@FeignClient(
    name = "member-service",
    url = "${member-service.url:http://localhost:8081}",
    fallback = MemberServiceClientFallback.class
)
public interface MemberServiceClient {

    /**
     * 회원 ID로 회원 정보 조회
     */
    @GetMapping("/members/{id}")
    MemberDto getMemberById(@PathVariable("id") Long id);

    /**
     * 사용자명으로 회원 정보 조회
     */
    @GetMapping("/members/username/{username}")
    MemberDto getMemberByUsername(@PathVariable("username") String username);

    /**
     * 회원 서비스 헬스 체크
     */
    @GetMapping("/members/health")
    HealthCheckResponse getHealthCheck();

    /**
     * Member Service 응답을 위한 DTO
     */
    class MemberDto {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String phoneNumber;
        private String status;
        private String statusDescription;

        // 기본 생성자
        public MemberDto() {}

        // 전체 생성자
        public MemberDto(Long id, String username, String email, String fullName, 
                        String phoneNumber, String status, String statusDescription) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.phoneNumber = phoneNumber;
            this.status = status;
            this.statusDescription = statusDescription;
        }

        // Getter/Setter
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getStatusDescription() { return statusDescription; }
        public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }

        @Override
        public String toString() {
            return "MemberDto{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", fullName='" + fullName + '\'' +
                    ", phoneNumber='" + phoneNumber + '\'' +
                    ", status='" + status + '\'' +
                    ", statusDescription='" + statusDescription + '\'' +
                    '}';
        }
    }

    /**
     * 헬스 체크 응답을 위한 DTO
     */
    class HealthCheckResponse {
        private String status;
        private String service;

        // 기본 생성자
        public HealthCheckResponse() {}

        // 전체 생성자
        public HealthCheckResponse(String status, String service) {
            this.status = status;
            this.service = service;
        }

        // Getter/Setter
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getService() { return service; }
        public void setService(String service) { this.service = service; }

        @Override
        public String toString() {
            return "HealthCheckResponse{" +
                    "status='" + status + '\'' +
                    ", service='" + service + '\'' +
                    '}';
        }
    }
}
