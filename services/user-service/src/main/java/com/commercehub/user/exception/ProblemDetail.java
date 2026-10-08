package com.commercehub.user.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ProblemDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        String instance,
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
    public record Violation(String field, String message) {
    }
}
