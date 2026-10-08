package com.commercehub.product.api;

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

    private static final String PRODUCTS = "/api/v1/products";

    @Test
    void catalogReadsArePublic() {
        given().when().get(PRODUCTS).then().statusCode(200);
        given().when().get(PRODUCTS + "/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void createWithoutTokenIs401() {
        given().contentType(ContentType.JSON)
                .body(product())
                .when().post(PRODUCTS)
                .then()
                .statusCode(401)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:unauthorized"));
    }

    @Test
    void invalidBearerOnWriteIs401() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer not-a-jwt")
                .body(product())
                .when().post(PRODUCTS)
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer-user", roles = "CUSTOMER")
    void customerCannotWriteTheCatalog() {
        given().contentType(ContentType.JSON)
                .body(product())
                .when().post(PRODUCTS)
                .then()
                .statusCode(403)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:forbidden"));
    }

    private static Map<String, Object> product() {
        return Map.of(
                "sku", "SKU-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(),
                "name", "Security Keyboard",
                "categoryCode", "PERIPHERALS",
                "price", "10");
    }
}
