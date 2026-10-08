package com.commercehub.user.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "TokenResponse")
public record TokenResponse(
        String accessToken,
        @Schema(examples = "Bearer") String tokenType,
        @Schema(description = "Lifetime of the access token in seconds") long expiresIn,
        UserResponse user) {
}
