package com.commercehub.order.application;

import com.commercehub.order.domain.Money;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;

@ApplicationScoped
public class OrderCalculator {

    public BigDecimal lineTotal(BigDecimal unitPrice, long quantity) {
        return Money.normalize(Money.normalize(unitPrice).multiply(BigDecimal.valueOf(quantity)));
    }

    public BigDecimal orderTotal(Iterable<BigDecimal> lineTotals) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal lineTotal : lineTotals) {
            total = total.add(lineTotal);
        }
        return Money.normalize(total);
    }
}
