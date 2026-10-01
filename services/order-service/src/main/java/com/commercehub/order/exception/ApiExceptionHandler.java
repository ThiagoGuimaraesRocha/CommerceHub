package com.commercehub.order.exception;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ApiExceptionHandler {

    private static final Logger LOG = Logger.getLogger(ApiExceptionHandler.class);

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> notFound(OrderNotFoundException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("order-not-found", "Not Found", 404, exception.getMessage(), instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> invalidState(InvalidOrderStateException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of(exception.type(), "Conflict", 409, exception.getMessage(), instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> unknownProduct(UnknownProductException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("unknown-product", "Bad Request", 400, exception.getMessage(), instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> duplicateProduct(DuplicateProductInOrderException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("duplicate-product-in-order", "Bad Request", 400, exception.getMessage(), instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> currencyMismatch(CurrencyMismatchException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("currency-mismatch", "Bad Request", 400, exception.getMessage(), instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> productUnavailable(RemoteProductServiceException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("product-service-unavailable", "Service Unavailable", 503,
                "Product Service is unavailable", instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> badRequest(BadRequestException exception, UriInfo uriInfo) {
        return problem(ProblemDetail.of("bad-request", "Bad Request", 400, exception.getMessage(), instance(uriInfo)));
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> webApplication(WebApplicationException exception, UriInfo uriInfo) {
        Response.StatusType status = exception.getResponse().getStatusInfo();
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
