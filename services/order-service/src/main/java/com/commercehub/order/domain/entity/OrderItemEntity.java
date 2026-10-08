package com.commercehub.order.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @Column(name = "order_item_id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "product_id", length = 36, nullable = false, updatable = false)
    private String productId;

    @Column(name = "sku", length = 64, nullable = false, updatable = false)
    private String sku;

    @Column(name = "product_name", length = 150, nullable = false, updatable = false)
    private String productName;

    @Column(name = "quantity", nullable = false, updatable = false)
    private long quantity;

    @Column(name = "unit_price", precision = 19, scale = 4, nullable = false, updatable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 19, scale = 4, nullable = false, updatable = false)
    private BigDecimal lineTotal;

    public static OrderItemEntity newItem() {
        OrderItemEntity entity = new OrderItemEntity();
        entity.id = UUID.randomUUID().toString();
        return entity;
    }

    public String getId() {
        return id;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
