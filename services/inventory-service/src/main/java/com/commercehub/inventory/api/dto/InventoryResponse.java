package com.commercehub.inventory.api.dto;

import java.time.OffsetDateTime;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "Inventory")
public record InventoryResponse(
        @Schema(examples = "9d8c7b6a-1111-4222-8333-444455556666") String productId,
        @Schema(examples = "48") long availableQuantity,
        @Schema(examples = "2") long reservedQuantity,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
