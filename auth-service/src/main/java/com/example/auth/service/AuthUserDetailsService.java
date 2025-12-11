package com.example.auth.service;

import com.example.auth.repository.AuthUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security의 UserDetailsService 구현 클래스
 * 
 * 역할:
 * - 사용자 인증 시 데이터베이스에서 사용자 정보를 조회
 * - 조회한 사용자 정보를 Spring Security의 UserDetails 객체로 변환
 * 
 * 동작 흐름:
 * 1. AuthenticationManager.authenticate() 호출 시 내부적으로 이 서비스가 호출됨
 * 2. loadUserByUsername() 메서드가 데이터베이스에서 사용자 조회
 * 3. 조회한 사용자 정보를 UserDetails 객체로 변환하여 반환
 * 4. Spring Security가 반환된 비밀번호와 입력된 비밀번호를 비교하여 인증 수행
 */
@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    public AuthUserDetailsService(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    /**
     * 사용자명으로 사용자 정보를 조회하여 UserDetails 객체로 반환
     * 
     * 이 메서드는 AuthenticationManager.authenticate() 호출 시
     * 내부적으로 자동으로 호출됩니다.
     * 
     * @param username 조회할 사용자명
     * @return Spring Security의 UserDetails 객체 (사용자명, 암호화된 비밀번호, 권한 목록 포함)
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return authUserRepository.findByUsername(username)
                .map(user -> new User(
                        user.getUsername(),              // 사용자명
                        user.getPassword(),              // 암호화된 비밀번호 (BCrypt)
                        mapRoles(user.getRoles())        // 권한(역할) 목록
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * 데이터베이스에 저장된 역할 문자열을 Spring Security의 GrantedAuthority 리스트로 변환
     * 
     * 역할 문자열 형식: "ADMIN,USER" 또는 "ROLE_ADMIN,ROLE_USER"
     * 
     * 변환 규칙:
     * - 쉼표로 구분된 역할 문자열을 분리
     * - "ROLE_" 접두사가 없으면 자동으로 추가
     * - 각 역할을 SimpleGrantedAuthority 객체로 변환
     * 
     * 예시:
     * - 입력: "ADMIN,USER" -> 출력: [ROLE_ADMIN, ROLE_USER]
     * - 입력: "ROLE_ADMIN,ROLE_USER" -> 출력: [ROLE_ADMIN, ROLE_USER]
     * 
     * @param roles 데이터베이스에 저장된 역할 문자열 (예: "ADMIN,USER")
     * @return SimpleGrantedAuthority 리스트
     */
    private List<SimpleGrantedAuthority> mapRoles(String roles) {
        return Arrays.stream(roles.split(","))
                .map(String::trim)                      // 공백 제거
                .filter(role -> !role.isEmpty())        // 빈 문자열 제거
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)  // "ROLE_" 접두사 추가
                .map(SimpleGrantedAuthority::new)        // SimpleGrantedAuthority 객체 생성
                .collect(Collectors.toList());
    }
}
