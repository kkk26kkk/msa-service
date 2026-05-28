package com.example.member.event;

import com.example.member.entity.Member;

import java.time.LocalDateTime;

public record MemberCreatedEvent(
        String eventId,
        String eventType,
        LocalDateTime occurredAt,
        String version,
        Payload payload
) {
    public static final String TYPE = "MemberCreatedEvent";
    public static final String VERSION = "v1";

    public static MemberCreatedEvent of(Member member) {
        return new MemberCreatedEvent(
                java.util.UUID.randomUUID().toString(),
                TYPE,
                LocalDateTime.now(),
                VERSION,
                new Payload(
                        member.getId(),
                        member.getUsername(),
                        member.getEmail(),
                        member.getFullName(),
                        member.getStatus().name()
                )
        );
    }

    public record Payload(
            Long memberId,
            String username,
            String email,
            String fullName,
            String status
    ) {
    }
}
