package com.commercehub.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateOrderRequest")
public record CreateOrderRequest(
        @Schema(description = "Customer id. Temporary body field until Sprint 5 JWT.", examples = "11111111-1111-1111-1111-111111111111")
        @NotBlank @Size(max = 36)
        String customerId,

        @Schema(description = "Order lines. Each product may appear only once.")
        @NotNull @NotEmpty
        List<@Valid OrderItemRequest> items) {
}
