package com.example.order.event;

import com.example.order.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedEvent(
        String eventId,
        String eventType,
        LocalDateTime occurredAt,
        String version,
        Payload payload
) {
    public static final String TYPE = "OrderCreatedEvent";
    public static final String VERSION = "v1";

    public static OrderCreatedEvent of(Order order) {
        return new OrderCreatedEvent(
                java.util.UUID.randomUUID().toString(),
                TYPE,
                LocalDateTime.now(),
                VERSION,
                new Payload(
                        order.getId(),
                        order.getMemberId(),
                        order.getProductName(),
                        order.getQuantity(),
                        order.getUnitPrice(),
                        order.getTotalAmount(),
                        order.getStatus().name()
                )
        );
    }

    public record Payload(
            Long orderId,
            Long memberId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            String status
    ) {
    }
}
