package com.example.order.event;

import java.time.LocalDateTime;

public record MemberCreatedEvent(
        String eventId,
        String eventType,
        LocalDateTime occurredAt,
        String version,
        Payload payload
) {
    public record Payload(
            Long memberId,
            String username,
            String email,
            String fullName,
            String status
    ) {
    }
}
