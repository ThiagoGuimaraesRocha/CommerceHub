package com.commercehub.product.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monetary values are stored as NUMBER(19,4) and always exposed with scale 4.
 */
public final class Money {

    public static final int SCALE = 4;
    public static final int INTEGER_DIGITS = 15;
    public static final String DEFAULT_CURRENCY = "BRL";

    private Money() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        return amount == null ? null : amount.setScale(SCALE, RoundingMode.HALF_EVEN);
    }
}
