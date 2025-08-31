package com.example.order.controller;

import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 주문 REST API 컨트롤러
 * 
 * 주문 관련 HTTP 요청을 처리하고 Member Service와 연동합니다.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 주문 생성
     * 
     * POST /orders
     */
    @PostMapping
    public ResponseEntity<OrderDto.Response> createOrder(@Valid @RequestBody OrderDto.CreateRequest request) {
        log.info("Creating order request received for member: {}", request.getMemberId());
        
        OrderDto.Response response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 주문 목록 조회 (페이징)
     * 
     * GET /orders?page=0&size=10&sort=id,desc
     */
    @GetMapping
    public ResponseEntity<Page<OrderDto.Summary>> getOrders(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        log.debug("Get orders request received with pagination: {}", pageable);
        
        Page<OrderDto.Summary> orders = orderService.getOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * 모든 주문 조회 (목록)
     * 
     * GET /orders/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<OrderDto.Summary>> getAllOrders() {
        log.debug("Get all orders request received");
        
        List<OrderDto.Summary> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * ID로 주문 조회
     * 
     * GET /orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto.Response> getOrderById(@PathVariable Long id) {
        log.debug("Get order by ID request received: {}", id);
        
        OrderDto.Response order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * 회원별 주문 조회
     * 
     * GET /orders/member/{memberId}
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<OrderDto.Summary>> getOrdersByMemberId(@PathVariable Long memberId) {
        log.debug("Get orders by member ID request received: {}", memberId);
        
        List<OrderDto.Summary> orders = orderService.getOrdersByMemberId(memberId);
        return ResponseEntity.ok(orders);
    }

    /**
     * 상태별 주문 조회
     * 
     * GET /orders/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDto.Summary>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        log.debug("Get orders by status request received: {}", status);
        
        List<OrderDto.Summary> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    /**
     * 상품명으로 주문 검색
     * 
     * GET /orders/search?productName=상품명
     */
    @GetMapping("/search")
    public ResponseEntity<List<OrderDto.Summary>> searchOrdersByProductName(@RequestParam String productName) {
        log.debug("Search orders by product name request received: {}", productName);
        
        List<OrderDto.Summary> orders = orderService.searchOrdersByProductName(productName);
        return ResponseEntity.ok(orders);
    }

    /**
     * 기간별 주문 조회
     * 
     * GET /orders/period?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
     */
    @GetMapping("/period")
    public ResponseEntity<List<OrderDto.Summary>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.debug("Get orders by date range request received: {} to {}", startDate, endDate);
        
        List<OrderDto.Summary> orders = orderService.getOrdersByDateRange(startDate, endDate);
        return ResponseEntity.ok(orders);
    }

    /**
     * 최근 주문 조회
     * 
     * GET /orders/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<OrderDto.Summary>> getRecentOrders() {
        log.debug("Get recent orders request received");
        
        List<OrderDto.Summary> orders = orderService.getRecentOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * 주문 정보 수정
     * 
     * PUT /orders/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderDto.Response> updateOrder(
            @PathVariable Long id, 
            @Valid @RequestBody OrderDto.UpdateRequest request) {
        log.info("Update order request received for ID: {}", id);
        
        OrderDto.Response response = orderService.updateOrder(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 주문 삭제
     * 
     * DELETE /orders/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable Long id) {
        log.info("Delete order request received for ID: {}", id);
        
        orderService.deleteOrder(id);
        
        Map<String, String> response = Map.of(
            "message", "주문이 성공적으로 삭제되었습니다",
            "orderId", id.toString()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 회원별 총 주문 금액 조회
     * 
     * GET /orders/stats/total-amount/{memberId}
     */
    @GetMapping("/stats/total-amount/{memberId}")
    public ResponseEntity<Map<String, Object>> getTotalAmountByMemberId(@PathVariable Long memberId) {
        log.debug("Get total amount by member ID request received: {}", memberId);
        
        BigDecimal totalAmount = orderService.getTotalAmountByMemberId(memberId);
        Map<String, Object> response = Map.of(
            "memberId", memberId,
            "totalAmount", totalAmount
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 상태별 주문 수 조회
     * 
     * GET /orders/stats/count/{status}
     */
    @GetMapping("/stats/count/{status}")
    public ResponseEntity<Map<String, Object>> getOrderCountByStatus(@PathVariable Order.OrderStatus status) {
        log.debug("Get order count by status request received: {}", status);
        
        long count = orderService.getOrderCountByStatus(status);
        Map<String, Object> response = Map.of(
            "status", status,
            "count", count
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 헬스 체크
     * 
     * GET /orders/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("Order service health check requested");
        
        Map<String, String> response = Map.of(
            "status", "UP",
            "service", "order-service"
        );
        return ResponseEntity.ok(response);
    }
}


