package com.commercehub.inventory.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercehub.inventory.api.dto.InventoryResponse;
import com.commercehub.inventory.application.InventoryService;
import com.commercehub.inventory.domain.entity.InventoryItemEntity;
import com.commercehub.inventory.exception.InventoryItemNotFoundException;
import com.commercehub.inventory.infrastructure.persistence.InventoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    InventoryRepository repository;

    InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(repository);
    }

    @Test
    void setStockCreatesWhenMissing() {
        String productId = UUID.randomUUID().toString();
        when(repository.findByProductId(productId)).thenReturn(Optional.empty());

        InventoryResponse response = service.setStock(productId, 50);

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.availableQuantity()).isEqualTo(50);
        assertThat(response.reservedQuantity()).isZero();
        ArgumentCaptor<InventoryItemEntity> captor = ArgumentCaptor.forClass(InventoryItemEntity.class);
        verify(repository).persist(captor.capture());
        assertThat(captor.getValue().getAvailableQty()).isEqualTo(50);
    }

    @Test
    void setStockAdjustsAvailableWithoutTouchingReserved() {
        String productId = UUID.randomUUID().toString();
        InventoryItemEntity existing = InventoryItemEntity.newItem(productId, 10);
        existing.reserve(4);
        when(repository.findByProductId(productId)).thenReturn(Optional.of(existing));

        InventoryResponse response = service.setStock(productId, 80);

        assertThat(response.availableQuantity()).isEqualTo(80);
        assertThat(response.reservedQuantity()).isEqualTo(4);
        verify(repository, never()).persist(any(InventoryItemEntity.class));
    }

    @Test
    void getStockThrowsWhenMissing() {
        String productId = UUID.randomUUID().toString();
        when(repository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStock(productId))
                .isInstanceOf(InventoryItemNotFoundException.class);
    }
}
