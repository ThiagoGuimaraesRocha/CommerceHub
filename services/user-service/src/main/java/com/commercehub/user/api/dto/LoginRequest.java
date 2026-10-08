package com.commercehub.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "LoginRequest")
public record LoginRequest(
        @Schema(examples = "customer@commercehub.local")
        @NotBlank @Email @Size(max = 320)
        String email,

        @Schema(examples = "********")
        @NotBlank @Size(max = 72)
        String password) {
}
