package com.example.order.service;

import com.example.order.client.MemberServiceClient;
import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.messaging.OrderEventPublisher;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 이벤트 발행 테스트")
class OrderServiceEventPublisherTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MemberIntegrationService memberIntegrationService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성 시 OrderCreatedEvent 발행")
    void createOrder_PublishesOrderCreatedEvent() {
        OrderDto.CreateRequest request = OrderDto.CreateRequest.builder()
                .memberId(1L)
                .productName("상품A")
                .quantity(2)
                .unitPrice(new BigDecimal("1000"))
                .orderMemo("테스트 주문")
                .build();

        MemberServiceClient.MemberDto memberDto = new MemberServiceClient.MemberDto(
                1L, "user1", "user1@test.com", "사용자1", "010-0000-0000", "ACTIVE", "활성"
        );

        Order savedOrder = request.toEntity();
        savedOrder.setId(10L);

        when(memberIntegrationService.validateMember(1L)).thenReturn(memberDto);
        when(orderRepository.save(ArgumentMatchers.any(Order.class))).thenReturn(savedOrder);

        orderService.createOrder(request);

        verify(orderEventPublisher, times(1)).publishOrderCreated(savedOrder);
    }

    @Test
    @DisplayName("주문 상태 변경 시 OrderStatusChangedEvent 발행")
    void updateOrder_PublishesOrderStatusChangedEvent() {
        Order order = Order.builder()
                .id(20L)
                .memberId(1L)
                .productName("상품B")
                .quantity(1)
                .unitPrice(new BigDecimal("3000"))
                .totalAmount(new BigDecimal("3000"))
                .status(Order.OrderStatus.PENDING)
                .build();

        OrderDto.UpdateRequest request = OrderDto.UpdateRequest.builder()
                .status(Order.OrderStatus.SHIPPED)
                .build();

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(orderRepository.save(ArgumentMatchers.any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberIntegrationService.getMemberName(1L)).thenReturn("사용자1");

        orderService.updateOrder(20L, request);

        verify(orderEventPublisher, times(1))
                .publishOrderStatusChanged(ArgumentMatchers.any(Order.class), ArgumentMatchers.eq("PENDING"));
    }
}
