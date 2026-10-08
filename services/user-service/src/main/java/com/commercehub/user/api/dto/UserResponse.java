package com.commercehub.user.api.dto;

import java.time.OffsetDateTime;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "UserResponse")
public record UserResponse(
        String id,
        String email,
        String fullName,
        String roleCode,
        boolean active,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
