package com.commercehub.order.application;

import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.enumtype.OrderStatus;
import com.commercehub.order.exception.InvalidOrderStateException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderStateTransitionService {

    public void confirm(OrderEntity order) {
        transition(order, OrderStatus.CONFIRMED);
    }

    public void transition(OrderEntity order, OrderStatus target) {
        OrderStatus current = order.getStatus();
        if (!isAllowed(current, target)) {
            throw InvalidOrderStateException.transition(current, target);
        }
        order.setStatus(target);
    }

    public boolean isAllowed(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case CREATED -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.INVENTORY_RESERVED || to == OrderStatus.CANCELLED;
            case INVENTORY_RESERVED -> to == OrderStatus.PAYMENT_PENDING
                    || to == OrderStatus.CANCELLED
                    || to == OrderStatus.COMPLETED;
            case PAYMENT_PENDING -> to == OrderStatus.PAYMENT_APPROVED || to == OrderStatus.CANCELLED;
            case PAYMENT_APPROVED -> to == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
