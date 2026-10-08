package com.commercehub.order.mapper;

import com.commercehub.order.api.dto.OrderItemResponse;
import com.commercehub.order.api.dto.OrderResponse;
import com.commercehub.order.domain.Money;
import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.entity.OrderItemEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@ApplicationScoped
public class OrderMapper {

    public OrderResponse toResponse(OrderEntity entity) {
        return new OrderResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus().name(),
                Money.normalize(entity.getTotalAmount()),
                entity.getCurrencyCode(),
                entity.getCancellationReason() == null ? null : entity.getCancellationReason().name(),
                entity.getCancellationNote(),
                entity.getCancelledBy() == null ? null : entity.getCancelledBy().name(),
                utc(entity.getCancelledAt()),
                entity.getVersion(),
                utc(entity.getCreatedAt()),
                utc(entity.getUpdatedAt()),
                entity.getItems().stream().map(this::toItemResponse).toList());
    }

    public OrderItemResponse toItemResponse(OrderItemEntity item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getSku(),
                item.getProductName(),
                item.getQuantity(),
                Money.normalize(item.getUnitPrice()),
                Money.normalize(item.getLineTotal()));
    }

    public List<OrderResponse> toResponses(List<OrderEntity> entities) {
        return entities.stream().map(this::toResponse).toList();
    }

    private static OffsetDateTime utc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
