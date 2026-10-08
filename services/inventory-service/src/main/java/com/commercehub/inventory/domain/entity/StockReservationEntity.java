package com.commercehub.inventory.domain.entity;

import com.commercehub.inventory.domain.enumtype.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * One reserved line for an order/product. A failed (all-or-nothing) attempt never creates a row, so a
 * reservation is only ever {@code RESERVED} or {@code RELEASED}.
 */
@Entity
@Table(name = "stock_reservations")
public class StockReservationEntity {

    @Id
    @Column(name = "reservation_id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "order_id", length = 36, nullable = false, updatable = false)
    private String orderId;

    @Column(name = "product_id", length = 36, nullable = false, updatable = false)
    private String productId;

    @Column(name = "quantity", nullable = false, updatable = false)
    private long quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ReservationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected StockReservationEntity() {
    }

    public static StockReservationEntity reserved(String orderId, String productId, long quantity) {
        StockReservationEntity entity = new StockReservationEntity();
        entity.id = UUID.randomUUID().toString();
        entity.orderId = orderId;
        entity.productId = productId;
        entity.quantity = quantity;
        entity.status = ReservationStatus.RESERVED;
        return entity;
    }

    public void markReleased() {
        this.status = ReservationStatus.RELEASED;
    }

    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public long getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}
