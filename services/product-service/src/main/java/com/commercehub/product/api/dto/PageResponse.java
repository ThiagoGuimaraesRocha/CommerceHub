package com.commercehub.product.api.dto;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ProductPage")
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {
}
