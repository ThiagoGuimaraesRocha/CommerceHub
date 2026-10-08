package com.commercehub.inventory.application;

import com.commercehub.inventory.domain.entity.InventoryItemEntity;
import com.commercehub.inventory.domain.entity.StockReservationEntity;
import com.commercehub.inventory.infrastructure.persistence.InventoryRepository;
import com.commercehub.inventory.infrastructure.persistence.StockReservationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reserves and releases stock. Reservation is all-or-nothing: if any line is unknown or short, nothing is
 * reserved. Methods run inside the saga transaction so the stock change and the outbox message commit together.
 */
@ApplicationScoped
public class StockReservationService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;

    public StockReservationService(InventoryRepository inventoryRepository,
            StockReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    public ReservationOutcome reserve(String orderId, List<ReservationRequest> items) {
        Map<String, InventoryItemEntity> available = new LinkedHashMap<>();
        List<ReservationOutcome.UnavailableLine> unavailable = new ArrayList<>();
        boolean anyUnknown = false;

        for (ReservationRequest item : items) {
            InventoryItemEntity inventory = inventoryRepository.findByProductId(item.productId()).orElse(null);
            if (inventory == null) {
                anyUnknown = true;
                unavailable.add(new ReservationOutcome.UnavailableLine(item.productId(), item.quantity(), 0));
            } else if (inventory.getAvailableQty() < item.quantity()) {
                unavailable.add(new ReservationOutcome.UnavailableLine(
                        item.productId(), item.quantity(), inventory.getAvailableQty()));
            } else {
                available.put(item.productId(), inventory);
            }
        }

        if (!unavailable.isEmpty()) {
            String reason = anyUnknown
                    ? ReservationOutcome.REASON_UNKNOWN_PRODUCT
                    : ReservationOutcome.REASON_INSUFFICIENT_STOCK;
            return ReservationOutcome.failed(reason, unavailable);
        }

        List<ReservationOutcome.ReservedLine> reservations = new ArrayList<>();
        for (ReservationRequest item : items) {
            InventoryItemEntity inventory = available.get(item.productId());
            inventory.reserve(item.quantity());
            StockReservationEntity reservation =
                    StockReservationEntity.reserved(orderId, item.productId(), item.quantity());
            reservationRepository.persist(reservation);
            reservations.add(new ReservationOutcome.ReservedLine(
                    reservation.getId(), item.productId(), item.quantity()));
        }
        return ReservationOutcome.reserved(reservations);
    }

    /**
     * Releases every still-reserved line of an order. Returns the released lines, or an empty list when
     * there was nothing to release (already released or never reserved) so the caller can stay idempotent.
     */
    public List<ReservationOutcome.ReservedLine> release(String orderId) {
        List<StockReservationEntity> reserved = reservationRepository.findReservedByOrderId(orderId);
        List<ReservationOutcome.ReservedLine> released = new ArrayList<>();
        for (StockReservationEntity reservation : reserved) {
            inventoryRepository.findByProductId(reservation.getProductId())
                    .ifPresent(inventory -> inventory.release(reservation.getQuantity()));
            reservation.markReleased();
            released.add(new ReservationOutcome.ReservedLine(
                    reservation.getId(), reservation.getProductId(), reservation.getQuantity()));
        }
        return released;
    }
}
