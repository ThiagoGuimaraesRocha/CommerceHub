package com.commercehub.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "OrderItemRequest")
public record OrderItemRequest(
        @Schema(description = "Product id from the Product Service.", examples = "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c")
        @NotBlank @Size(max = 36)
        String productId,

        @Schema(description = "Quantity of units.", examples = "2")
        @NotNull @Min(1)
        Long quantity) {
}
