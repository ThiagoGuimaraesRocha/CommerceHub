package com.commercehub.inventory.application;

import com.commercehub.inventory.api.dto.InventoryResponse;
import com.commercehub.inventory.domain.entity.InventoryItemEntity;
import com.commercehub.inventory.exception.InventoryItemNotFoundException;
import com.commercehub.inventory.infrastructure.persistence.InventoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Administrative stock operations. Setting stock is idempotent: it creates the record on first use and
 * adjusts the available quantity afterwards (reserved quantity is never touched here).
 */
@ApplicationScoped
public class InventoryService {

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public InventoryResponse setStock(String productId, long availableQuantity) {
        InventoryItemEntity item = repository.findByProductId(productId).orElse(null);
        if (item == null) {
            item = InventoryItemEntity.newItem(productId, availableQuantity);
            repository.persist(item);
        } else {
            item.setAvailableQty(availableQuantity);
        }
        return toResponse(item);
    }

    public InventoryResponse getStock(String productId) {
        InventoryItemEntity item = repository.findByProductId(productId)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId));
        return toResponse(item);
    }

    private static InventoryResponse toResponse(InventoryItemEntity item) {
        return new InventoryResponse(
                item.getProductId(),
                item.getAvailableQty(),
                item.getReservedQty(),
                item.getVersion(),
                utc(item.getCreatedAt()),
                utc(item.getUpdatedAt()));
    }

    private static OffsetDateTime utc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
