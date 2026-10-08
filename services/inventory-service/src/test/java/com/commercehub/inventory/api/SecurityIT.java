package com.commercehub.inventory.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SecurityIT {

    private static final String INVENTORY = "/api/v1/inventory";

    @Test
    void missingTokenIs401() {
        given().when().get(INVENTORY + "/" + UUID.randomUUID())
                .then()
                .statusCode(401)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:unauthorized"));
    }

    @Test
    @TestSecurity(user = "customer-user", roles = "CUSTOMER")
    void customerCannotReadOrWriteStock() {
        String productId = UUID.randomUUID().toString();
        given().contentType(ContentType.JSON)
                .body(Map.of("availableQuantity", 10))
                .when().put(INVENTORY + "/" + productId)
                .then()
                .statusCode(403)
                .body("type", equalTo("urn:commercehub:problem:forbidden"));

        given().when().get(INVENTORY + "/" + productId)
                .then()
                .statusCode(403);
    }
}
