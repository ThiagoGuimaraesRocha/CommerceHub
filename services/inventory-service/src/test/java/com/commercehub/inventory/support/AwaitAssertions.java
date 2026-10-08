package com.commercehub.inventory.support;

import java.util.concurrent.TimeUnit;

public final class AwaitAssertions {

    private AwaitAssertions() {
    }

    public static void untilAsserted(Runnable assertion) {
        untilAsserted(15, assertion);
    }

    public static void untilAsserted(int timeoutSeconds, Runnable assertion) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        AssertionError last = new AssertionError("condition was not met");
        while (System.nanoTime() < deadline) {
            try {
                assertion.run();
                return;
            } catch (AssertionError e) {
                last = e;
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("interrupted while waiting", ie);
                }
            }
        }
        throw last;
    }
}
