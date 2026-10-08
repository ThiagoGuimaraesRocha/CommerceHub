package com.commercehub.order.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CancellationReason")
public record CancellationReasonResponse(
        @Schema(examples = "CHANGED_MIND") String code,
        @Schema(examples = "Changed my mind") String label,
        @Schema(description = "Whether a free-text note is required.") boolean noteRequired) {
}
