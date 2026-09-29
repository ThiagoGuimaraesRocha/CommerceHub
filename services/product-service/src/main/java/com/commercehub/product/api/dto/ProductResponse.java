package com.commercehub.product.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "Product")
public record ProductResponse(
        @Schema(examples = "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c") String id,
        @Schema(examples = "KB-MECH-001") String sku,
        @Schema(examples = "Mechanical Keyboard") String name,
        String description,
        @Schema(examples = "PERIPHERALS") String categoryCode,
        @Schema(description = "Always serialized with 4 decimal places.", examples = "349.9000") BigDecimal price,
        @Schema(examples = "BRL") String currencyCode,
        boolean active,
        @Schema(description = "Optimistic locking version; send it back on PUT.") long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
