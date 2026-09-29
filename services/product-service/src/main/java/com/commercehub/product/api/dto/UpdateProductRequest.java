package com.commercehub.product.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "UpdateProductRequest", description = "Full replacement of a product. `version` must match the current version.")
public record UpdateProductRequest(
        @Schema(examples = "KB-MECH-001")
        @NotBlank @Size(max = 64) @Pattern(regexp = ProductConstraints.SKU_PATTERN, message = ProductConstraints.SKU_MESSAGE)
        String sku,

        @Schema(examples = "Mechanical Keyboard")
        @NotBlank @Size(max = 150)
        String name,

        @Size(max = 1000)
        String description,

        @Schema(examples = "PERIPHERALS")
        @NotBlank @Size(max = 60) @Pattern(regexp = ProductConstraints.CATEGORY_PATTERN, message = ProductConstraints.CATEGORY_MESSAGE)
        String categoryCode,

        @Schema(examples = "329.9000")
        @NotNull @DecimalMin("0") @Digits(integer = 15, fraction = 4)
        BigDecimal price,

        @Schema(examples = "BRL")
        @Pattern(regexp = ProductConstraints.CURRENCY_PATTERN, message = ProductConstraints.CURRENCY_MESSAGE)
        String currencyCode,

        @Schema(examples = "true")
        @NotNull
        Boolean active,

        @Schema(description = "Optimistic locking version returned by the last read.", examples = "0")
        @NotNull @PositiveOrZero
        Long version) {
}
