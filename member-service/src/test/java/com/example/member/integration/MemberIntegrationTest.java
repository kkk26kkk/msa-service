package com.example.member.integration;

import com.example.member.dto.MemberDto;
import com.example.member.entity.Member;
import com.example.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Member Service 통합 테스트
 * 
 * 실제 Spring Boot 컨텍스트를 로드하여 전체 플로우를 테스트합니다.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Member 통합 테스트")
class MemberIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 기존 데이터 정리
        memberRepository.deleteAll();
        
        // 테스트용 데이터 추가
        Member testMember = Member.builder()
                .username("existinguser")
                .password("password123")
                .email("existing@example.com")
                .fullName("기존 사용자")
                .phoneNumber("010-1234-5678")
                .status(Member.MemberStatus.ACTIVE)
                .build();
        memberRepository.save(testMember);
    }

    @Test
    @DisplayName("회원 생성부터 조회까지 전체 플로우 테스트")
    void createAndRetrieveMemberFlow() throws Exception {
        // 1. 회원 생성
        MemberDto.CreateRequest createRequest = MemberDto.CreateRequest.builder()
                .username("newuser")
                .password("password123")
                .email("newuser@example.com")
                .fullName("새로운 사용자")
                .phoneNumber("010-9999-0000")
                .build();

        // 회원 생성 API 호출
        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"));

        // 2. 생성된 회원 조회
        mockMvc.perform(get("/members/username/newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"));

        // 3. 전체 회원 목록 조회 (기존 1명 + 새로 생성 1명 = 2명)
        mockMvc.perform(get("/members/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("회원 수정 플로우 테스트")
    void updateMemberFlow() throws Exception {
        // 1. 기존 회원 조회
        Member existingMember = memberRepository.findByUsername("existinguser").orElseThrow();

        // 2. 회원 정보 수정
        MemberDto.UpdateRequest updateRequest = MemberDto.UpdateRequest.builder()
                .fullName("수정된 이름")
                .phoneNumber("010-8888-7777")
                .status(Member.MemberStatus.INACTIVE)
                .build();

        mockMvc.perform(put("/members/{id}", existingMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("수정된 이름"))
                .andExpect(jsonPath("$.phoneNumber").value("010-8888-7777"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        // 3. 수정된 내용 확인
        mockMvc.perform(get("/members/{id}", existingMember.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("수정된 이름"));
    }

    @Test
    @DisplayName("회원 검색 기능 테스트")
    void searchMemberFlow() throws Exception {
        // 1. 이름으로 검색
        mockMvc.perform(get("/members/search")
                        .param("name", "기존"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("기존 사용자"));

        // 2. 상태별 조회
        mockMvc.perform(get("/members/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 3. 활성 회원 수 조회
        mockMvc.perform(get("/members/stats/active-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCount").value(1));
    }

    @Test
    @DisplayName("중복 데이터 검증 테스트")
    void duplicateDataValidationFlow() throws Exception {
        // 1. 중복된 사용자명으로 회원 생성 시도
        MemberDto.CreateRequest duplicateUsernameRequest = MemberDto.CreateRequest.builder()
                .username("existinguser") // 이미 존재하는 사용자명
                .password("password123")
                .email("different@example.com")
                .fullName("다른 사용자")
                .build();

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUsernameRequest)))
                .andExpected(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate Member"));

        // 2. 중복된 이메일로 회원 생성 시도
        MemberDto.CreateRequest duplicateEmailRequest = MemberDto.CreateRequest.builder()
                .username("differentuser")
                .password("password123")
                .email("existing@example.com") // 이미 존재하는 이메일
                .fullName("다른 사용자")
                .build();

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
                .andExpected(status().isConflict());
    }

    @Test
    @DisplayName("회원 삭제 플로우 테스트")
    void deleteMemberFlow() throws Exception {
        // 1. 기존 회원 조회
        Member existingMember = memberRepository.findByUsername("existinguser").orElseThrow();

        // 2. 회원 삭제
        mockMvc.perform(delete("/members/{id}", existingMember.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원이 성공적으로 삭제되었습니다"));

        // 3. 삭제된 회원 조회 시도 (404 에러 발생해야 함)
        mockMvc.perform(get("/members/{id}", existingMember.getId()))
                .andExpected(status().isNotFound());

        // 4. 전체 회원 수 확인 (0명이어야 함)
        mockMvc.perform(get("/members/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("페이징 기능 테스트")
    void paginationFlow() throws Exception {
        // 1. 추가 테스트 데이터 생성 (총 6명)
        for (int i = 1; i <= 5; i++) {
            Member member = Member.builder()
                    .username("user" + i)
                    .password("password123")
                    .email("user" + i + "@example.com")
                    .fullName("사용자" + i)
                    .status(Member.MemberStatus.ACTIVE)
                    .build();
            memberRepository.save(member);
        }

        // 2. 첫 번째 페이지 조회 (size=3)
        mockMvc.perform(get("/members")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        // 3. 두 번째 페이지 조회
        mockMvc.perform(get("/members")
                        .param("page", "1")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    @DisplayName("헬스 체크 테스트")
    void healthCheckFlow() throws Exception {
        mockMvc.perform(get("/members/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("member-service"));
    }
}
