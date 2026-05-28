package com.example.order.event;

import com.example.order.entity.Order;

import java.time.LocalDateTime;

public record OrderStatusChangedEvent(
        String eventId,
        String eventType,
        LocalDateTime occurredAt,
        String version,
        Payload payload
) {
    public static final String TYPE = "OrderStatusChangedEvent";
    public static final String VERSION = "v1";

    public static OrderStatusChangedEvent of(Order order, String previousStatus) {
        return new OrderStatusChangedEvent(
                java.util.UUID.randomUUID().toString(),
                TYPE,
                LocalDateTime.now(),
                VERSION,
                new Payload(
                        order.getId(),
                        order.getMemberId(),
                        previousStatus,
                        order.getStatus().name()
                )
        );
    }

    public record Payload(
            Long orderId,
            Long memberId,
            String previousStatus,
            String currentStatus
    ) {
    }
}
