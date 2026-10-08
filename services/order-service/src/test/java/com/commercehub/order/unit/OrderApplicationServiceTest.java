package com.commercehub.order.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercehub.order.api.dto.CreateOrderRequest;
import com.commercehub.order.api.dto.OrderItemRequest;
import com.commercehub.order.api.dto.OrderResponse;
import com.commercehub.order.application.OrderApplicationService;
import com.commercehub.order.application.OrderCalculator;
import com.commercehub.order.application.OrderCancellationService;
import com.commercehub.order.application.OrderStateTransitionService;
import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.exception.DuplicateProductInOrderException;
import com.commercehub.order.exception.UnknownProductException;
import com.commercehub.order.infrastructure.client.ProductClient;
import com.commercehub.order.infrastructure.client.ProductSnapshotResponse;
import com.commercehub.order.infrastructure.messaging.outbox.OutboxWriter;
import com.commercehub.order.infrastructure.persistence.OrderRepository;
import com.commercehub.order.mapper.OrderMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    OrderRepository repository;

    @Mock
    ProductClient productClient;

    @Mock
    OutboxWriter outboxWriter;

    OrderApplicationService service;

    @BeforeEach
    void setUp() {
        OrderStateTransitionService transitions = new OrderStateTransitionService();
        service = new OrderApplicationService(
                repository,
                new OrderMapper(),
                new OrderCalculator(),
                transitions,
                new OrderCancellationService(transitions),
                outboxWriter,
                productClient);
    }

    @Test
    void createSnapshotsProductAndCalculatesTotals() {
        String productId = UUID.randomUUID().toString();
        when(productClient.getById(productId)).thenReturn(new ProductSnapshotResponse(
                productId, "KB-001", "Keyboard", new BigDecimal("10.5"), "BRL", true));

        OrderResponse response = service.create(new CreateOrderRequest(
                "11111111-1111-1111-1111-111111111111",
                List.of(new OrderItemRequest(productId, 2L))));

        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.totalAmount()).hasToString("21.0000");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().sku()).isEqualTo("KB-001");
        assertThat(response.items().getFirst().unitPrice()).hasToString("10.5000");
        assertThat(response.items().getFirst().lineTotal()).hasToString("21.0000");

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(repository).persistAndFlush(captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(1);
    }

    @Test
    void createRejectsInactiveProduct() {
        String productId = UUID.randomUUID().toString();
        when(productClient.getById(productId)).thenReturn(new ProductSnapshotResponse(
                productId, "KB-001", "Keyboard", BigDecimal.ONE, "BRL", false));

        assertThatThrownBy(() -> service.create(new CreateOrderRequest(
                "11111111-1111-1111-1111-111111111111",
                List.of(new OrderItemRequest(productId, 1L)))))
                .isInstanceOf(UnknownProductException.class);
    }

    @Test
    void createRejectsDuplicateProductIds() {
        String productId = UUID.randomUUID().toString();
        assertThatThrownBy(() -> service.create(new CreateOrderRequest(
                "11111111-1111-1111-1111-111111111111",
                List.of(new OrderItemRequest(productId, 1L), new OrderItemRequest(productId, 2L)))))
                .isInstanceOf(DuplicateProductInOrderException.class);
    }
}
