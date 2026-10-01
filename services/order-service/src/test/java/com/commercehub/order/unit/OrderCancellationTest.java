package com.commercehub.order.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercehub.order.api.dto.CancelOrderRequest;
import com.commercehub.order.application.OrderCancellationService;
import com.commercehub.order.application.OrderStateTransitionService;
import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.enumtype.CancellationReason;
import com.commercehub.order.domain.enumtype.CancelledBy;
import com.commercehub.order.domain.enumtype.OrderStatus;
import com.commercehub.order.exception.InvalidOrderStateException;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderCancellationTest {

    OrderCancellationService cancellation;

    @BeforeEach
    void setUp() {
        cancellation = new OrderCancellationService(new OrderStateTransitionService());
    }

    @Test
    void customerCanCancelCreatedOrder() {
        OrderEntity order = OrderEntity.newOrder();
        cancellation.cancelByCustomer(order, new CancelOrderRequest("CHANGED_MIND", null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo(CancellationReason.CHANGED_MIND);
        assertThat(order.getCancelledBy()).isEqualTo(CancelledBy.CUSTOMER);
        assertThat(order.getCancelledAt()).isNotNull();
        assertThat(order.getCancellationNote()).isNull();
    }

    @Test
    void otherRequiresNote() {
        OrderEntity order = OrderEntity.newOrder();
        assertThatThrownBy(() -> cancellation.cancelByCustomer(order, new CancelOrderRequest("OTHER", "  ")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("note");
    }

    @Test
    void systemReasonIsRejectedForCustomer() {
        OrderEntity order = OrderEntity.newOrder();
        assertThatThrownBy(() -> cancellation.cancelByCustomer(order, new CancelOrderRequest("INSUFFICIENT_STOCK", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void confirmedCannotBeCancelledByCustomer() {
        OrderEntity order = OrderEntity.newOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> cancellation.cancelByCustomer(order, new CancelOrderRequest("CHANGED_MIND", null)))
                .isInstanceOf(InvalidOrderStateException.class)
                .extracting("type").isEqualTo("order-awaiting-inventory");
    }
}
