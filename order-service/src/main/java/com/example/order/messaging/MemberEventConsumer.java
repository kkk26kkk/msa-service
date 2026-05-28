package com.example.order.messaging;

import com.example.order.event.MemberCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemberEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MemberEventConsumer.class);

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = "${app.kafka.topics.member-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeMemberCreated(MemberCreatedEvent event) {
        if (event == null || event.eventId() == null) {
            log.warn("Ignored invalid member created event: {}", event);
            return;
        }

        if (!processedEventIds.add(event.eventId())) {
            log.debug("Skipped duplicated member created event: eventId={}", event.eventId());
            return;
        }

        log.info("Consumed member created event: eventId={}, memberId={}, username={}",
                event.eventId(),
                event.payload() != null ? event.payload().memberId() : null,
                event.payload() != null ? event.payload().username() : null);
    }
}
