package com.example.member.controller;

import com.example.member.dto.MemberDto;
import com.example.member.entity.Member;
import com.example.member.service.MemberService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 회원 REST API 컨트롤러
 * 
 * 회원 관련 HTTP 요청을 처리하는 REST 컨트롤러입니다.
 */
@RestController
@RequestMapping("/members")
public class MemberController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 회원 생성
     * 
     * POST /members
     */
    @PostMapping
    @PreAuthorize("hasRole(T(com.example.member.security.SecurityRoles).ADMIN)")
    public ResponseEntity<MemberDto.Response> createMember(@Valid @RequestBody MemberDto.CreateRequest request) {
        log.info("Creating member request received for username: {}", request.getUsername());
        
        MemberDto.Response response = memberService.createMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 회원 목록 조회 (페이징)
     * 
     * GET /members?page=0&size=10&sort=id,desc
     */
    @GetMapping
    @PreAuthorize("hasAnyRole(T(com.example.member.security.SecurityRoles).ADMIN, T(com.example.member.security.SecurityRoles).USER)")
    public ResponseEntity<Page<MemberDto.Summary>> getMembers(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        log.debug("Get members request received with pagination: {}", pageable);
        
        Page<MemberDto.Summary> members = memberService.getMembers(pageable);
        return ResponseEntity.ok(members);
    }

    /**
     * 모든 회원 조회 (목록)
     * 
     * GET /members/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole(T(com.example.member.security.SecurityRoles).ADMIN, T(com.example.member.security.SecurityRoles).USER)")
    public ResponseEntity<List<MemberDto.Summary>> getAllMembers() {
        log.debug("Get all members request received");
        
        List<MemberDto.Summary> members = memberService.getAllMembers();
        return ResponseEntity.ok(members);
    }

    /**
     * ID로 회원 조회
     * 
     * GET /members/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole(T(com.example.member.security.SecurityRoles).ADMIN, T(com.example.member.security.SecurityRoles).USER)")
    public ResponseEntity<MemberDto.Response> getMemberById(@PathVariable Long id) {
        log.debug("Get member by ID request received: {}", id);
        
        MemberDto.Response member = memberService.getMemberById(id);
        return ResponseEntity.ok(member);
    }

    /**
     * 사용자명으로 회원 조회
     * 
     * GET /members/username/{username}
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasAnyRole(T(com.example.member.security.SecurityRoles).ADMIN, T(com.example.member.security.SecurityRoles).USER)")
    public ResponseEntity<MemberDto.Response> getMemberByUsername(@PathVariable String username) {
        log.debug("Get member by username request received: {}", username);
        
        MemberDto.Response member = memberService.getMemberByUsername(username);
        return ResponseEntity.ok(member);
    }

    /**
     * 상태별 회원 조회
     * 
     * GET /members/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole(T(com.example.member.security.SecurityRoles).ADMIN, T(com.example.member.security.SecurityRoles).USER)")
    public ResponseEntity<List<MemberDto.Summary>> getMembersByStatus(@PathVariable Member.MemberStatus status) {
        log.debug("Get members by status request received: {}", status);
        
        List<MemberDto.Summary> members = memberService.getMembersByStatus(status);
        return ResponseEntity.ok(members);
    }

    /**
     * 이름으로 회원 검색
     * 
     * GET /members/search?name=홍길동
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole(T(com.example.member.security.SecurityRoles).ADMIN, T(com.example.member.security.SecurityRoles).USER)")
    public ResponseEntity<List<MemberDto.Summary>> searchMembersByName(@RequestParam String name) {
        log.debug("Search members by name request received: {}", name);
        
        List<MemberDto.Summary> members = memberService.searchMembersByName(name);
        return ResponseEntity.ok(members);
    }

    /**
     * 회원 정보 수정
     * 
     * PUT /members/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole(T(com.example.member.security.SecurityRoles).ADMIN)")
    public ResponseEntity<MemberDto.Response> updateMember(
            @PathVariable Long id, 
            @Valid @RequestBody MemberDto.UpdateRequest request) {
        log.info("Update member request received for ID: {}", id);
        
        MemberDto.Response response = memberService.updateMember(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 삭제
     * 
     * DELETE /members/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole(T(com.example.member.security.SecurityRoles).ADMIN)")
    public ResponseEntity<Map<String, String>> deleteMember(@PathVariable Long id) {
        log.info("Delete member request received for ID: {}", id);
        
        memberService.deleteMember(id);
        
        Map<String, String> response = Map.of(
            "message", "회원이 성공적으로 삭제되었습니다",
            "memberId", id.toString()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 활성 회원 수 조회
     * 
     * GET /members/stats/active-count
     */
    @GetMapping("/stats/active-count")
    @PreAuthorize("hasAnyRole(T(com.example.member.security.SecurityRoles).ADMIN, T(com.example.member.security.SecurityRoles).USER)")
    public ResponseEntity<Map<String, Long>> getActiveMemberCount() {
        log.debug("Get active member count request received");
        
        long count = memberService.getActiveMemberCount();
        Map<String, Long> response = Map.of("activeCount", count);
        return ResponseEntity.ok(response);
    }

    /**
     * 헬스 체크
     * 
     * GET /members/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("Member service health check requested");
        
        Map<String, String> response = Map.of(
            "status", "UP",
            "service", "member-service"
        );
        return ResponseEntity.ok(response);
    }
}
