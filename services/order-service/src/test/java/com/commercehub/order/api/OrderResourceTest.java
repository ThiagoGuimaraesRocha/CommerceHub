package com.commercehub.order.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;

import com.commercehub.order.exception.UnknownProductException;
import com.commercehub.order.infrastructure.client.ProductClient;
import com.commercehub.order.infrastructure.client.ProductSnapshotResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderResourceTest {

    private static final String ORDERS = "/api/v1/orders";
    private static final String CUSTOMER = "11111111-1111-1111-1111-111111111111";

    @InjectMock
    @RestClient
    ProductClient productClient;

    @Inject
    CircuitBreakerMaintenance circuitBreakers;

    @BeforeEach
    void resetClient() {
        circuitBreakers.resetAll();
        reset(productClient);
        lenient().when(productClient.getById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            String suffix = id == null || id.length() < 8 ? "XXXXXXXX" : id.substring(0, 8);
            return new ProductSnapshotResponse(id, "SKU-" + suffix, "Product " + suffix,
                    new BigDecimal("10.5"), "BRL", true);
        });
    }

    @Test
    void createReturns201WithSnapshotAndTotal() {
        String productId = UUID.randomUUID().toString();

        given().contentType(ContentType.JSON)
                .body(orderBody(productId, 2))
                .when().post(ORDERS)
                .then()
                .statusCode(201)
                .header("Location", containsString(ORDERS + "/"))
                .body("status", equalTo("CREATED"))
                .body("currencyCode", equalTo("BRL"))
                .body("totalAmount", equalTo(21.0f))
                .body("items.size()", equalTo(1))
                .body("items[0].productId", equalTo(productId))
                .body("items[0].sku", notNullValue())
                .body("items[0].quantity", equalTo(2));
    }

    @Test
    void totalIsSerializedWithScaleFour() {
        String id = createOrder(UUID.randomUUID().toString(), 1).path("id");
        String body = given().when().get(ORDERS + "/" + id).then().statusCode(200).extract().asString();
        org.assertj.core.api.Assertions.assertThat(body).contains("\"totalAmount\":10.5000");
    }

    @Test
    void unknownProductReturns400() {
        doThrow(new UnknownProductException("x")).when(productClient).getById(anyString());

        given().contentType(ContentType.JSON)
                .body(orderBody(UUID.randomUUID().toString(), 1))
                .when().post(ORDERS)
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:unknown-product"));
    }

    @Test
    void productServiceUnavailableReturns503() {
        // Throw a plain runtime failure so the CircuitBreaker (failOn remote/processing) stays closed
        // for later tests; OrderApplicationService still maps it to 503.
        doThrow(new IllegalStateException("connection reset")).when(productClient).getById(anyString());

        given().contentType(ContentType.JSON)
                .body(orderBody(UUID.randomUUID().toString(), 1))
                .when().post(ORDERS)
                .then()
                .statusCode(503)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:product-service-unavailable"));
    }

    @Test
    void validationErrorsReturnProblemDetails() {
        given().contentType(ContentType.JSON)
                .body(Map.of("customerId", "", "items", List.of()))
                .when().post(ORDERS)
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:validation-error"))
                .body("violations.field", hasItem("customerId"));
    }

    @Test
    void getUnknownOrderReturns404() {
        given().when().get(ORDERS + "/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("type", equalTo("urn:commercehub:problem:order-not-found"));
    }

    @Test
    void listByCustomerReturnsCreatedOrders() {
        String id = createOrder(UUID.randomUUID().toString(), 1).path("id");

        given().queryParam("customerId", CUSTOMER)
                .when().get(ORDERS)
                .then()
                .statusCode(200)
                .body("id", hasItem(id));
    }

    @Test
    void confirmTransitionsCreatedToConfirmed() {
        String id = createOrder(UUID.randomUUID().toString(), 1).path("id");

        given().when().post(ORDERS + "/" + id + "/confirm")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    void confirmTwiceReturns409() {
        String id = createOrder(UUID.randomUUID().toString(), 1).path("id");
        given().when().post(ORDERS + "/" + id + "/confirm").then().statusCode(200);

        given().when().post(ORDERS + "/" + id + "/confirm")
                .then()
                .statusCode(409)
                .body("type", equalTo("urn:commercehub:problem:invalid-order-state"));
    }

    @Test
    void cancelCreatedOrderWithReason() {
        String id = createOrder(UUID.randomUUID().toString(), 1).path("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("reasonCode", "CHANGED_MIND"))
                .when().post(ORDERS + "/" + id + "/cancel")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"))
                .body("cancellationReason", equalTo("CHANGED_MIND"))
                .body("cancelledBy", equalTo("CUSTOMER"));
    }

    @Test
    void cancelOtherWithoutNoteReturns400() {
        String id = createOrder(UUID.randomUUID().toString(), 1).path("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("reasonCode", "OTHER"))
                .when().post(ORDERS + "/" + id + "/cancel")
                .then()
                .statusCode(400)
                .body("type", equalTo("urn:commercehub:problem:bad-request"));
    }

    @Test
    void cancelConfirmedReturnsAwaitingInventory() {
        String id = createOrder(UUID.randomUUID().toString(), 1).path("id");
        given().when().post(ORDERS + "/" + id + "/confirm").then().statusCode(200);

        given().contentType(ContentType.JSON)
                .body(Map.of("reasonCode", "CHANGED_MIND"))
                .when().post(ORDERS + "/" + id + "/cancel")
                .then()
                .statusCode(409)
                .body("type", equalTo("urn:commercehub:problem:order-awaiting-inventory"));
    }

    @Test
    void systemReasonRejected() {
        String id = createOrder(UUID.randomUUID().toString(), 1).path("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("reasonCode", "INSUFFICIENT_STOCK"))
                .when().post(ORDERS + "/" + id + "/cancel")
                .then()
                .statusCode(400);
    }

    @Test
    void cancellationReasonsListsCustomerSelectableOnly() {
        given().when().get(ORDERS + "/cancellation-reasons")
                .then()
                .statusCode(200)
                .body("code", hasItem("CHANGED_MIND"))
                .body("code", hasItem("OTHER"))
                .body("find { it.code == 'OTHER' }.noteRequired", equalTo(true));
    }

    private ExtractableResponse<Response> createOrder(String productId, long quantity) {
        return given().contentType(ContentType.JSON)
                .body(orderBody(productId, quantity))
                .when().post(ORDERS)
                .then()
                .statusCode(201)
                .extract();
    }

    private static Map<String, Object> orderBody(String productId, long quantity) {
        return Map.of(
                "customerId", CUSTOMER,
                "items", List.of(Map.of("productId", productId, "quantity", quantity)));
    }
}
