package com.example.member.messaging;

import com.example.member.entity.Member;
import com.example.member.event.MemberCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberEventPublisher 테스트")
class MemberEventPublisherTest {

    @Mock
    private KafkaTemplate<String, MemberCreatedEvent> kafkaTemplate;

    @Test
    @DisplayName("회원 생성 이벤트 발행")
    void publishMemberCreated() {
        MemberEventPublisher memberEventPublisher = new MemberEventPublisher(kafkaTemplate, "member.created.v1");

        Member member = Member.builder()
                .id(1L)
                .username("user1")
                .email("user1@test.com")
                .fullName("사용자1")
                .status(Member.MemberStatus.ACTIVE)
                .build();

        memberEventPublisher.publishMemberCreated(member);

        verify(kafkaTemplate, times(1))
                .send(ArgumentMatchers.eq("member.created.v1"), ArgumentMatchers.eq("1"), ArgumentMatchers.any(MemberCreatedEvent.class));
    }
}
