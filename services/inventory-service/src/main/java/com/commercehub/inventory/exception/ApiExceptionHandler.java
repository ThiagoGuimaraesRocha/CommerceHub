package com.commercehub.inventory.exception;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ApiExceptionHandler {

    private static final Logger LOG = Logger.getLogger(ApiExceptionHandler.class);

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> notFound(InventoryItemNotFoundException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("inventory-item-not-found", "Not Found", 404, exception.getMessage(), instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> badRequest(BadRequestException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("bad-request", "Bad Request", 400, exception.getMessage(), instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> unauthorized(UnauthorizedException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("unauthorized", "Unauthorized", 401, "Authentication is required", instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> authenticationFailed(AuthenticationFailedException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("unauthorized", "Unauthorized", 401, "Authentication is required", instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> notAuthorized(NotAuthorizedException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("unauthorized", "Unauthorized", 401, "Authentication is required", instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> forbidden(ForbiddenException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("forbidden", "Forbidden", 403, "Insufficient permissions", instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> jaxrsForbidden(jakarta.ws.rs.ForbiddenException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("forbidden", "Forbidden", 403, "Insufficient permissions", instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> webApplication(WebApplicationException exception, UriInfo uriInfo) {
        Response.StatusType status = exception.getResponse().getStatusInfo();
        if (status.getStatusCode() == 401) {
            return problem(ProblemDetail.of("unauthorized", "Unauthorized", 401, "Authentication is required", instance(uriInfo)));
        }
        if (status.getStatusCode() == 403) {
            return problem(ProblemDetail.of("forbidden", "Forbidden", 403, "Insufficient permissions", instance(uriInfo)));
        }
        String detail = status.getStatusCode() == 400 ? "The request body is malformed or has invalid types" : exception.getMessage();
        return problem(ProblemDetail.of("http-" + status.getStatusCode(), status.getReasonPhrase(),
                status.getStatusCode(), detail, instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> unexpected(Exception exception, UriInfo uriInfo) {
        LOG.error("Unexpected error while handling " + instance(uriInfo), exception);
        return problem(ProblemDetail.of("internal-error", "Internal Server Error", 500,
                "An unexpected error occurred", instance(uriInfo)));
    }

    static String instance(UriInfo uriInfo) {
        return "/" + uriInfo.getPath().replaceFirst("^/", "");
    }

    private static RestResponse<ProblemDetail> problem(ProblemDetail body) {
        return RestResponse.ResponseBuilder
                .create(RestResponse.Status.fromStatusCode(body.status()), body)
                .type(ProblemDetail.MEDIA_TYPE)
                .build();
    }
}
