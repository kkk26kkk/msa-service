package com.example.order.messaging;

import com.example.order.entity.Order;
import com.example.order.event.OrderCreatedEvent;
import com.example.order.event.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String orderCreatedTopic;
    private final String orderStatusChangedTopic;

    public OrderEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.order-created}") String orderCreatedTopic,
            @Value("${app.kafka.topics.order-status-changed}") String orderStatusChangedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderCreatedTopic = orderCreatedTopic;
        this.orderStatusChangedTopic = orderStatusChangedTopic;
    }

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.of(order);
        kafkaTemplate.send(orderCreatedTopic, String.valueOf(order.getId()), event);
        log.info("Published order created event: orderId={}, eventId={}", order.getId(), event.eventId());
    }

    public void publishOrderStatusChanged(Order order, String previousStatus) {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.of(order, previousStatus);
        kafkaTemplate.send(orderStatusChangedTopic, String.valueOf(order.getId()), event);
        log.info("Published order status changed event: orderId={}, eventId={}", order.getId(), event.eventId());
    }
}
