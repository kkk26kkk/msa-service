package com.example.order.service;

import com.example.order.client.MemberServiceClient;
import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.exception.InvalidOrderException;
import com.example.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 서비스
 * 
 * 주문 관련 비즈니스 로직을 처리하고 Member Service와 연동합니다.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final MemberServiceClient memberServiceClient;

    public OrderService(OrderRepository orderRepository, MemberServiceClient memberServiceClient) {
        this.orderRepository = orderRepository;
        this.memberServiceClient = memberServiceClient;
    }

    /**
     * 주문 생성
     */
    @Transactional
    public OrderDto.Response createOrder(OrderDto.CreateRequest request) {
        log.info("Creating new order for member: {}", request.getMemberId());

        // 1. 회원 정보 조회 및 검증
        MemberServiceClient.MemberDto member = validateMember(request.getMemberId());
        
        // 2. 주문 데이터 검증
        validateOrderRequest(request);

        // 3. 엔터티 생성 및 저장
        Order order = request.toEntity();
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully with ID: {}", savedOrder.getId());
        
        // 4. 회원명과 함께 응답 DTO 생성
        return OrderDto.Response.from(savedOrder, member.getFullName());
    }

    /**
     * ID로 주문 조회
     */
    public OrderDto.Response getOrderById(Long id) {
        log.debug("Retrieving order by ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. ID: " + id));

        // 회원 정보 조회
        String memberName = getMemberName(order.getMemberId());
        
        return OrderDto.Response.from(order, memberName);
    }

    /**
     * 모든 주문 조회
     */
    public List<OrderDto.Summary> getAllOrders() {
        log.debug("Retrieving all orders");

        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(order -> {
                    String memberName = getMemberName(order.getMemberId());
                    return OrderDto.Summary.from(order, memberName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 주문 페이징 조회
     */
    public Page<OrderDto.Summary> getOrders(Pageable pageable) {
        log.debug("Retrieving orders with pagination: {}", pageable);

        Page<Order> orderPage = orderRepository.findAll(pageable);
        return orderPage.map(order -> {
            String memberName = getMemberName(order.getMemberId());
            return OrderDto.Summary.from(order, memberName);
        });
    }

    /**
     * 회원별 주문 조회
     */
    public List<OrderDto.Summary> getOrdersByMemberId(Long memberId) {
        log.debug("Retrieving orders by member ID: {}", memberId);

        // 회원 정보 검증
        MemberServiceClient.MemberDto member = validateMember(memberId);
        
        List<Order> orders = orderRepository.findByMemberId(memberId);
        return orders.stream()
                .map(order -> OrderDto.Summary.from(order, member.getFullName()))
                .collect(Collectors.toList());
    }

    /**
     * 상태별 주문 조회
     */
    public List<OrderDto.Summary> getOrdersByStatus(Order.OrderStatus status) {
        log.debug("Retrieving orders by status: {}", status);

        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
                .map(order -> {
                    String memberName = getMemberName(order.getMemberId());
                    return OrderDto.Summary.from(order, memberName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 상품명으로 주문 검색
     */
    public List<OrderDto.Summary> searchOrdersByProductName(String productName) {
        log.debug("Searching orders by product name: {}", productName);

        List<Order> orders = orderRepository.findByProductNameContaining(productName);
        return orders.stream()
                .map(order -> {
                    String memberName = getMemberName(order.getMemberId());
                    return OrderDto.Summary.from(order, memberName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 주문 정보 수정
     */
    @Transactional
    public OrderDto.Response updateOrder(Long id, OrderDto.UpdateRequest request) {
        log.info("Updating order with ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. ID: " + id));

        // 수정 가능한 필드 업데이트
        if (request.getQuantity() != null) {
            order.setQuantity(request.getQuantity());
            // 수량 변경 시 총 금액 재계산
            BigDecimal newTotalAmount = order.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            order.setTotalAmount(newTotalAmount);
        }
        
        if (request.getUnitPrice() != null) {
            order.setUnitPrice(request.getUnitPrice());
            // 단가 변경 시 총 금액 재계산
            BigDecimal newTotalAmount = request.getUnitPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            order.setTotalAmount(newTotalAmount);
        }
        
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        
        if (request.getOrderMemo() != null) {
            order.setOrderMemo(request.getOrderMemo());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order updated successfully with ID: {}", updatedOrder.getId());

        // 회원 정보 조회
        String memberName = getMemberName(order.getMemberId());
        
        return OrderDto.Response.from(updatedOrder, memberName);
    }

    /**
     * 주문 삭제
     */
    @Transactional
    public void deleteOrder(Long id) {
        log.info("Deleting order with ID: {}", id);

        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException("주문을 찾을 수 없습니다. ID: " + id);
        }

        orderRepository.deleteById(id);
        log.info("Order deleted successfully with ID: {}", id);
    }

    /**
     * 기간별 주문 조회
     */
    public List<OrderDto.Summary> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Retrieving orders between {} and {}", startDate, endDate);

        List<Order> orders = orderRepository.findByCreatedAtBetween(startDate, endDate);
        return orders.stream()
                .map(order -> {
                    String memberName = getMemberName(order.getMemberId());
                    return OrderDto.Summary.from(order, memberName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 회원별 총 주문 금액 조회
     */
    public BigDecimal getTotalAmountByMemberId(Long memberId) {
        log.debug("Retrieving total amount by member ID: {}", memberId);
        
        // 회원 정보 검증
        validateMember(memberId);
        
        BigDecimal totalAmount = orderRepository.getTotalAmountByMemberId(memberId);
        return totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    /**
     * 상태별 주문 수 조회
     */
    public long getOrderCountByStatus(Order.OrderStatus status) {
        log.debug("Retrieving order count by status: {}", status);
        return orderRepository.countByStatus(status);
    }

    /**
     * 최근 주문 조회
     */
    public List<OrderDto.Summary> getRecentOrders() {
        log.debug("Retrieving recent orders");

        List<Order> orders = orderRepository.findTop10ByOrderByCreatedAtDesc();
        return orders.stream()
                .map(order -> {
                    String memberName = getMemberName(order.getMemberId());
                    return OrderDto.Summary.from(order, memberName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 회원 정보 검증 (Fallback 허용)
     */
    private MemberServiceClient.MemberDto validateMember(Long memberId) {
        try {
            MemberServiceClient.MemberDto member = memberServiceClient.getMemberById(memberId);
            
            // Fallback 응답인지 확인
            if ("UNKNOWN".equals(member.getStatus())) {
                log.warn("Member Service is not available for member ID: {}, using fallback data", memberId);
                // Fallback 데이터를 사용하지만 주문 처리는 계속 진행
                return member;
            }
            
            return member;
        } catch (Exception e) {
            log.error("Failed to validate member with ID: {}, creating fallback member", memberId, e);
            
            // Exception 발생 시에도 Fallback 데이터 생성
            return new MemberServiceClient.MemberDto(
                memberId,
                "unknown-user-" + memberId,
                "unknown@example.com",
                "알 수 없는 사용자",
                "000-0000-0000",
                "UNKNOWN",
                "서비스 일시 중단"
            );
        }
    }

    /**
     * 회원명 조회 (안전한 방식)
     */
    private String getMemberName(Long memberId) {
        try {
            MemberServiceClient.MemberDto member = memberServiceClient.getMemberById(memberId);
            return member.getFullName();
        } catch (Exception e) {
            log.warn("Failed to get member name for ID: {}, using fallback", memberId);
            return "알 수 없는 사용자";
        }
    }

    /**
     * 주문 요청 데이터 검증
     */
    private void validateOrderRequest(OrderDto.CreateRequest request) {
        if (request.getQuantity() <= 0) {
            throw new InvalidOrderException("수량은 0보다 커야 합니다");
        }
        
        if (request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("단가는 0보다 커야 합니다");
        }
        
        // 총 금액이 너무 큰 경우 검증
        BigDecimal totalAmount = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal maxAmount = new BigDecimal("999999999.99");
        if (totalAmount.compareTo(maxAmount) > 0) {
            throw new InvalidOrderException("주문 금액이 너무 큽니다");
        }
    }
}
