package com.commercehub.order.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(OtelTestProfile.class)
class TracePropagationIT {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String TRACEPARENT = "00-" + TRACE_ID + "-00f067aa0ba902b7-01";

    @Test
    void incomingW3cTraceparentContinuesTheHttpSpan() {
        given()
                .header("traceparent", TRACEPARENT)
                .when().get("/__otel/span")
                .then()
                .statusCode(200)
                .body("valid", equalTo(true))
                .body("traceId", equalTo(TRACE_ID));
    }

    @Test
    void httpRequestWithoutIncomingContextStillCreatesASpan() {
        given()
                .when().get("/__otel/span")
                .then()
                .statusCode(200)
                .body("valid", equalTo(true));
    }
}
