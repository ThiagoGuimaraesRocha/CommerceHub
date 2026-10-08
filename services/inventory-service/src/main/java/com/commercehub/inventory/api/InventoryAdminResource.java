package com.commercehub.inventory.api;

import com.commercehub.inventory.api.dto.InventoryResponse;
import com.commercehub.inventory.api.dto.SetStockRequest;
import com.commercehub.inventory.application.InventoryService;
import com.commercehub.inventory.exception.ProblemDetail;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Administrative stock endpoint. Requires the {@code ADMIN} role.
 * Demo data is loaded through this endpoint (by {@code scripts/seed.sh}), never written to the database directly.
 */
@Path("/api/v1/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "bearer")
@Tag(name = "Inventory", description = "Administrative stock balance")
public class InventoryAdminResource {

    private final InventoryService service;

    public InventoryAdminResource(InventoryService service) {
        this.service = service;
    }

    @PUT
    @Path("/{productId}")
    @Operation(summary = "Create or adjust stock", description = "Idempotent: sets the absolute available quantity for a product.")
    @APIResponse(responseCode = "200", description = "Stock set",
            content = @Content(schema = @Schema(implementation = InventoryResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public InventoryResponse setStock(@PathParam("productId") String productId, @Valid @NotNull SetStockRequest request) {
        return service.setStock(productId, request.availableQuantity());
    }

    @GET
    @Path("/{productId}")
    @Operation(summary = "Get stock", description = "Returns available and reserved quantities for a product.")
    @APIResponse(responseCode = "200", description = "Stock found",
            content = @Content(schema = @Schema(implementation = InventoryResponse.class)))
    @APIResponse(responseCode = "404", description = "No inventory for the product",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public InventoryResponse getStock(@PathParam("productId") String productId) {
        return service.getStock(productId);
    }
}
