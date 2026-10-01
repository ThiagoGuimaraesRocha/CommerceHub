package com.commercehub.order.domain.enumtype;

import java.util.Arrays;
import java.util.List;

public enum CancellationReason {
    CHANGED_MIND("Changed my mind", true, false),
    ORDERED_BY_MISTAKE("Ordered by mistake", true, false),
    FOUND_BETTER_PRICE("Found a better price", true, false),
    DELIVERY_TIME_TOO_LONG("Delivery time too long", true, false),
    OTHER("Other", true, true),
    INSUFFICIENT_STOCK("Insufficient stock", false, false),
    UNKNOWN_PRODUCT("Unknown product", false, false),
    PAYMENT_FAILED("Payment failed", false, false);

    private final String label;
    private final boolean customerSelectable;
    private final boolean noteRequired;

    CancellationReason(String label, boolean customerSelectable, boolean noteRequired) {
        this.label = label;
        this.customerSelectable = customerSelectable;
        this.noteRequired = noteRequired;
    }

    public String label() {
        return label;
    }

    public boolean isCustomerSelectable() {
        return customerSelectable;
    }

    public boolean isNoteRequired() {
        return noteRequired;
    }

    public static List<CancellationReason> customerSelectableReasons() {
        return Arrays.stream(values()).filter(CancellationReason::isCustomerSelectable).toList();
    }

    public static CancellationReason fromCode(String code) {
        try {
            return CancellationReason.valueOf(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
