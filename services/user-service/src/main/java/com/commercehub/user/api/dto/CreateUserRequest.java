package com.commercehub.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateUserRequest")
public record CreateUserRequest(
        @Schema(examples = "customer@commercehub.local")
        @NotBlank @Email @Size(max = 320)
        String email,

        @Schema(description = "Plain password. Stored only as a bcrypt hash. 8–72 characters.")
        @NotBlank @Size(min = 8, max = 72)
        String password,

        @Schema(examples = "Ana Customer")
        @NotBlank @Size(max = 150)
        String fullName,

        @Schema(description = "CUSTOMER or ADMIN", examples = "CUSTOMER")
        @NotBlank @Pattern(regexp = "CUSTOMER|ADMIN")
        String roleCode) {
}
