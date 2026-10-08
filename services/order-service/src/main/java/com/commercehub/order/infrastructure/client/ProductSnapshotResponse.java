package com.commercehub.order.infrastructure.client;

import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Subset of the Product Service response used as a purchase-time snapshot.
 */
@Schema(name = "ProductSnapshot")
public record ProductSnapshotResponse(
        String id,
        String sku,
        String name,
        BigDecimal price,
        String currencyCode,
        boolean active) {
}
