package com.commercehub.user.api;

import com.commercehub.user.api.dto.LoginRequest;
import com.commercehub.user.api.dto.TokenResponse;
import com.commercehub.user.api.dto.UserResponse;
import com.commercehub.user.application.JwtService;
import com.commercehub.user.application.UserService;
import com.commercehub.user.domain.entity.UserEntity;
import com.commercehub.user.exception.ProblemDetail;
import com.commercehub.user.mapper.UserMapper;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Demonstration login. Issues a JWT signed for this environment only.")
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final UserMapper mapper;

    public AuthResource(UserService userService, JwtService jwtService, UserMapper mapper) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Login", description = "Returns a Bearer JWT. Demonstration only — not a corporate IdP.")
    @APIResponse(responseCode = "200", description = "Authenticated",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(mediaType = ProblemDetail.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    public TokenResponse login(@Valid @NotNull LoginRequest request) {
        UserEntity user = userService.authenticate(request.email(), request.password());
        String token = jwtService.issue(user);
        UserResponse body = mapper.toResponse(user);
        LOG.infof("User %s signed in", user.getId());
        return new TokenResponse(token, "Bearer", jwtService.lifespanSeconds(), body);
    }
}
