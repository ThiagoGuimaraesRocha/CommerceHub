package com.commercehub.order.api.dto;

import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "OrderItem")
public record OrderItemResponse(
        @Schema(examples = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee") String id,
        @Schema(examples = "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c") String productId,
        @Schema(examples = "KB-MECH-001") String sku,
        @Schema(examples = "Mechanical Keyboard") String productName,
        long quantity,
        @Schema(description = "Unit price snapshot with scale 4.", examples = "349.9000") BigDecimal unitPrice,
        @Schema(description = "Line total with scale 4.", examples = "699.8000") BigDecimal lineTotal) {
}
