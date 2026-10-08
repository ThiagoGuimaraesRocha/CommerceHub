package com.commercehub.inventory.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * RFC 9457 Problem Details body, served as {@code application/problem+json}.
 */
@Schema(name = "ProblemDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        @Schema(examples = "urn:commercehub:problem:inventory-item-not-found") String type,
        @Schema(examples = "Not Found") String title,
        @Schema(examples = "404") int status,
        @Schema(examples = "No inventory for product ...") String detail,
        @Schema(examples = "/api/v1/inventory/...") String instance,
        List<Violation> violations) {

    public static final String MEDIA_TYPE = "application/problem+json";
    private static final String TYPE_BASE = "urn:commercehub:problem:";

    public static ProblemDetail of(String type, String title, int status, String detail, String instance) {
        return new ProblemDetail(TYPE_BASE + type, title, status, detail, instance, null);
    }

    public static ProblemDetail validation(String instance, List<Violation> violations) {
        return new ProblemDetail(TYPE_BASE + "validation-error", "Bad Request", 400,
                "The request contains invalid fields", instance, violations);
    }

    @Schema(name = "Violation")
    public record Violation(
            @Schema(examples = "availableQuantity") String field,
            @Schema(examples = "must be greater than or equal to 0") String message) {
    }
}
