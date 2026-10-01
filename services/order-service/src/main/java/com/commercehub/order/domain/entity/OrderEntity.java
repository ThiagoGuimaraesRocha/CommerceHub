package com.commercehub.order.domain.entity;

import com.commercehub.order.domain.enumtype.CancellationReason;
import com.commercehub.order.domain.enumtype.CancelledBy;
import com.commercehub.order.domain.enumtype.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(name = "order_id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "customer_id", length = 36, nullable = false, updatable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 40)
    private CancellationReason cancellationReason;

    @Column(name = "cancellation_note", length = 500)
    private String cancellationNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 20)
    private CancelledBy cancelledBy;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Version
    @Column(name = "version_no", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<OrderItemEntity> items = new ArrayList<>();

    public static OrderEntity newOrder() {
        OrderEntity entity = new OrderEntity();
        entity.id = UUID.randomUUID().toString();
        entity.status = OrderStatus.CREATED;
        return entity;
    }

    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public CancellationReason getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(CancellationReason cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCancellationNote() {
        return cancellationNote;
    }

    public void setCancellationNote(String cancellationNote) {
        this.cancellationNote = cancellationNote;
    }

    public CancelledBy getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(CancelledBy cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
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

    public List<OrderItemEntity> getItems() {
        return items;
    }
}
