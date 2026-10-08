package com.commercehub.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateOrderRequest")
public record CreateOrderRequest(
        @Schema(description = "Order lines. Each product may appear only once.")
        @NotNull @NotEmpty
        List<@Valid OrderItemRequest> items) {
}
