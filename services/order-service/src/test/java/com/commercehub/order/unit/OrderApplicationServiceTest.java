package com.commercehub.order.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercehub.order.api.dto.CancelOrderRequest;
import com.commercehub.order.api.dto.CreateOrderRequest;
import com.commercehub.order.api.dto.OrderItemRequest;
import com.commercehub.order.api.dto.OrderResponse;
import com.commercehub.order.application.OrderApplicationService;
import com.commercehub.order.application.OrderCalculator;
import com.commercehub.order.application.OrderCancellationService;
import com.commercehub.order.application.OrderStateTransitionService;
import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.entity.OrderItemEntity;
import com.commercehub.order.domain.enumtype.OrderStatus;
import com.commercehub.order.exception.DuplicateProductInOrderException;
import com.commercehub.order.exception.UnknownProductException;
import com.commercehub.order.infrastructure.client.ProductClient;
import com.commercehub.order.infrastructure.client.ProductSnapshotResponse;
import com.commercehub.order.infrastructure.messaging.KafkaTopics;
import com.commercehub.order.infrastructure.messaging.outbox.OutboxWriter;
import com.commercehub.order.infrastructure.messaging.payload.OrderCancelledPayload;
import com.commercehub.order.infrastructure.messaging.payload.OrderConfirmedPayload;
import com.commercehub.order.infrastructure.messaging.payload.ReleaseInventoryPayload;
import com.commercehub.order.infrastructure.persistence.OrderRepository;
import com.commercehub.order.mapper.OrderMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
        verify(outboxWriter, never()).writeEvent(any(), any(), any(), any(), any());
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

    @Test
    void confirmWritesExactlyOneOrderConfirmed() {
        OrderEntity order = persistedOrder(OrderStatus.CREATED);
        when(repository.findByIdOptional(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = service.confirm(order.getId());

        assertThat(response.status()).isEqualTo("CONFIRMED");
        ArgumentCaptor<OrderConfirmedPayload> payload = ArgumentCaptor.forClass(OrderConfirmedPayload.class);
        verify(outboxWriter).writeEvent(
                eq("OrderConfirmed"), eq(KafkaTopics.ORDER_EVENTS), eq(order.getId()), isNull(), payload.capture());
        assertThat(payload.getValue().orderId()).isEqualTo(order.getId());
        assertThat(payload.getValue().items()).hasSize(1);
        assertThat(payload.getValue().items().getFirst().quantity()).isEqualTo(2L);
    }

    @Test
    void cancelCreatedOrderDoesNotReleaseInventory() {
        OrderEntity order = persistedOrder(OrderStatus.CREATED);
        when(repository.findByIdOptional(order.getId())).thenReturn(Optional.of(order));

        service.cancel(order.getId(), new CancelOrderRequest("CHANGED_MIND", null));

        ArgumentCaptor<OrderCancelledPayload> payload = ArgumentCaptor.forClass(OrderCancelledPayload.class);
        verify(outboxWriter).writeEvent(
                eq("OrderCancelled"), eq(KafkaTopics.ORDER_EVENTS), eq(order.getId()), isNull(), payload.capture());
        assertThat(payload.getValue().previousStatus()).isEqualTo("CREATED");
        verify(outboxWriter, never()).writeCommand(any(), any(), any(), any(), any());
    }

    @Test
    void cancelInventoryReservedWritesCompensationCommand() {
        OrderEntity order = persistedOrder(OrderStatus.INVENTORY_RESERVED);
        when(repository.findByIdOptional(order.getId())).thenReturn(Optional.of(order));

        service.cancel(order.getId(), new CancelOrderRequest("CHANGED_MIND", null));

        verify(outboxWriter).writeEvent(eq("OrderCancelled"), eq(KafkaTopics.ORDER_EVENTS), eq(order.getId()), isNull(), any());
        ArgumentCaptor<ReleaseInventoryPayload> command = ArgumentCaptor.forClass(ReleaseInventoryPayload.class);
        verify(outboxWriter).writeCommand(
                eq("ReleaseInventory"), eq(KafkaTopics.INVENTORY_COMMANDS), eq(order.getId()), isNull(), command.capture());
        assertThat(command.getValue().reason()).isEqualTo(ReleaseInventoryPayload.REASON_CUSTOMER_CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    private static OrderEntity persistedOrder(OrderStatus status) {
        OrderEntity order = OrderEntity.newOrder();
        order.setCustomerId("11111111-1111-1111-1111-111111111111");
        order.setCurrencyCode("BRL");
        order.setTotalAmount(new BigDecimal("21.0000"));
        order.setStatus(status);

        OrderItemEntity item = OrderItemEntity.newItem();
        item.setProductId(UUID.randomUUID().toString());
        item.setSku("KB-001");
        item.setProductName("Keyboard");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("10.5000"));
        item.setLineTotal(new BigDecimal("21.0000"));
        order.addItem(item);
        return order;
    }
}
