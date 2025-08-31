package com.example.member.service;

import com.example.member.dto.MemberDto;
import com.example.member.entity.Member;
import com.example.member.exception.MemberNotFoundException;
import com.example.member.exception.DuplicateMemberException;
import com.example.member.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 회원 서비스
 * 
 * 회원 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@Transactional(readOnly = true)
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * 회원 생성
     */
    @Transactional
    public MemberDto.Response createMember(MemberDto.CreateRequest request) {
        log.info("Creating new member with username: {}", request.getUsername());

        // 중복 검사
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateMemberException("이미 존재하는 사용자명입니다: " + request.getUsername());
        }

        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateMemberException("이미 존재하는 이메일입니다: " + request.getEmail());
        }

        // 엔터티 생성 및 저장
        Member member = request.toEntity();
        Member savedMember = memberRepository.save(member);

        log.info("Member created successfully with ID: {}", savedMember.getId());
        return MemberDto.Response.from(savedMember);
    }

    /**
     * ID로 회원 조회
     */
    public MemberDto.Response getMemberById(Long id) {
        log.debug("Retrieving member by ID: {}", id);

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다. ID: " + id));

        return MemberDto.Response.from(member);
    }

    /**
     * 사용자명으로 회원 조회
     */
    public MemberDto.Response getMemberByUsername(String username) {
        log.debug("Retrieving member by username: {}", username);

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다. Username: " + username));

        return MemberDto.Response.from(member);
    }

    /**
     * 모든 회원 조회
     */
    public List<MemberDto.Summary> getAllMembers() {
        log.debug("Retrieving all members");

        List<Member> members = memberRepository.findAll();
        return members.stream()
                .map(MemberDto.Summary::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원 페이징 조회
     */
    public Page<MemberDto.Summary> getMembers(Pageable pageable) {
        log.debug("Retrieving members with pagination: {}", pageable);

        Page<Member> memberPage = memberRepository.findAll(pageable);
        return memberPage.map(MemberDto.Summary::from);
    }

    /**
     * 상태별 회원 조회
     */
    public List<MemberDto.Summary> getMembersByStatus(Member.MemberStatus status) {
        log.debug("Retrieving members by status: {}", status);

        List<Member> members = memberRepository.findByStatus(status);
        return members.stream()
                .map(MemberDto.Summary::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원 정보 수정
     */
    @Transactional
    public MemberDto.Response updateMember(Long id, MemberDto.UpdateRequest request) {
        log.info("Updating member with ID: {}", id);

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다. ID: " + id));

        // 수정 가능한 필드 업데이트
        if (request.getFullName() != null) {
            member.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            member.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getStatus() != null) {
            member.setStatus(request.getStatus());
        }

        Member updatedMember = memberRepository.save(member);
        log.info("Member updated successfully with ID: {}", updatedMember.getId());

        return MemberDto.Response.from(updatedMember);
    }

    /**
     * 회원 삭제
     */
    @Transactional
    public void deleteMember(Long id) {
        log.info("Deleting member with ID: {}", id);

        if (!memberRepository.existsById(id)) {
            throw new MemberNotFoundException("회원을 찾을 수 없습니다. ID: " + id);
        }

        memberRepository.deleteById(id);
        log.info("Member deleted successfully with ID: {}", id);
    }

    /**
     * 이름으로 회원 검색
     */
    public List<MemberDto.Summary> searchMembersByName(String name) {
        log.debug("Searching members by name: {}", name);

        List<Member> members = memberRepository.findByFullNameContaining(name);
        return members.stream()
                .map(MemberDto.Summary::from)
                .collect(Collectors.toList());
    }

    /**
     * 활성 회원 수 조회
     */
    public long getActiveMemberCount() {
        log.debug("Retrieving active member count");
        return memberRepository.countActiveMembers();
    }
}
