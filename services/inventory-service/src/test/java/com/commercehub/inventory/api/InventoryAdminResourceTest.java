package com.commercehub.inventory.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(user = "admin-user", roles = "ADMIN")
class InventoryAdminResourceTest {

    private static final String INVENTORY = "/api/v1/inventory";

    @Test
    void putCreatesAndGetReturnsStock() {
        String productId = UUID.randomUUID().toString();

        given().contentType(ContentType.JSON)
                .body(Map.of("availableQuantity", 50))
                .when().put(INVENTORY + "/" + productId)
                .then()
                .statusCode(200)
                .body("productId", equalTo(productId))
                .body("availableQuantity", equalTo(50))
                .body("reservedQuantity", equalTo(0));

        given().when().get(INVENTORY + "/" + productId)
                .then()
                .statusCode(200)
                .body("availableQuantity", equalTo(50));
    }

    @Test
    void putIsIdempotentAndAdjustsAvailable() {
        String productId = UUID.randomUUID().toString();
        given().contentType(ContentType.JSON).body(Map.of("availableQuantity", 10))
                .when().put(INVENTORY + "/" + productId).then().statusCode(200);

        given().contentType(ContentType.JSON).body(Map.of("availableQuantity", 25))
                .when().put(INVENTORY + "/" + productId)
                .then()
                .statusCode(200)
                .body("availableQuantity", equalTo(25));
    }

    @Test
    void getUnknownProductReturns404() {
        given().when().get(INVENTORY + "/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:inventory-item-not-found"));
    }

    @Test
    void negativeQuantityReturns400() {
        given().contentType(ContentType.JSON)
                .body(Map.of("availableQuantity", -1))
                .when().put(INVENTORY + "/" + UUID.randomUUID())
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:validation-error"))
                .body("violations.field", hasItem("availableQuantity"));
    }
}
