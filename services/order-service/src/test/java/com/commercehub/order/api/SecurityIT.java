package com.commercehub.order.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;

import com.commercehub.order.api.dto.CreateOrderRequest;
import com.commercehub.order.api.dto.OrderItemRequest;
import com.commercehub.order.api.dto.OrderResponse;
import com.commercehub.order.application.OrderApplicationService;
import com.commercehub.order.infrastructure.client.ProductClient;
import com.commercehub.order.infrastructure.client.ProductSnapshotResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SecurityIT {

    private static final String ORDERS = "/api/v1/orders";
    private static final String OWNER = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String OTHER = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @InjectMock
    @RestClient
    ProductClient productClient;

    @Inject
    OrderApplicationService orders;

    @BeforeEach
    void products() {
        reset(productClient);
        lenient().when(productClient.getById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return new ProductSnapshotResponse(id, "SKU-SEC", "Sec product", new BigDecimal("10.5"), "BRL", true);
        });
    }

    @Test
    void missingTokenIs401() {
        given().when().get(ORDERS)
                .then()
                .statusCode(401)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:unauthorized"));
    }

    @Test
    @TestSecurity(user = OTHER, roles = "CUSTOMER")
    void customerCannotReadSomeoneElsesOrder() {
        OrderResponse created = orders.create(OWNER, new CreateOrderRequest(
                List.of(new OrderItemRequest(UUID.randomUUID().toString(), 1L))));

        given().when().get(ORDERS + "/" + created.id())
                .then()
                .statusCode(403)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:order-not-owned"));
    }

    @Test
    @TestSecurity(user = OTHER, roles = "CUSTOMER")
    void listReturnsOnlyTheAuthenticatedCustomersOrders() {
        orders.create(OWNER, new CreateOrderRequest(
                List.of(new OrderItemRequest(UUID.randomUUID().toString(), 1L))));
        OrderResponse mine = orders.create(OTHER, new CreateOrderRequest(
                List.of(new OrderItemRequest(UUID.randomUUID().toString(), 1L))));

        given().when().get(ORDERS)
                .then()
                .statusCode(200)
                .body("id", org.hamcrest.Matchers.hasItem(mine.id()))
                .body("id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(
                        orders.findByCustomerId(OWNER).getFirst().id())));
    }

    @Test
    @TestSecurity(user = OWNER, roles = "CUSTOMER")
    void createUsesJwtSubjectAsCustomerId() {
        given().contentType(ContentType.JSON)
                .body(Map.of("items", List.of(Map.of("productId", UUID.randomUUID().toString(), "quantity", 1))))
                .when().post(ORDERS)
                .then()
                .statusCode(201)
                .body("customerId", equalTo(OWNER));
    }
}
