package com.commercehub.product.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateProductRequest")
public record CreateProductRequest(
        @Schema(description = "Unique stock keeping unit. Uppercase letters, digits and hyphens.", examples = "KB-MECH-001")
        @NotBlank @Size(max = 64) @Pattern(regexp = ProductConstraints.SKU_PATTERN, message = ProductConstraints.SKU_MESSAGE)
        String sku,

        @Schema(examples = "Mechanical Keyboard")
        @NotBlank @Size(max = 150)
        String name,

        @Schema(examples = "Hot-swappable mechanical keyboard with brown switches.")
        @Size(max = 1000)
        String description,

        @Schema(description = "Uppercase category code.", examples = "PERIPHERALS")
        @NotBlank @Size(max = 60) @Pattern(regexp = ProductConstraints.CATEGORY_PATTERN, message = ProductConstraints.CATEGORY_MESSAGE)
        String categoryCode,

        @Schema(description = "Unit price, zero or greater, up to 4 decimal places.", examples = "349.9000")
        @NotNull @DecimalMin("0") @Digits(integer = 15, fraction = 4)
        BigDecimal price,

        @Schema(description = "ISO 4217 currency code. Defaults to BRL.", examples = "BRL")
        @Pattern(regexp = ProductConstraints.CURRENCY_PATTERN, message = ProductConstraints.CURRENCY_MESSAGE)
        String currencyCode) {
}
