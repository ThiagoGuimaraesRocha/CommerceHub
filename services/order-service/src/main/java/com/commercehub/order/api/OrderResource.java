package com.commercehub.order.api;

import com.commercehub.order.api.dto.CancelOrderRequest;
import com.commercehub.order.api.dto.CancellationReasonResponse;
import com.commercehub.order.api.dto.CreateOrderRequest;
import com.commercehub.order.api.dto.OrderResponse;
import com.commercehub.order.application.OrderApplicationService;
import com.commercehub.order.exception.ProblemDetail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/v1/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Orders", description = "Order lifecycle")
public class OrderResource {

    private final OrderApplicationService service;

    public OrderResource(OrderApplicationService service) {
        this.service = service;
    }

    @POST
    @Operation(summary = "Create an order", description = "Validates products via Product Service and snapshots price, SKU and name.")
    @APIResponse(responseCode = "201", description = "Order created",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request or unknown/inactive product",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "503", description = "Product Service unavailable",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public RestResponse<OrderResponse> create(@Valid @NotNull CreateOrderRequest request, UriInfo uriInfo) {
        OrderResponse created = service.create(request);
        return RestResponse.ResponseBuilder
                .<OrderResponse>created(uriInfo.getAbsolutePathBuilder().path(created.id()).build())
                .entity(created)
                .build();
    }

    @GET
    @Operation(summary = "List orders by customer")
    @APIResponse(responseCode = "200", description = "Orders for the customer")
    public List<OrderResponse> listByCustomer(
            @Parameter(description = "Customer id", required = true)
            @QueryParam("customerId") @NotBlank String customerId) {
        return service.findByCustomerId(customerId);
    }

    @GET
    @Path("/cancellation-reasons")
    @Operation(summary = "List customer-selectable cancellation reasons")
    @APIResponse(responseCode = "200", description = "Selectable reasons")
    public List<CancellationReasonResponse> cancellationReasons() {
        return service.cancellationReasons();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an order by id")
    @APIResponse(responseCode = "200", description = "Order found",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @APIResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public OrderResponse get(@PathParam("id") String id) {
        return service.findById(id);
    }

    @POST
    @Path("/{id}/confirm")
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Confirm an order", description = "Validates CREATED -> CONFIRMED. Event publishing arrives in Sprint 4.")
    @APIResponse(responseCode = "200", description = "Order confirmed",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @APIResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Invalid state transition",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public OrderResponse confirm(@PathParam("id") String id) {
        return service.confirm(id);
    }

    @POST
    @Path("/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Customer cancellation while CREATED (or INVENTORY_RESERVED from Sprint 4).")
    @APIResponse(responseCode = "200", description = "Order cancelled",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid reason or missing note",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Invalid state for cancellation",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public OrderResponse cancel(@PathParam("id") String id, @Valid @NotNull CancelOrderRequest request) {
        return service.cancel(id, request);
    }
}
