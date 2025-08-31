package com.example.order.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 엔터티
 * 
 * 주문의 기본 정보를 관리하는 JPA 엔터티입니다.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다")
    private String productName;

    @Column(nullable = false)
    @NotNull(message = "수량은 필수입니다")
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "단가는 필수입니다")
    @DecimalMin(value = "0.0", inclusive = false, message = "단가는 0보다 커야 합니다")
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    @NotNull(message = "총 금액은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = false, message = "총 금액은 0보다 커야 합니다")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(length = 500)
    @Size(max = 500, message = "주문 메모는 500자를 초과할 수 없습니다")
    private String orderMemo;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 기본 생성자
    public Order() {}

    // 전체 생성자
    public Order(Long id, Long memberId, String productName, Integer quantity, 
                 BigDecimal unitPrice, BigDecimal totalAmount, OrderStatus status, 
                 String orderMemo, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.memberId = memberId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : OrderStatus.PENDING;
        this.orderMemo = orderMemo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Builder 패턴을 위한 정적 메서드
    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    // Getter 메서드들
    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public String getOrderMemo() { return orderMemo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setter 메서드들
    public void setId(Long id) { this.id = id; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setOrderMemo(String orderMemo) { this.orderMemo = orderMemo; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", memberId=" + memberId +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                ", orderMemo='" + orderMemo + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // Builder 클래스
    public static class OrderBuilder {
        private Long id;
        private Long memberId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private OrderStatus status = OrderStatus.PENDING;
        private String orderMemo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public OrderBuilder id(Long id) { this.id = id; return this; }
        public OrderBuilder memberId(Long memberId) { this.memberId = memberId; return this; }
        public OrderBuilder productName(String productName) { this.productName = productName; return this; }
        public OrderBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public OrderBuilder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public OrderBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public OrderBuilder status(OrderStatus status) { this.status = status; return this; }
        public OrderBuilder orderMemo(String orderMemo) { this.orderMemo = orderMemo; return this; }
        public OrderBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public OrderBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Order build() {
            return new Order(id, memberId, productName, quantity, unitPrice, totalAmount, status, orderMemo, createdAt, updatedAt);
        }
    }

    /**
     * 주문 상태 열거형
     */
    public enum OrderStatus {
        PENDING("대기중"),
        CONFIRMED("확인완료"),
        PROCESSING("처리중"),
        SHIPPED("배송중"),
        DELIVERED("배송완료"),
        CANCELLED("취소됨"),
        REFUNDED("환불됨");

        private final String description;

        OrderStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}


