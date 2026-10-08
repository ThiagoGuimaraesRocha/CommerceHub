package com.commercehub.order.application;

import com.commercehub.order.api.dto.CancelOrderRequest;
import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.enumtype.CancellationReason;
import com.commercehub.order.domain.enumtype.CancelledBy;
import com.commercehub.order.domain.enumtype.OrderStatus;
import com.commercehub.order.exception.InvalidOrderStateException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ApplicationScoped
public class OrderCancellationService {

    private final OrderStateTransitionService transitions;

    public OrderCancellationService(OrderStateTransitionService transitions) {
        this.transitions = transitions;
    }

    public void cancelByCustomer(OrderEntity order, CancelOrderRequest request) {
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.CONFIRMED) {
            throw InvalidOrderStateException.awaitingInventory(order.getId());
        }
        if (status != OrderStatus.CREATED && status != OrderStatus.INVENTORY_RESERVED) {
            throw InvalidOrderStateException.cancelNotAllowed(status);
        }

        CancellationReason reason = CancellationReason.fromCode(request.reasonCode());
        if (reason == null || !reason.isCustomerSelectable()) {
            throw new BadRequestException("Cancellation reason is unknown or not selectable by the customer");
        }
        String note = normalizeNote(request.note());
        if (reason.isNoteRequired() && (note == null || note.isBlank())) {
            throw new BadRequestException("Cancellation note is required when reasonCode is OTHER");
        }
        if (!reason.isNoteRequired()) {
            note = null;
        }

        transitions.transition(order, OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancellationNote(note);
        order.setCancelledBy(CancelledBy.CUSTOMER);
        order.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * System-driven cancellation used by the saga (Sprint 4). Kept here so the rules live in one place.
     */
    public void cancelBySystem(OrderEntity order, CancellationReason reason) {
        if (reason == null || reason.isCustomerSelectable()) {
            throw new IllegalArgumentException("System cancellation requires a system reason code");
        }
        transitions.transition(order, OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancellationNote(null);
        order.setCancelledBy(CancelledBy.SYSTEM);
        order.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
