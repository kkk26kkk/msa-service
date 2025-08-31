package com.example.member.service;

import com.example.member.dto.MemberDto;
import com.example.member.entity.Member;
import com.example.member.exception.MemberNotFoundException;
import com.example.member.exception.DuplicateMemberException;
import com.example.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Member Service 유닛 테스트
 * 
 * Mockito를 사용하여 MemberService의 비즈니스 로직을 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Member Service 테스트")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    private Member testMember;
    private MemberDto.CreateRequest createRequest;
    private MemberDto.UpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        // 테스트용 Member 엔터티 생성
        testMember = Member.builder()
                .id(1L)
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .fullName("테스트 사용자")
                .phoneNumber("010-1234-5678")
                .status(Member.MemberStatus.ACTIVE)
                .build();

        // 테스트용 CreateRequest DTO 생성
        createRequest = MemberDto.CreateRequest.builder()
                .username("newuser")
                .password("password123")
                .email("newuser@example.com")
                .fullName("새로운 사용자")
                .phoneNumber("010-9999-0000")
                .build();

        // 테스트용 UpdateRequest DTO 생성
        updateRequest = MemberDto.UpdateRequest.builder()
                .fullName("수정된 이름")
                .phoneNumber("010-8888-7777")
                .status(Member.MemberStatus.INACTIVE)
                .build();
    }

    @Test
    @DisplayName("회원 생성 성공")
    void createMember_Success() {
        // Given
        when(memberRepository.existsByUsername(createRequest.getUsername())).thenReturn(false);
        when(memberRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        MemberDto.Response result = memberService.createMember(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(testMember.getUsername());
        assertThat(result.getEmail()).isEqualTo(testMember.getEmail());
        
        verify(memberRepository).existsByUsername(createRequest.getUsername());
        verify(memberRepository).existsByEmail(createRequest.getEmail());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원 생성 실패 - 중복된 사용자명")
    void createMember_DuplicateUsername() {
        // Given
        when(memberRepository.existsByUsername(createRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> memberService.createMember(createRequest))
                .isInstanceOf(DuplicateMemberException.class)
                .hasMessageContaining("이미 존재하는 사용자명입니다");

        verify(memberRepository).existsByUsername(createRequest.getUsername());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("회원 생성 실패 - 중복된 이메일")
    void createMember_DuplicateEmail() {
        // Given
        when(memberRepository.existsByUsername(createRequest.getUsername())).thenReturn(false);
        when(memberRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> memberService.createMember(createRequest))
                .isInstanceOf(DuplicateMemberException.class)
                .hasMessageContaining("이미 존재하는 이메일입니다");

        verify(memberRepository).existsByEmail(createRequest.getEmail());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("ID로 회원 조회 성공")
    void getMemberById_Success() {
        // Given
        Long memberId = 1L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));

        // When
        MemberDto.Response result = memberService.getMemberById(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testMember.getId());
        assertThat(result.getUsername()).isEqualTo(testMember.getUsername());
        
        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("ID로 회원 조회 실패 - 존재하지 않는 회원")
    void getMemberById_NotFound() {
        // Given
        Long memberId = 999L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> memberService.getMemberById(memberId))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessageContaining("회원을 찾을 수 없습니다");

        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("사용자명으로 회원 조회 성공")
    void getMemberByUsername_Success() {
        // Given
        String username = "testuser";
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(testMember));

        // When
        MemberDto.Response result = memberService.getMemberByUsername(username);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        
        verify(memberRepository).findByUsername(username);
    }

    @Test
    @DisplayName("모든 회원 조회")
    void getAllMembers_Success() {
        // Given
        List<Member> members = Arrays.asList(testMember, testMember);
        when(memberRepository.findAll()).thenReturn(members);

        // When
        List<MemberDto.Summary> result = memberService.getAllMembers();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo(testMember.getUsername());
        
        verify(memberRepository).findAll();
    }

    @Test
    @DisplayName("회원 페이징 조회")
    void getMembers_WithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Member> members = Arrays.asList(testMember);
        Page<Member> memberPage = new PageImpl<>(members, pageable, 1);
        when(memberRepository.findAll(pageable)).thenReturn(memberPage);

        // When
        Page<MemberDto.Summary> result = memberService.getMembers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        
        verify(memberRepository).findAll(pageable);
    }

    @Test
    @DisplayName("상태별 회원 조회")
    void getMembersByStatus_Success() {
        // Given
        Member.MemberStatus status = Member.MemberStatus.ACTIVE;
        List<Member> members = Arrays.asList(testMember);
        when(memberRepository.findByStatus(status)).thenReturn(members);

        // When
        List<MemberDto.Summary> result = memberService.getMembersByStatus(status);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(status);
        
        verify(memberRepository).findByStatus(status);
    }

    @Test
    @DisplayName("회원 정보 수정 성공")
    void updateMember_Success() {
        // Given
        Long memberId = 1L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        MemberDto.Response result = memberService.updateMember(memberId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(memberRepository).findById(memberId);
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 존재하지 않는 회원")
    void updateMember_NotFound() {
        // Given
        Long memberId = 999L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> memberService.updateMember(memberId, updateRequest))
                .isInstanceOf(MemberNotFoundException.class);

        verify(memberRepository).findById(memberId);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("회원 삭제 성공")
    void deleteMember_Success() {
        // Given
        Long memberId = 1L;
        when(memberRepository.existsById(memberId)).thenReturn(true);

        // When
        memberService.deleteMember(memberId);

        // Then
        verify(memberRepository).existsById(memberId);
        verify(memberRepository).deleteById(memberId);
    }

    @Test
    @DisplayName("회원 삭제 실패 - 존재하지 않는 회원")
    void deleteMember_NotFound() {
        // Given
        Long memberId = 999L;
        when(memberRepository.existsById(memberId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> memberService.deleteMember(memberId))
                .isInstanceOf(MemberNotFoundException.class);

        verify(memberRepository).existsById(memberId);
        verify(memberRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("이름으로 회원 검색")
    void searchMembersByName_Success() {
        // Given
        String name = "테스트";
        List<Member> members = Arrays.asList(testMember);
        when(memberRepository.findByFullNameContaining(name)).thenReturn(members);

        // When
        List<MemberDto.Summary> result = memberService.searchMembersByName(name);

        // Then
        assertThat(result).hasSize(1);
        verify(memberRepository).findByFullNameContaining(name);
    }

    @Test
    @DisplayName("활성 회원 수 조회")
    void getActiveMemberCount_Success() {
        // Given
        long expectedCount = 5L;
        when(memberRepository.countActiveMembers()).thenReturn(expectedCount);

        // When
        long result = memberService.getActiveMemberCount();

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(memberRepository).countActiveMembers();
    }
}


