package com.example.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.consumer.backoff-ms}") long backOffMs,
            @Value("${app.kafka.consumer.max-retry-attempts}") long maxRetryAttempts,
            @Value("${app.kafka.topics.member-created-dlt}") String memberCreatedDltTopic
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(defaultErrorHandler(
                kafkaTemplate,
                backOffMs,
                maxRetryAttempts,
                memberCreatedDltTopic
        ));
        return factory;
    }

    @Bean
    public DefaultErrorHandler defaultErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            long backOffMs,
            long maxRetryAttempts,
            String memberCreatedDltTopic
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    log.error("Routing failed record to DLT: topic={}, key={}, error={}",
                            record.topic(), record.key(), ex.getMessage());
                    return new TopicPartition(memberCreatedDltTopic, record.partition());
                }
        );

        return new DefaultErrorHandler(recoverer, new FixedBackOff(backOffMs, maxRetryAttempts));
    }

    @Bean
    public NewTopic memberCreatedTopic(@Value("${app.kafka.topics.member-created}") String name) {
        return new NewTopic(name, 1, (short) 1);
    }

    @Bean
    public NewTopic memberCreatedDltTopic(@Value("${app.kafka.topics.member-created-dlt}") String name) {
        return new NewTopic(name, 1, (short) 1);
    }

    @Bean
    public NewTopic orderCreatedTopic(@Value("${app.kafka.topics.order-created}") String name) {
        return new NewTopic(name, 1, (short) 1);
    }

    @Bean
    public NewTopic orderStatusChangedTopic(@Value("${app.kafka.topics.order-status-changed}") String name) {
        return new NewTopic(name, 1, (short) 1);
    }
}
