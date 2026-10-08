package com.commercehub.inventory.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.List;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ConstraintViolationHandler {

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> handle(ConstraintViolationException exception, UriInfo uriInfo) {
        List<ProblemDetail.Violation> violations = exception.getConstraintViolations().stream()
                .map(v -> new ProblemDetail.Violation(fieldName(v), v.getMessage()))
                .sorted(Comparator.comparing(ProblemDetail.Violation::field).thenComparing(ProblemDetail.Violation::message))
                .toList();
        return RestResponse.ResponseBuilder
                .create(RestResponse.Status.BAD_REQUEST, ProblemDetail.validation(ApiExceptionHandler.instance(uriInfo), violations))
                .type(ProblemDetail.MEDIA_TYPE)
                .build();
    }

    private static String fieldName(ConstraintViolation<?> violation) {
        String name = "";
        for (Path.Node node : violation.getPropertyPath()) {
            name = node.getName();
        }
        return name;
    }
}
