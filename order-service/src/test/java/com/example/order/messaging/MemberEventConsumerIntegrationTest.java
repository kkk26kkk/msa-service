package com.example.order.messaging;

import com.example.order.OrderServiceApplication;
import com.example.order.event.MemberCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest(
        classes = OrderServiceApplication.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.fail-fast=false",
                "eureka.client.enabled=false",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "member.created.v1",
                "member.created.v1.DLT"
        }
)
@DisplayName("MemberEventConsumer Kafka 통합 테스트")
class MemberEventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @SpyBean
    private MemberEventConsumer memberEventConsumer;

    @Test
    @DisplayName("MemberCreatedEvent 수신")
    void consumeMemberCreatedEvent() {
        MemberCreatedEvent event = new MemberCreatedEvent(
                UUID.randomUUID().toString(),
                "MemberCreatedEvent",
                LocalDateTime.now(),
                "v1",
                new MemberCreatedEvent.Payload(1L, "user1", "user1@test.com", "사용자1", "ACTIVE")
        );

        kafkaTemplate.send("member.created.v1", "1", event);
        kafkaTemplate.flush();

        Mockito.verify(memberEventConsumer, Mockito.timeout(5000).times(1)).consumeMemberCreated(Mockito.any());
    }
}
