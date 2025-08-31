package com.example.member.repository;

import com.example.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 회원 리포지토리
 * 
 * 회원 데이터 접근을 담당하는 JPA Repository입니다.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 사용자명으로 회원 조회
     */
    Optional<Member> findByUsername(String username);

    /**
     * 이메일로 회원 조회
     */
    Optional<Member> findByEmail(String email);

    /**
     * 사용자명 존재 여부 확인
     */
    boolean existsByUsername(String username);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 상태별 회원 조회
     */
    List<Member> findByStatus(Member.MemberStatus status);

    /**
     * 상태별 회원 페이징 조회
     */
    Page<Member> findByStatus(Member.MemberStatus status, Pageable pageable);

    /**
     * 이름으로 회원 검색 (부분 일치)
     */
    @Query("SELECT m FROM Member m WHERE m.fullName LIKE %:name%")
    List<Member> findByFullNameContaining(@Param("name") String name);

    /**
     * 사용자명 또는 이메일로 회원 검색
     */
    @Query("SELECT m FROM Member m WHERE m.username = :keyword OR m.email = :keyword")
    Optional<Member> findByUsernameOrEmail(@Param("keyword") String keyword);

    /**
     * 활성 회원 수 조회
     */
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE'")
    long countActiveMembers();
}


