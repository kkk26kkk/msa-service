package com.example.auth.service;

import com.example.auth.entity.AuthUser;
import com.example.auth.repository.AuthUserRepository;
import com.example.auth.security.SecurityRoles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 사용자 등록 및 관리 서비스
 * 
 * 주요 기능:
 * - 새 사용자 등록 (비밀번호 암호화, 역할 설정)
 * - 기존 사용자 확인 및 반환
 * 
 * 사용 위치:
 * - DataInitializer: 애플리케이션 시작 시 기본 사용자 생성
 */
@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 새 사용자를 등록하거나 기존 사용자를 반환
     * 
     * 처리 과정:
     * 1. 사용자명 중복 확인
     * 2. 역할 문자열 정규화 (대문자 변환, "ROLE_" 접두사 제거, 중복 제거)
     * 3. 비밀번호 암호화 (BCrypt)
     * 4. 데이터베이스에 저장
     * 
     * 역할 처리 규칙:
     * - 역할이 지정되지 않으면 기본값 "USER" 사용
     * - "ROLE_" 접두사가 있으면 제거 후 저장 (저장 시에는 접두사 없이 저장)
     * - 대문자로 변환하여 저장
     * - 중복 역할 제거
     * 
     * 예시:
     * - 입력: roles = ["ADMIN", "ROLE_USER"] -> 저장: "ADMIN,USER"
     * - 입력: roles = null -> 저장: "USER"
     * 
     * @param username 사용자명
     * @param password 평문 비밀번호 (저장 시 자동 암호화됨)
     * @param roles 사용자 역할 배열 (예: ["ADMIN", "USER"])
     * @return 등록된 또는 기존 AuthUser 객체
     */
    @Transactional
    public AuthUser registerUser(String username, String password, String... roles) {
        // 기존 사용자가 있으면 반환 (중복 등록 방지)
        if (authUserRepository.existsByUsername(username)) {
            return authUserRepository.findByUsername(username).orElseThrow();
        }
        
        // 역할 문자열 정규화
        String roleValue = (roles == null || roles.length == 0)
                ? SecurityRoles.USER  // 역할이 없으면 기본값 "USER"
                : Arrays.stream(roles)
                    .filter(StringUtils::hasText)                    // 빈 문자열 제거
                    .map(String::trim)                                // 공백 제거
                    .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)  // "ROLE_" 접두사 제거
                    .map(String::toUpperCase)                         // 대문자 변환
                    .distinct()                                        // 중복 제거
                    .collect(Collectors.joining(","));                // 쉼표로 연결
                    
        // 역할이 비어있으면 기본값 사용
        if (!StringUtils.hasText(roleValue)) {
            roleValue = SecurityRoles.USER;
        }
        
        // AuthUser 객체 생성 및 저장
        AuthUser user = AuthUser.builder()
                .username(username)
                .password(passwordEncoder.encode(password))  // 비밀번호 암호화 (BCrypt)
                .roles(roleValue)                           // 정규화된 역할 문자열
                .build();
        return authUserRepository.save(user);
    }
}
