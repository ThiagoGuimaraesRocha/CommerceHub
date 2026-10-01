package com.commercehub.order.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CancelOrderRequest")
public record CancelOrderRequest(
        @Schema(description = "Customer-selectable cancellation reason code.", examples = "CHANGED_MIND")
        @NotBlank @Size(max = 40)
        String reasonCode,

        @Schema(description = "Required when reasonCode is OTHER. Max 500 characters.", examples = "I no longer need it")
        @Size(max = 500)
        String note) {
}
