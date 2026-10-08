package com.commercehub.inventory.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercehub.inventory.application.ReservationOutcome;
import com.commercehub.inventory.application.ReservationRequest;
import com.commercehub.inventory.application.StockReservationService;
import com.commercehub.inventory.domain.entity.InventoryItemEntity;
import com.commercehub.inventory.domain.entity.StockReservationEntity;
import com.commercehub.inventory.domain.enumtype.ReservationStatus;
import com.commercehub.inventory.infrastructure.persistence.InventoryRepository;
import com.commercehub.inventory.infrastructure.persistence.StockReservationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    @Mock
    InventoryRepository inventoryRepository;

    @Mock
    StockReservationRepository reservationRepository;

    StockReservationService service;

    @BeforeEach
    void setUp() {
        service = new StockReservationService(inventoryRepository, reservationRepository);
    }

    @Test
    void reserveAllOrNothingOnSuccess() {
        String p1 = UUID.randomUUID().toString();
        String p2 = UUID.randomUUID().toString();
        InventoryItemEntity item1 = InventoryItemEntity.newItem(p1, 10);
        InventoryItemEntity item2 = InventoryItemEntity.newItem(p2, 5);
        when(inventoryRepository.findByProductId(p1)).thenReturn(Optional.of(item1));
        when(inventoryRepository.findByProductId(p2)).thenReturn(Optional.of(item2));

        ReservationOutcome outcome = service.reserve("order-1", List.of(
                new ReservationRequest(p1, 3),
                new ReservationRequest(p2, 2)));

        assertThat(outcome.reserved()).isTrue();
        assertThat(item1.getAvailableQty()).isEqualTo(7);
        assertThat(item1.getReservedQty()).isEqualTo(3);
        assertThat(item2.getAvailableQty()).isEqualTo(3);
        assertThat(item2.getReservedQty()).isEqualTo(2);
        verify(reservationRepository, times(2)).persist(any(StockReservationEntity.class));
    }

    @Test
    void insufficientStockReservesNothing() {
        String p1 = UUID.randomUUID().toString();
        String p2 = UUID.randomUUID().toString();
        InventoryItemEntity item1 = InventoryItemEntity.newItem(p1, 10);
        InventoryItemEntity item2 = InventoryItemEntity.newItem(p2, 1);
        when(inventoryRepository.findByProductId(p1)).thenReturn(Optional.of(item1));
        when(inventoryRepository.findByProductId(p2)).thenReturn(Optional.of(item2));

        ReservationOutcome outcome = service.reserve("order-1", List.of(
                new ReservationRequest(p1, 3),
                new ReservationRequest(p2, 2)));

        assertThat(outcome.reserved()).isFalse();
        assertThat(outcome.failureReason()).isEqualTo(ReservationOutcome.REASON_INSUFFICIENT_STOCK);
        assertThat(item1.getAvailableQty()).isEqualTo(10);
        assertThat(item1.getReservedQty()).isZero();
        assertThat(item2.getAvailableQty()).isEqualTo(1);
        verify(reservationRepository, never()).persist(any(StockReservationEntity.class));
    }

    @Test
    void unknownProductReservesNothing() {
        String known = UUID.randomUUID().toString();
        String missing = UUID.randomUUID().toString();
        when(inventoryRepository.findByProductId(known)).thenReturn(Optional.of(InventoryItemEntity.newItem(known, 10)));
        when(inventoryRepository.findByProductId(missing)).thenReturn(Optional.empty());

        ReservationOutcome outcome = service.reserve("order-1", List.of(
                new ReservationRequest(known, 1),
                new ReservationRequest(missing, 1)));

        assertThat(outcome.reserved()).isFalse();
        assertThat(outcome.failureReason()).isEqualTo(ReservationOutcome.REASON_UNKNOWN_PRODUCT);
        verify(reservationRepository, never()).persist(any(StockReservationEntity.class));
    }

    @Test
    void releaseMovesReservedBackToAvailable() {
        String productId = UUID.randomUUID().toString();
        InventoryItemEntity item = InventoryItemEntity.newItem(productId, 10);
        item.reserve(2);
        StockReservationEntity reservation = StockReservationEntity.reserved("order-1", productId, 2);
        when(reservationRepository.findReservedByOrderId("order-1")).thenReturn(List.of(reservation));
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item));

        List<ReservationOutcome.ReservedLine> released = service.release("order-1");

        assertThat(released).hasSize(1);
        assertThat(item.getAvailableQty()).isEqualTo(10);
        assertThat(item.getReservedQty()).isZero();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void releaseIsIdempotentWhenNothingIsReserved() {
        when(reservationRepository.findReservedByOrderId("order-1")).thenReturn(List.of());

        assertThat(service.release("order-1")).isEmpty();
        verify(inventoryRepository, never()).findByProductId(any());
    }
}
