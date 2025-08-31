package com.example.member.controller;

import com.example.member.dto.MemberDto;
import com.example.member.entity.Member;
import com.example.member.exception.MemberNotFoundException;
import com.example.member.exception.DuplicateMemberException;
import com.example.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Member Controller 통합 테스트
 * 
 * @WebMvcTest를 사용하여 HTTP 요청/응답을 테스트합니다.
 */
@WebMvcTest(MemberController.class)
@DisplayName("Member Controller 테스트")
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberService memberService;

    private MemberDto.CreateRequest createRequest;
    private MemberDto.UpdateRequest updateRequest;
    private MemberDto.Response memberResponse;
    private MemberDto.Summary memberSummary;

    @BeforeEach
    void setUp() {
        // 테스트용 DTO 생성
        createRequest = MemberDto.CreateRequest.builder()
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .fullName("테스트 사용자")
                .phoneNumber("010-1234-5678")
                .build();

        updateRequest = MemberDto.UpdateRequest.builder()
                .fullName("수정된 이름")
                .phoneNumber("010-8888-7777")
                .status(Member.MemberStatus.INACTIVE)
                .build();

        memberResponse = MemberDto.Response.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("테스트 사용자")
                .phoneNumber("010-1234-5678")
                .status(Member.MemberStatus.ACTIVE)
                .statusDescription("활성")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        memberSummary = MemberDto.Summary.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("테스트 사용자")
                .status(Member.MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("회원 생성 API 테스트 - 성공")
    void createMember_Success() throws Exception {
        // Given
        when(memberService.createMember(any(MemberDto.CreateRequest.class))).thenReturn(memberResponse);

        // When & Then
        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(memberService).createMember(any(MemberDto.CreateRequest.class));
    }

    @Test
    @DisplayName("회원 생성 API 테스트 - 유효성 검사 실패")
    void createMember_ValidationFailed() throws Exception {
        // Given
        MemberDto.CreateRequest invalidRequest = MemberDto.CreateRequest.builder()
                .username("ab") // 너무 짧음 (최소 3자)
                .password("123") // 너무 짧음 (최소 6자)
                .email("invalid-email") // 잘못된 이메일 형식
                .build();

        // When & Then
        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(memberService, never()).createMember(any(MemberDto.CreateRequest.class));
    }

    @Test
    @DisplayName("회원 생성 API 테스트 - 중복 데이터")
    void createMember_DuplicateData() throws Exception {
        // Given
        when(memberService.createMember(any(MemberDto.CreateRequest.class)))
                .thenThrow(new DuplicateMemberException("이미 존재하는 사용자명입니다"));

        // When & Then
        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate Member"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 사용자명입니다"));

        verify(memberService).createMember(any(MemberDto.CreateRequest.class));
    }

    @Test
    @DisplayName("회원 목록 조회 API 테스트 - 페이징")
    void getMembers_WithPagination() throws Exception {
        // Given
        List<MemberDto.Summary> members = Arrays.asList(memberSummary, memberSummary);
        Page<MemberDto.Summary> memberPage = new PageImpl<>(members, PageRequest.of(0, 10), 2);
        when(memberService.getMembers(any())).thenReturn(memberPage);

        // When & Then
        mockMvc.perform(get("/members")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(10));

        verify(memberService).getMembers(any());
    }

    @Test
    @DisplayName("ID로 회원 조회 API 테스트 - 성공")
    void getMemberById_Success() throws Exception {
        // Given
        Long memberId = 1L;
        when(memberService.getMemberById(memberId)).thenReturn(memberResponse);

        // When & Then
        mockMvc.perform(get("/members/{id}", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(memberService).getMemberById(memberId);
    }

    @Test
    @DisplayName("ID로 회원 조회 API 테스트 - 존재하지 않음")
    void getMemberById_NotFound() throws Exception {
        // Given
        Long memberId = 999L;
        when(memberService.getMemberById(memberId))
                .thenThrow(new MemberNotFoundException("회원을 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(get("/members/{id}", memberId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Member Not Found"));

        verify(memberService).getMemberById(memberId);
    }

    @Test
    @DisplayName("사용자명으로 회원 조회 API 테스트")
    void getMemberByUsername_Success() throws Exception {
        // Given
        String username = "testuser";
        when(memberService.getMemberByUsername(username)).thenReturn(memberResponse);

        // When & Then
        mockMvc.perform(get("/members/username/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        verify(memberService).getMemberByUsername(username);
    }

    @Test
    @DisplayName("상태별 회원 조회 API 테스트")
    void getMembersByStatus_Success() throws Exception {
        // Given
        List<MemberDto.Summary> members = Arrays.asList(memberSummary);
        when(memberService.getMembersByStatus(Member.MemberStatus.ACTIVE)).thenReturn(members);

        // When & Then
        mockMvc.perform(get("/members/status/{status}", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(memberService).getMembersByStatus(Member.MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("이름으로 회원 검색 API 테스트")
    void searchMembersByName_Success() throws Exception {
        // Given
        String name = "테스트";
        List<MemberDto.Summary> members = Arrays.asList(memberSummary);
        when(memberService.searchMembersByName(name)).thenReturn(members);

        // When & Then
        mockMvc.perform(get("/members/search")
                        .param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(memberService).searchMembersByName(name);
    }

    @Test
    @DisplayName("회원 정보 수정 API 테스트 - 성공")
    void updateMember_Success() throws Exception {
        // Given
        Long memberId = 1L;
        when(memberService.updateMember(eq(memberId), any(MemberDto.UpdateRequest.class)))
                .thenReturn(memberResponse);

        // When & Then
        mockMvc.perform(put("/members/{id}", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(memberService).updateMember(eq(memberId), any(MemberDto.UpdateRequest.class));
    }

    @Test
    @DisplayName("회원 삭제 API 테스트 - 성공")
    void deleteMember_Success() throws Exception {
        // Given
        Long memberId = 1L;
        doNothing().when(memberService).deleteMember(memberId);

        // When & Then
        mockMvc.perform(delete("/members/{id}", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원이 성공적으로 삭제되었습니다"))
                .andExpect(jsonPath("$.memberId").value("1"));

        verify(memberService).deleteMember(memberId);
    }

    @Test
    @DisplayName("활성 회원 수 조회 API 테스트")
    void getActiveMemberCount_Success() throws Exception {
        // Given
        long count = 10L;
        when(memberService.getActiveMemberCount()).thenReturn(count);

        // When & Then
        mockMvc.perform(get("/members/stats/active-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCount").value(10));

        verify(memberService).getActiveMemberCount();
    }

    @Test
    @DisplayName("헬스 체크 API 테스트")
    void health_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/members/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("member-service"));
    }
}
