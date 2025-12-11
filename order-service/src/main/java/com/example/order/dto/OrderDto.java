package com.example.order.dto;

import com.example.order.entity.Order;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

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
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
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
    }

    /**
     * 주문 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class UpdateRequest {
        
        private Integer quantity;
        private BigDecimal unitPrice;
        private Order.OrderStatus status;
        private String orderMemo;
    }

    /**
     * 주문 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
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

        /**
         * Entity를 DTO로 변환 (회원명 없이)
         */
        public static Response from(Order order) {
            return Response.builder()
                    .id(order.getId())
                    .memberId(order.getMemberId())
                    .memberName(null)  // 회원명은 별도로 설정
                    .productName(order.getProductName())
                    .quantity(order.getQuantity())
                    .unitPrice(order.getUnitPrice())
                    .totalAmount(order.getTotalAmount())
                    .status(order.getStatus())
                    .statusDescription(order.getStatus().getDescription())
                    .orderMemo(order.getOrderMemo())
                    .createdAt(order.getCreatedAt())
                    .updatedAt(order.getUpdatedAt())
                    .build();
        }

        /**
         * Entity를 DTO로 변환 (회원명 포함)
         */
        public static Response from(Order order, String memberName) {
            Response response = from(order);
            response.setMemberName(memberName);
            return response;
        }
    }

    /**
     * 주문 요약 DTO (목록 조회용)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
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

        /**
         * Entity를 Summary DTO로 변환
         */
        public static Summary from(Order order) {
            return Summary.builder()
                    .id(order.getId())
                    .memberId(order.getMemberId())
                    .memberName(null)  // 회원명은 별도로 설정
                    .productName(order.getProductName())
                    .quantity(order.getQuantity())
                    .totalAmount(order.getTotalAmount())
                    .status(order.getStatus())
                    .createdAt(order.getCreatedAt())
                    .build();
        }

        /**
         * Entity를 Summary DTO로 변환 (회원명 포함)
         */
        public static Summary from(Order order, String memberName) {
            Summary summary = from(order);
            summary.setMemberName(memberName);
            return summary;
        }
    }
}


