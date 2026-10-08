package com.commercehub.order.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;

import com.commercehub.order.infrastructure.client.ProductClient;
import com.commercehub.order.infrastructure.client.ProductSnapshotResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(user = "11111111-1111-1111-1111-111111111111", roles = "CUSTOMER")
class ObservabilityIT {

    private static final Pattern COUNTER = Pattern.compile(
            "^commercehub_orders_created_total(?:\\{[^}]*})?\\s+([0-9.]+(?:[eE][+-]?[0-9]+)?)$",
            Pattern.MULTILINE);

    @InjectMock
    @RestClient
    ProductClient productClient;

    @BeforeEach
    void resetClient() {
        reset(productClient);
        lenient().when(productClient.getById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return new ProductSnapshotResponse(id, "SKU-OBS", "Observed product",
                    new BigDecimal("10.5"), "BRL", true);
        });
    }

    @Test
    void readinessExposesOutboxCheck() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("outbox"));
    }

    @Test
    void creatingAnOrderIncrementsTheBusinessCounter() {
        double before = counterValue(given().when().get("/q/metrics").then().extract().asString());

        given().contentType(ContentType.JSON)
                .body(Map.of("items", List.of(Map.of("productId", UUID.randomUUID().toString(), "quantity", 1))))
                .when().post("/api/v1/orders")
                .then()
                .statusCode(201);

        String metrics = given().when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("commercehub_orders_created_total"))
                .extract()
                .asString();
        assertThat(counterValue(metrics)).isGreaterThan(before);
    }

    private static double counterValue(String body) {
        Matcher matcher = COUNTER.matcher(body);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0d;
    }
}
