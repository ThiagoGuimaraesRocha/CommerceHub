package com.commercehub.product.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(user = "admin-user", roles = "ADMIN")
class ProductResourceTest {

    private static final String PRODUCTS = "/api/v1/products";

    @Test
    void createReturns201WithLocationAndScaleFourPrice() {
        String sku = uniqueSku();

        given().contentType(ContentType.JSON)
                .body(product(sku, "349.9"))
                .when().post(PRODUCTS)
                .then()
                .statusCode(201)
                .header("Location", containsString(PRODUCTS + "/"))
                .body("id", notNullValue())
                .body("sku", equalTo(sku))
                .body("price", equalTo(349.9f))
                .body("currencyCode", equalTo("BRL"))
                .body("active", equalTo(true))
                .body("version", equalTo(0));
    }

    @Test
    void priceIsSerializedWithFourDecimalPlaces() {
        String id = create(uniqueSku(), "10").path("id");

        String body = given().when().get(PRODUCTS + "/" + id).then().statusCode(200).extract().asString();

        org.assertj.core.api.Assertions.assertThat(body).contains("\"price\":10.0000");
    }

    @Test
    void zeroPriceIsAccepted() {
        given().contentType(ContentType.JSON)
                .body(product(uniqueSku(), "0"))
                .when().post(PRODUCTS)
                .then()
                .statusCode(201);
    }

    @Test
    void negativePriceAndInvalidFieldsReturnProblemDetails() {
        given().contentType(ContentType.JSON)
                .body(Map.of("sku", "bad sku", "name", "", "categoryCode", "PERIPHERALS", "price", "-1"))
                .when().post(PRODUCTS)
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:validation-error"))
                .body("status", equalTo(400))
                .body("instance", equalTo(PRODUCTS))
                .body("violations.field", hasItems("sku", "name", "price"));
    }

    @Test
    void moreThanFourDecimalPlacesIsRejected() {
        given().contentType(ContentType.JSON)
                .body(product(uniqueSku(), "1.12345"))
                .when().post(PRODUCTS)
                .then()
                .statusCode(400)
                .body("violations.field", hasItem("price"));
    }

    @Test
    void malformedJsonReturns400Problem() {
        given().contentType(ContentType.JSON)
                .body("{\"sku\": ")
                .when().post(PRODUCTS)
                .then()
                .statusCode(400)
                .contentType("application/problem+json");
    }

    @Test
    void duplicateSkuReturns409() {
        String sku = uniqueSku();
        create(sku, "10");

        given().contentType(ContentType.JSON)
                .body(product(sku, "20"))
                .when().post(PRODUCTS)
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:duplicate-sku"))
                .body("detail", containsString(sku));
    }

    @Test
    void unknownProductReturns404() {
        given().when().get(PRODUCTS + "/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:product-not-found"));
    }

    @Test
    void updateIncrementsVersionAndStaleVersionReturns409() {
        String sku = uniqueSku();
        String id = create(sku, "100").path("id");

        given().contentType(ContentType.JSON)
                .body(update(sku, "90.5", true, 0))
                .when().put(PRODUCTS + "/" + id)
                .then()
                .statusCode(200)
                .body("price", equalTo(90.5f))
                .body("version", equalTo(1));

        given().contentType(ContentType.JSON)
                .body(update(sku, "80", true, 0))
                .when().put(PRODUCTS + "/" + id)
                .then()
                .statusCode(409)
                .body("type", equalTo("urn:commercehub:problem:stale-version"));
    }

    @Test
    void deleteDeactivatesTheProduct() {
        String id = create(uniqueSku(), "5").path("id");

        given().when().delete(PRODUCTS + "/" + id).then().statusCode(204);

        given().when().get(PRODUCTS + "/" + id)
                .then()
                .statusCode(200)
                .body("active", equalTo(false));
    }

    @Test
    void listFiltersByCategoryAndPaginates() {
        String category = "CAT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        for (int i = 0; i < 3; i++) {
            given().contentType(ContentType.JSON)
                    .body(Map.of("sku", uniqueSku(), "name", "Item " + i, "categoryCode", category, "price", "1"))
                    .when().post(PRODUCTS)
                    .then().statusCode(201);
        }

        given().queryParam("category", category).queryParam("size", 2)
                .when().get(PRODUCTS)
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(2))
                .body("items.categoryCode", everyItem(equalTo(category)))
                .body("totalItems", equalTo(3))
                .body("totalPages", equalTo(2));
    }

    @Test
    void invalidPageSizeReturns400() {
        given().queryParam("size", 500)
                .when().get(PRODUCTS)
                .then()
                .statusCode(400)
                .body("violations.field", hasItem("size"));
    }

    @Test
    void listWithoutFiltersReturnsPage() {
        create(uniqueSku(), "1");

        given().when().get(PRODUCTS)
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("totalItems", greaterThanOrEqualTo(1));
    }

    @Test
    void locationHeaderPointsToTheCreatedProduct() {
        ExtractableResponse<Response> created = create(uniqueSku(), "1");

        org.assertj.core.api.Assertions.assertThat(created.header("Location")).endsWith("/" + created.path("id"));
        given().when().get(created.header("Location")).then().statusCode(200).body("id", endsWith(created.path("id")));
    }

    private static ExtractableResponse<Response> create(String sku, String price) {
        return given().contentType(ContentType.JSON)
                .body(product(sku, price))
                .when().post(PRODUCTS)
                .then().statusCode(201)
                .extract();
    }

    private static Map<String, Object> product(String sku, String price) {
        return Map.of("sku", sku, "name", "Mechanical Keyboard", "categoryCode", "PERIPHERALS", "price", price);
    }

    private static Map<String, Object> update(String sku, String price, boolean active, long version) {
        return Map.of("sku", sku, "name", "Mechanical Keyboard", "categoryCode", "PERIPHERALS",
                "price", price, "currencyCode", "BRL", "active", active, "version", version);
    }

    private static String uniqueSku() {
        return "SKU-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
    }
}
