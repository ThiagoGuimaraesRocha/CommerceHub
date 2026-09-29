package com.commercehub.product.api.dto;

final class ProductConstraints {

    static final String SKU_PATTERN = "^[A-Z0-9][A-Z0-9-]*[A-Z0-9]$";
    static final String SKU_MESSAGE = "must contain only uppercase letters, digits and inner hyphens";

    static final String CATEGORY_PATTERN = "^[A-Z][A-Z0-9_]*$";
    static final String CATEGORY_MESSAGE = "must be an uppercase code (letters, digits and underscores)";

    static final String CURRENCY_PATTERN = "^[A-Z]{3}$";
    static final String CURRENCY_MESSAGE = "must be a 3-letter ISO 4217 code";

    private ProductConstraints() {
    }
}
