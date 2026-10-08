package com.commercehub.inventory.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MetricsEndpointTest {

    @Test
    void prometheusMetricsArePublic() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .contentType(containsString("openmetrics-text"))
                .body(containsString("jvm_"))
                .body(containsString("commercehub_outbox_events"));
    }
}
