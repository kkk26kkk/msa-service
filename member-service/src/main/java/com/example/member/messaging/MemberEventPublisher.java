package com.example.member.messaging;

import com.example.member.entity.Member;
import com.example.member.event.MemberCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MemberEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MemberEventPublisher.class);

    private final KafkaTemplate<String, MemberCreatedEvent> kafkaTemplate;
    private final String memberCreatedTopic;

    public MemberEventPublisher(
            KafkaTemplate<String, MemberCreatedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.member-created}") String memberCreatedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.memberCreatedTopic = memberCreatedTopic;
    }

    public void publishMemberCreated(Member member) {
        MemberCreatedEvent event = MemberCreatedEvent.of(member);
        kafkaTemplate.send(memberCreatedTopic, String.valueOf(member.getId()), event);
        log.info("Published member created event: memberId={}, eventId={}", member.getId(), event.eventId());
    }
}
