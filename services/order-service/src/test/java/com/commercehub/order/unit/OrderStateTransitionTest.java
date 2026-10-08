package com.commercehub.order.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercehub.order.application.OrderStateTransitionService;
import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.enumtype.OrderStatus;
import com.commercehub.order.exception.InvalidOrderStateException;
import org.junit.jupiter.api.Test;

class OrderStateTransitionTest {

    private final OrderStateTransitionService transitions = new OrderStateTransitionService();

    @Test
    void confirmsCreatedOrder() {
        OrderEntity order = OrderEntity.newOrder();
        transitions.confirm(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void rejectsInvalidConfirm() {
        OrderEntity order = OrderEntity.newOrder();
        order.setStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> transitions.confirm(order))
                .isInstanceOf(InvalidOrderStateException.class)
                .extracting("type").isEqualTo("invalid-order-state");
    }

    @Test
    void allowsCreatedToCancelled() {
        assertThat(transitions.isAllowed(OrderStatus.CREATED, OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void allowsConfirmedToInventoryReserved() {
        assertThat(transitions.isAllowed(OrderStatus.CONFIRMED, OrderStatus.INVENTORY_RESERVED)).isTrue();
    }

    @Test
    void allowsInventoryReservedToCancelled() {
        assertThat(transitions.isAllowed(OrderStatus.INVENTORY_RESERVED, OrderStatus.CANCELLED)).isTrue();
    }
}
