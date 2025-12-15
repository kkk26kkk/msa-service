package com.example.member.service;

import com.example.member.dto.MemberDto;
import com.example.member.entity.Member;
import com.example.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Member Service 캐싱 통합 테스트
 * 
 * 실제 캐시 동작을 검증하는 통합 테스트입니다.
 */
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.fail-fast=false",
    "eureka.client.enabled=false"
})
@ActiveProfiles("test")
@Transactional
@DisplayName("Member Service 캐싱 테스트")
class MemberServiceCacheTest {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceCacheTest.class);

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CacheManager cacheManager;

    private Member testMember;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = Member.builder()
                .username("cachetest")
                .password("password123")
                .email("cachetest@example.com")
                .fullName("캐시 테스트 사용자")
                .phoneNumber("010-1111-2222")
                .status(Member.MemberStatus.ACTIVE)
                .build();
        
        testMember = memberRepository.save(testMember);
    }

    @Test
    @DisplayName("ID로 회원 조회 - 캐시 히트 확인")
    void getMemberById_CacheHit() {
        // Given
        Long memberId = testMember.getId();
        log.debug("=== 캐시 테스트 시작: ID로 회원 조회 ===");
        log.debug("회원 ID: {}", memberId);
        
        // 첫 번째 조회 (캐시 미스)
        log.debug("1. 첫 번째 조회 (캐시 미스 예상)");
        MemberDto.Response firstResult = memberService.getMemberById(memberId);
        assertThat(firstResult).isNotNull();
        log.debug("첫 번째 조회 결과: ID={}, Username={}", firstResult.getId(), firstResult.getUsername());
        
        // 캐시 확인
        var cache = cacheManager.getCache("members");
        assertThat(cache).isNotNull();
        if (cache != null) {
            var cachedValue = cache.get(memberId);
            assertThat(cachedValue).isNotNull();
            log.debug("2. 캐시 확인: 캐시에 데이터가 저장되었습니다. Key={}, Value 존재={}", memberId, cachedValue != null);
        }
        
        // 두 번째 조회 (캐시 히트 - Repository 호출 없이 캐시에서 반환)
        log.debug("3. 두 번째 조회 (캐시 히트 예상)");
        MemberDto.Response secondResult = memberService.getMemberById(memberId);
        assertThat(secondResult).isNotNull();
        assertThat(secondResult.getId()).isEqualTo(firstResult.getId());
        assertThat(secondResult.getUsername()).isEqualTo(firstResult.getUsername());
        log.debug("두 번째 조회 결과: ID={}, Username={}", secondResult.getId(), secondResult.getUsername());
        log.debug("=== 캐시 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자명으로 회원 조회 - 캐시 히트 확인")
    void getMemberByUsername_CacheHit() {
        // Given
        String username = testMember.getUsername();
        log.debug("=== 캐시 테스트 시작: 사용자명으로 회원 조회 ===");
        log.debug("Username: {}", username);
        
        // 첫 번째 조회 (캐시 미스)
        log.debug("1. 첫 번째 조회 (캐시 미스 예상)");
        MemberDto.Response firstResult = memberService.getMemberByUsername(username);
        assertThat(firstResult).isNotNull();
        log.debug("첫 번째 조회 결과: ID={}, Username={}", firstResult.getId(), firstResult.getUsername());
        
        // 캐시 확인
        var cache = cacheManager.getCache("memberByUsername");
        assertThat(cache).isNotNull();
        if (cache != null) {
            assertThat(cache.get(username)).isNotNull();
            log.debug("2. 캐시 확인: 캐시에 데이터가 저장되었습니다. Key={}", username);
        }
        
        // 두 번째 조회 (캐시 히트)
        log.debug("3. 두 번째 조회 (캐시 히트 예상)");
        MemberDto.Response secondResult = memberService.getMemberByUsername(username);
        assertThat(secondResult).isNotNull();
        assertThat(secondResult.getUsername()).isEqualTo(firstResult.getUsername());
        log.debug("두 번째 조회 결과: ID={}, Username={}", secondResult.getId(), secondResult.getUsername());
        log.debug("=== 캐시 테스트 완료 ===");
    }

    @Test
    @DisplayName("활성 회원 수 조회 - 캐시 히트 확인")
    void getActiveMemberCount_CacheHit() {
        log.debug("=== 캐시 테스트 시작: 활성 회원 수 조회 ===");
        
        // 첫 번째 조회 (캐시 미스)
        log.debug("1. 첫 번째 조회 (캐시 미스 예상)");
        long firstCount = memberService.getActiveMemberCount();
        assertThat(firstCount).isGreaterThanOrEqualTo(0);
        log.debug("첫 번째 조회 결과: 활성 회원 수={}", firstCount);
        
        // 캐시 확인
        var cache = cacheManager.getCache("activeMemberCount");
        assertThat(cache).isNotNull();
        log.debug("2. 캐시 확인: 캐시가 생성되었습니다.");
        
        // 두 번째 조회 (캐시 히트)
        log.debug("3. 두 번째 조회 (캐시 히트 예상)");
        long secondCount = memberService.getActiveMemberCount();
        assertThat(secondCount).isEqualTo(firstCount);
        log.debug("두 번째 조회 결과: 활성 회원 수={}", secondCount);
        log.debug("=== 캐시 테스트 완료 ===");
    }

    @Test
    @DisplayName("회원 수정 시 캐시 무효화 확인")
    void updateMember_CacheEviction() {
        // Given
        Long memberId = testMember.getId();
        String username = testMember.getUsername();
        log.debug("=== 캐시 무효화 테스트 시작: 회원 수정 ===");
        log.debug("회원 ID: {}, Username: {}", memberId, username);
        
        // 캐시에 데이터 저장
        log.debug("1. 캐시에 데이터 저장");
        memberService.getMemberById(memberId);
        memberService.getMemberByUsername(username);
        
        // 캐시에 데이터가 있는지 확인
        var membersCache = cacheManager.getCache("members");
        var usernameCache = cacheManager.getCache("memberByUsername");
        assertThat(membersCache).isNotNull();
        assertThat(usernameCache).isNotNull();
        if (membersCache != null && usernameCache != null) {
            assertThat(membersCache.get(memberId)).isNotNull();
            assertThat(usernameCache.get(username)).isNotNull();
            log.debug("2. 캐시 확인: 캐시에 데이터가 저장되었습니다.");
            
            // 회원 정보 수정
            log.debug("3. 회원 정보 수정 (캐시 무효화 예상)");
            MemberDto.UpdateRequest updateRequest = MemberDto.UpdateRequest.builder()
                    .fullName("수정된 이름")
                    .build();
            
            memberService.updateMember(memberId, updateRequest);
            
            // 캐시가 무효화되었는지 확인
            assertThat(membersCache.get(memberId)).isNull();
            assertThat(usernameCache.get(username)).isNull();
            log.debug("4. 캐시 무효화 확인: 캐시가 비워졌습니다.");
            log.debug("=== 캐시 무효화 테스트 완료 ===");
        }
    }

    @Test
    @DisplayName("회원 삭제 시 캐시 무효화 확인")
    void deleteMember_CacheEviction() {
        // Given
        Long memberId = testMember.getId();
        String username = testMember.getUsername();
        log.debug("=== 캐시 무효화 테스트 시작: 회원 삭제 ===");
        log.debug("회원 ID: {}, Username: {}", memberId, username);
        
        // 캐시에 데이터 저장
        log.debug("1. 캐시에 데이터 저장");
        memberService.getMemberById(memberId);
        memberService.getMemberByUsername(username);
        memberService.getActiveMemberCount();
        
        // 캐시에 데이터가 있는지 확인
        var membersCache = cacheManager.getCache("members");
        var usernameCache = cacheManager.getCache("memberByUsername");
        var activeCountCache = cacheManager.getCache("activeMemberCount");
        assertThat(membersCache).isNotNull();
        assertThat(usernameCache).isNotNull();
        assertThat(activeCountCache).isNotNull();
        if (membersCache != null && usernameCache != null) {
            assertThat(membersCache.get(memberId)).isNotNull();
            assertThat(usernameCache.get(username)).isNotNull();
            log.debug("2. 캐시 확인: 캐시에 데이터가 저장되었습니다.");
            
            // 회원 삭제
            log.debug("3. 회원 삭제 (캐시 무효화 예상)");
            memberService.deleteMember(memberId);
            
            // 캐시가 무효화되었는지 확인
            assertThat(membersCache.get(memberId)).isNull();
            assertThat(usernameCache.get(username)).isNull();
            log.debug("4. 캐시 무효화 확인: 캐시가 비워졌습니다.");
            log.debug("=== 캐시 무효화 테스트 완료 ===");
        }
        // activeMemberCount는 allEntries = true로 전체 무효화
    }

    @Test
    @DisplayName("회원 생성 시 활성 회원 수 캐시 무효화 확인")
    void createMember_CacheEviction() {
        log.debug("=== 캐시 무효화 테스트 시작: 회원 생성 ===");
        
        // Given
        // 활성 회원 수 캐시에 데이터 저장
        log.debug("1. 활성 회원 수 캐시에 데이터 저장");
        long initialCount = memberService.getActiveMemberCount();
        log.debug("초기 활성 회원 수: {}", initialCount);
        
        var activeCountCache = cacheManager.getCache("activeMemberCount");
        assertThat(activeCountCache).isNotNull();
        log.debug("2. 캐시 확인: 활성 회원 수 캐시가 생성되었습니다.");
        
        // 새 회원 생성
        log.debug("3. 새 회원 생성 (활성 회원 수 캐시 무효화 예상)");
        MemberDto.CreateRequest createRequest = MemberDto.CreateRequest.builder()
                .username("newcacheuser")
                .password("password123")
                .email("newcacheuser@example.com")
                .fullName("새 캐시 사용자")
                .phoneNumber("010-3333-4444")
                .build();
        
        memberService.createMember(createRequest);
        
        // 활성 회원 수가 증가했는지 확인 (캐시 무효화 후 재조회)
        log.debug("4. 활성 회원 수 재조회 (캐시 무효화 후)");
        long newCount = memberService.getActiveMemberCount();
        assertThat(newCount).isGreaterThan(initialCount);
        log.debug("새 활성 회원 수: {} (증가: {})", newCount, newCount - initialCount);
        log.debug("=== 캐시 무효화 테스트 완료 ===");
    }
}

