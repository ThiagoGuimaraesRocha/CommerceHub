package com.commercehub.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "SetStockRequest")
public record SetStockRequest(
        @Schema(description = "Absolute available quantity to set for the product.", examples = "50")
        @NotNull @Min(0)
        Long availableQuantity) {
}
