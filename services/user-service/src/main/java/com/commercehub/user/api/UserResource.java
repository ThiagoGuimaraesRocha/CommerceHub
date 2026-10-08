package com.commercehub.user.api;

import com.commercehub.user.api.dto.CreateUserRequest;
import com.commercehub.user.api.dto.UserResponse;
import com.commercehub.user.application.UserService;
import com.commercehub.user.exception.ProblemDetail;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "User directory. Writes require ADMIN.")
public class UserResource {

    private final UserService service;
    private final SecurityIdentity identity;

    public UserResource(UserService service, SecurityIdentity identity) {
        this.service = service;
        this.identity = identity;
    }

    @POST
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Create a user")
    @APIResponse(responseCode = "201", description = "User created",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "403", description = "Requires ADMIN",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Email already exists",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public RestResponse<UserResponse> create(@Valid @NotNull CreateUserRequest request, UriInfo uriInfo) {
        UserResponse created = service.create(request);
        return RestResponse.ResponseBuilder
                .<UserResponse>created(uriInfo.getAbsolutePathBuilder().path(created.id()).build())
                .entity(created)
                .build();
    }

    @GET
    @Path("/me")
    @Authenticated
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Get the authenticated user")
    @APIResponse(responseCode = "200", description = "Current user",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @APIResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public UserResponse me() {
        return service.findById(identity.getPrincipal().getName());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Get a user by id")
    @APIResponse(responseCode = "200", description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @APIResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "403", description = "Requires ADMIN",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "User not found",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public UserResponse get(@PathParam("id") String id) {
        return service.findById(id);
    }
}
