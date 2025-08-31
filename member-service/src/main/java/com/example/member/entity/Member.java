package com.example.member.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
// Lombok 대신 직접 구현
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 회원 엔터티
 * 
 * 회원의 기본 정보를 관리하는 JPA 엔터티입니다.
 */
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 50, message = "사용자명은 3-50자 사이여야 합니다")
    private String username;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다")
    private String password;

    @Column(unique = true, nullable = false, length = 100)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Column(length = 100)
    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
    private String fullName;

    @Column(length = 20)
    @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 기본 생성자
    public Member() {}

    // 전체 생성자
    public Member(Long id, String username, String password, String email, String fullName, 
                  String phoneNumber, MemberStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.status = status != null ? status : MemberStatus.ACTIVE;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Builder 패턴을 위한 정적 메서드
    public static MemberBuilder builder() {
        return new MemberBuilder();
    }

    // Getter 메서드들
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public MemberStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setter 메서드들
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setStatus(MemberStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // Builder 클래스
    public static class MemberBuilder {
        private Long id;
        private String username;
        private String password;
        private String email;
        private String fullName;
        private String phoneNumber;
        private MemberStatus status = MemberStatus.ACTIVE;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public MemberBuilder id(Long id) { this.id = id; return this; }
        public MemberBuilder username(String username) { this.username = username; return this; }
        public MemberBuilder password(String password) { this.password = password; return this; }
        public MemberBuilder email(String email) { this.email = email; return this; }
        public MemberBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public MemberBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public MemberBuilder status(MemberStatus status) { this.status = status; return this; }
        public MemberBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public MemberBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Member build() {
            return new Member(id, username, password, email, fullName, phoneNumber, status, createdAt, updatedAt);
        }
    }

    /**
     * 회원 상태 열거형
     */
    public enum MemberStatus {
        ACTIVE("활성"),
        INACTIVE("비활성"),
        SUSPENDED("정지");

        private final String description;

        MemberStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
