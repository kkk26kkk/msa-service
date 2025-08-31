package com.example.order.dto;

import com.example.order.entity.Order;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 DTO 클래스들
 * 
 * API 요청/응답을 위한 데이터 전송 객체들입니다.
 */
public class OrderDto {

    /**
     * 주문 생성 요청 DTO
     */
    public static class CreateRequest {
        
        @NotNull(message = "회원 ID는 필수입니다")
        private Long memberId;

        @NotBlank(message = "상품명은 필수입니다")
        @Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다")
        private String productName;

        @NotNull(message = "수량은 필수입니다")
        private Integer quantity;

        @NotNull(message = "단가는 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "단가는 0보다 커야 합니다")
        private BigDecimal unitPrice;

        @Size(max = 500, message = "주문 메모는 500자를 초과할 수 없습니다")
        private String orderMemo;

        // 기본 생성자
        public CreateRequest() {}

        // 전체 생성자
        public CreateRequest(Long memberId, String productName, Integer quantity, 
                           BigDecimal unitPrice, String orderMemo) {
            this.memberId = memberId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.orderMemo = orderMemo;
        }

        // Getter/Setter
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public String getOrderMemo() { return orderMemo; }
        public void setOrderMemo(String orderMemo) { this.orderMemo = orderMemo; }

        /**
         * DTO를 Entity로 변환
         * 총 금액은 자동 계산됩니다.
         */
        public Order toEntity() {
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            
            return Order.builder()
                    .memberId(memberId)
                    .productName(productName)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .totalAmount(totalAmount)
                    .status(Order.OrderStatus.PENDING)
                    .orderMemo(orderMemo)
                    .build();
        }

        @Override
        public String toString() {
            return "CreateRequest{" +
                    "memberId=" + memberId +
                    ", productName='" + productName + '\'' +
                    ", quantity=" + quantity +
                    ", unitPrice=" + unitPrice +
                    ", orderMemo='" + orderMemo + '\'' +
                    '}';
        }
    }

    /**
     * 주문 수정 요청 DTO
     */
    public static class UpdateRequest {
        
        private Integer quantity;
        private BigDecimal unitPrice;
        private Order.OrderStatus status;
        private String orderMemo;

        // 기본 생성자
        public UpdateRequest() {}

        // 전체 생성자
        public UpdateRequest(Integer quantity, BigDecimal unitPrice, 
                           Order.OrderStatus status, String orderMemo) {
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.status = status;
            this.orderMemo = orderMemo;
        }

        // Getter/Setter
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public Order.OrderStatus getStatus() { return status; }
        public void setStatus(Order.OrderStatus status) { this.status = status; }

        public String getOrderMemo() { return orderMemo; }
        public void setOrderMemo(String orderMemo) { this.orderMemo = orderMemo; }

        @Override
        public String toString() {
            return "UpdateRequest{" +
                    "quantity=" + quantity +
                    ", unitPrice=" + unitPrice +
                    ", status=" + status +
                    ", orderMemo='" + orderMemo + '\'' +
                    '}';
        }
    }

    /**
     * 주문 응답 DTO
     */
    public static class Response {
        
        private Long id;
        private Long memberId;
        private String memberName;  // Member Service에서 조회한 회원명
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private Order.OrderStatus status;
        private String statusDescription;
        private String orderMemo;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;

        // 기본 생성자
        public Response() {}

        // 전체 생성자
        public Response(Long id, Long memberId, String memberName, String productName, 
                       Integer quantity, BigDecimal unitPrice, BigDecimal totalAmount, 
                       Order.OrderStatus status, String statusDescription, String orderMemo,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.memberId = memberId;
            this.memberName = memberName;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalAmount = totalAmount;
            this.status = status;
            this.statusDescription = statusDescription;
            this.orderMemo = orderMemo;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        /**
         * Entity를 DTO로 변환 (회원명 없이)
         */
        public static Response from(Order order) {
            return new Response(
                order.getId(),
                order.getMemberId(),
                null,  // 회원명은 별도로 설정
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getStatus().getDescription(),
                order.getOrderMemo(),
                order.getCreatedAt(),
                order.getUpdatedAt()
            );
        }

        /**
         * Entity를 DTO로 변환 (회원명 포함)
         */
        public static Response from(Order order, String memberName) {
            Response response = from(order);
            response.setMemberName(memberName);
            return response;
        }

        // Getter/Setter
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }

        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public Order.OrderStatus getStatus() { return status; }
        public void setStatus(Order.OrderStatus status) { this.status = status; }

        public String getStatusDescription() { return statusDescription; }
        public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }

        public String getOrderMemo() { return orderMemo; }
        public void setOrderMemo(String orderMemo) { this.orderMemo = orderMemo; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        @Override
        public String toString() {
            return "Response{" +
                    "id=" + id +
                    ", memberId=" + memberId +
                    ", memberName='" + memberName + '\'' +
                    ", productName='" + productName + '\'' +
                    ", quantity=" + quantity +
                    ", unitPrice=" + unitPrice +
                    ", totalAmount=" + totalAmount +
                    ", status=" + status +
                    ", statusDescription='" + statusDescription + '\'' +
                    ", orderMemo='" + orderMemo + '\'' +
                    ", createdAt=" + createdAt +
                    ", updatedAt=" + updatedAt +
                    '}';
        }
    }

    /**
     * 주문 요약 DTO (목록 조회용)
     */
    public static class Summary {
        
        private Long id;
        private Long memberId;
        private String memberName;
        private String productName;
        private Integer quantity;
        private BigDecimal totalAmount;
        private Order.OrderStatus status;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        // 기본 생성자
        public Summary() {}

        // 전체 생성자
        public Summary(Long id, Long memberId, String memberName, String productName, 
                      Integer quantity, BigDecimal totalAmount, Order.OrderStatus status, 
                      LocalDateTime createdAt) {
            this.id = id;
            this.memberId = memberId;
            this.memberName = memberName;
            this.productName = productName;
            this.quantity = quantity;
            this.totalAmount = totalAmount;
            this.status = status;
            this.createdAt = createdAt;
        }

        /**
         * Entity를 Summary DTO로 변환
         */
        public static Summary from(Order order) {
            return new Summary(
                order.getId(),
                order.getMemberId(),
                null,  // 회원명은 별도로 설정
                order.getProductName(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt()
            );
        }

        /**
         * Entity를 Summary DTO로 변환 (회원명 포함)
         */
        public static Summary from(Order order, String memberName) {
            Summary summary = from(order);
            summary.setMemberName(memberName);
            return summary;
        }

        // Getter/Setter
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }

        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public Order.OrderStatus getStatus() { return status; }
        public void setStatus(Order.OrderStatus status) { this.status = status; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        @Override
        public String toString() {
            return "Summary{" +
                    "id=" + id +
                    ", memberId=" + memberId +
                    ", memberName='" + memberName + '\'' +
                    ", productName='" + productName + '\'' +
                    ", quantity=" + quantity +
                    ", totalAmount=" + totalAmount +
                    ", status=" + status +
                    ", createdAt=" + createdAt +
                    '}';
        }
    }
}


