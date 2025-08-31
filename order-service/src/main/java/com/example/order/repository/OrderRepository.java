package com.example.order.repository;

import com.example.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 리포지토리
 * 
 * 주문 데이터 접근을 담당하는 JPA Repository입니다.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 회원별 주문 목록 조회
     */
    List<Order> findByMemberId(Long memberId);

    /**
     * 회원별 주문 목록 페이징 조회
     */
    Page<Order> findByMemberId(Long memberId, Pageable pageable);

    /**
     * 상태별 주문 조회
     */
    List<Order> findByStatus(Order.OrderStatus status);

    /**
     * 상태별 주문 페이징 조회
     */
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    /**
     * 회원별 특정 상태 주문 조회
     */
    List<Order> findByMemberIdAndStatus(Long memberId, Order.OrderStatus status);

    /**
     * 상품명으로 주문 검색 (부분 일치)
     */
    @Query("SELECT o FROM Order o WHERE o.productName LIKE %:productName%")
    List<Order> findByProductNameContaining(@Param("productName") String productName);

    /**
     * 기간별 주문 조회
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 금액 이상의 주문 조회
     */
    List<Order> findByTotalAmountGreaterThanEqual(BigDecimal amount);

    /**
     * 회원별 총 주문 금액 조회
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.memberId = :memberId")
    BigDecimal getTotalAmountByMemberId(@Param("memberId") Long memberId);

    /**
     * 상태별 주문 수 조회
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") Order.OrderStatus status);

    /**
     * 회원별 주문 수 조회
     */
    long countByMemberId(Long memberId);

    /**
     * 최근 주문 조회 (최신순 상위 N개)
     */
    List<Order> findTop10ByOrderByCreatedAtDesc();

    /**
     * 특정 회원의 최근 주문 조회
     */
    List<Order> findTop5ByMemberIdOrderByCreatedAtDesc(Long memberId);
}


