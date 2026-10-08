package com.commercehub.product.api;

import com.commercehub.product.api.dto.CreateProductRequest;
import com.commercehub.product.api.dto.PageResponse;
import com.commercehub.product.api.dto.ProductResponse;
import com.commercehub.product.api.dto.UpdateProductRequest;
import com.commercehub.product.application.ProductService;
import com.commercehub.product.exception.ProblemDetail;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/v1/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Products", description = "Product catalog")
public class ProductResource {

    private final ProductService service;

    public ProductResource(ProductService service) {
        this.service = service;
    }

    @POST
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Create a product")
    @APIResponse(responseCode = "201", description = "Product created",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "SKU already exists",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public RestResponse<ProductResponse> create(@Valid @NotNull CreateProductRequest request, UriInfo uriInfo) {
        ProductResponse created = service.create(request);
        return RestResponse.ResponseBuilder
                .<ProductResponse>created(uriInfo.getAbsolutePathBuilder().path(created.id()).build())
                .entity(created)
                .build();
    }

    @GET
    @PermitAll
    @Operation(summary = "List products", description = "Paginated, ordered by name. Filters are optional. Public.")
    @APIResponse(responseCode = "200", description = "Page of products")
    public PageResponse<ProductResponse> list(
            @Parameter(description = "Filter by category code", example = "PERIPHERALS") @QueryParam("category") String category,
            @Parameter(description = "Filter by active flag") @QueryParam("active") Boolean active,
            @Parameter(description = "Zero-based page index") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size (1-100)") @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return service.search(category, active, page, size);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Get a product by id")
    @APIResponse(responseCode = "200", description = "Product found",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @APIResponse(responseCode = "404", description = "Product not found",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public ProductResponse get(@PathParam("id") String id) {
        return service.findById(id);
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Replace a product", description = "Uses optimistic locking: send the current `version`.")
    @APIResponse(responseCode = "200", description = "Product updated",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "Product not found",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "SKU already exists or version is stale",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public ProductResponse update(@PathParam("id") String id, @Valid @NotNull UpdateProductRequest request) {
        return service.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Deactivate a product",
            description = "Soft delete: the product stays readable with `active=false` so existing orders keep a valid reference.")
    @APIResponse(responseCode = "204", description = "Product deactivated")
    @APIResponse(responseCode = "404", description = "Product not found",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public Response delete(@PathParam("id") String id) {
        service.deactivate(id);
        return Response.noContent().build();
    }
}
