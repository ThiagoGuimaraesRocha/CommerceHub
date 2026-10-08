package com.commercehub.user.mapper;

import com.commercehub.user.api.dto.UserResponse;
import com.commercehub.user.domain.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ApplicationScoped
public class UserMapper {

    public UserResponse toResponse(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getFullName(),
                entity.getRole().name(),
                entity.isActive(),
                entity.getVersion(),
                utc(entity.getCreatedAt()),
                utc(entity.getUpdatedAt()));
    }

    private static OffsetDateTime utc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
