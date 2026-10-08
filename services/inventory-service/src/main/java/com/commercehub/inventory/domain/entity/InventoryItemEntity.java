package com.commercehub.inventory.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Stock balance for one product: {@code availableQty} can be reserved, {@code reservedQty} is held by
 * confirmed orders until released. The Inventory Service is the sole owner of this table.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItemEntity {

    @Id
    @Column(name = "inventory_item_id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "product_id", length = 36, nullable = false, updatable = false)
    private String productId;

    @Column(name = "available_qty", nullable = false)
    private long availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private long reservedQty;

    @Version
    @Column(name = "version_no", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected InventoryItemEntity() {
    }

    public static InventoryItemEntity newItem(String productId, long availableQty) {
        InventoryItemEntity entity = new InventoryItemEntity();
        entity.id = UUID.randomUUID().toString();
        entity.productId = productId;
        entity.availableQty = availableQty;
        entity.reservedQty = 0;
        return entity;
    }

    public void reserve(long quantity) {
        if (quantity > availableQty) {
            throw new IllegalArgumentException("Cannot reserve more than available");
        }
        this.availableQty -= quantity;
        this.reservedQty += quantity;
    }

    public void release(long quantity) {
        long effective = Math.min(quantity, reservedQty);
        this.reservedQty -= effective;
        this.availableQty += effective;
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public long getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(long availableQty) {
        this.availableQty = availableQty;
    }

    public long getReservedQty() {
        return reservedQty;
    }

    public long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
