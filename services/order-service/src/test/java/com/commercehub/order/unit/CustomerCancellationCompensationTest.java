package com.commercehub.order.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercehub.order.api.dto.CancelOrderRequest;
import com.commercehub.order.application.OrderCancellationService;
import com.commercehub.order.application.OrderStateTransitionService;
import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.enumtype.CancellationReason;
import com.commercehub.order.domain.enumtype.CancelledBy;
import com.commercehub.order.domain.enumtype.OrderStatus;
import org.junit.jupiter.api.Test;

/**
 * Customer cancellation after a successful reservation is allowed and must leave the order in a state
 * that the application service can compensate with {@code ReleaseInventory}.
 */
class CustomerCancellationCompensationTest {

    private final OrderCancellationService cancellation =
            new OrderCancellationService(new OrderStateTransitionService());

    @Test
    void customerCanCancelAfterInventoryIsReserved() {
        OrderEntity order = OrderEntity.newOrder();
        order.setStatus(OrderStatus.INVENTORY_RESERVED);

        cancellation.cancelByCustomer(order, new CancelOrderRequest("CHANGED_MIND", null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo(CancellationReason.CHANGED_MIND);
        assertThat(order.getCancelledBy()).isEqualTo(CancelledBy.CUSTOMER);
        assertThat(order.getCancelledAt()).isNotNull();
    }
}
