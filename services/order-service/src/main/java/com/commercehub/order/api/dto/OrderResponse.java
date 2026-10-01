package com.commercehub.order.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "Order")
public record OrderResponse(
        @Schema(examples = "17c6cbc4-1865-41bc-b0e7-a99d9d49f572") String id,
        @Schema(examples = "11111111-1111-1111-1111-111111111111") String customerId,
        @Schema(examples = "CREATED") String status,
        @Schema(description = "Order total with scale 4.", examples = "699.8000") BigDecimal totalAmount,
        @Schema(examples = "BRL") String currencyCode,
        String cancellationReason,
        String cancellationNote,
        String cancelledBy,
        OffsetDateTime cancelledAt,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<OrderItemResponse> items) {
}
