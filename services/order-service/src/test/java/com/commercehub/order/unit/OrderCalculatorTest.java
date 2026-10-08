package com.commercehub.order.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercehub.order.application.OrderCalculator;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderCalculatorTest {

    private final OrderCalculator calculator = new OrderCalculator();

    @Test
    void lineTotalMultipliesAndNormalizesScale() {
        assertThat(calculator.lineTotal(new BigDecimal("10.5"), 2)).hasToString("21.0000");
    }

    @Test
    void orderTotalSumsLineTotals() {
        assertThat(calculator.orderTotal(List.of(new BigDecimal("10.5000"), new BigDecimal("3.2500"))))
                .hasToString("13.7500");
    }
}
