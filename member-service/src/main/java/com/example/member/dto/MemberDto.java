package com.example.member.dto;

import com.example.member.entity.Member;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회원 DTO 클래스들
 * 
 * API 요청/응답을 위한 데이터 전송 객체들입니다.
 */
public class MemberDto {

    /**
     * 회원 생성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        
        @NotBlank(message = "사용자명은 필수입니다")
        @Size(min = 3, max = 50, message = "사용자명은 3-50자 사이여야 합니다")
        private String username;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다")
        private String password;

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
        private String fullName;

        @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
        private String phoneNumber;

        /**
         * DTO를 Entity로 변환
         */
        public Member toEntity() {
            return Member.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .fullName(fullName)
                    .phoneNumber(phoneNumber)
                    .status(Member.MemberStatus.ACTIVE)
                    .build();
        }
    }

    /**
     * 회원 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        
        @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
        private String fullName;

        @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
        private String phoneNumber;

        private Member.MemberStatus status;
    }

    /**
     * 회원 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String phoneNumber;
        private Member.MemberStatus status;
        private String statusDescription;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;

        /**
         * Entity를 DTO로 변환
         */
        public static Response from(Member member) {
            return Response.builder()
                    .id(member.getId())
                    .username(member.getUsername())
                    .email(member.getEmail())
                    .fullName(member.getFullName())
                    .phoneNumber(member.getPhoneNumber())
                    .status(member.getStatus())
                    .statusDescription(member.getStatus().getDescription())
                    .createdAt(member.getCreatedAt())
                    .updatedAt(member.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 회원 요약 DTO (목록 조회용)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private Member.MemberStatus status;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        /**
         * Entity를 Summary DTO로 변환
         */
        public static Summary from(Member member) {
            return Summary.builder()
                    .id(member.getId())
                    .username(member.getUsername())
                    .email(member.getEmail())
                    .fullName(member.getFullName())
                    .status(member.getStatus())
                    .createdAt(member.getCreatedAt())
                    .build();
        }
    }
}


