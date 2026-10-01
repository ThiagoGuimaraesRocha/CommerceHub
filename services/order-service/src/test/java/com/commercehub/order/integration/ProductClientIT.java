package com.commercehub.order.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercehub.order.exception.RemoteProductServiceException;
import com.commercehub.order.exception.UnknownProductException;
import com.commercehub.order.infrastructure.client.ProductClient;
import com.commercehub.order.infrastructure.client.ProductSnapshotResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = ProductClientIT.WireMockProductService.class, restrictToAnnotatedClass = true)
class ProductClientIT {

    @Inject
    @RestClient
    ProductClient productClient;

    static WireMockServer wireMock;

    @BeforeEach
    void reset() {
        wireMock.resetAll();
    }

    @Test
    void getByIdReturnsProductSnapshot() {
        String id = UUID.randomUUID().toString();
        wireMock.stubFor(get(urlEqualTo("/api/v1/products/" + id))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"%s","sku":"KB-001","name":"Keyboard","price":10.5000,"currencyCode":"BRL","active":true}
                                """.formatted(id))));

        ProductSnapshotResponse product = productClient.getById(id);
        assertThat(product.id()).isEqualTo(id);
        assertThat(product.sku()).isEqualTo("KB-001");
        assertThat(product.active()).isTrue();
    }

    @Test
    void notFoundBecomesUnknownProduct() {
        String id = UUID.randomUUID().toString();
        wireMock.stubFor(get(urlEqualTo("/api/v1/products/" + id))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> productClient.getById(id))
                .isInstanceOf(UnknownProductException.class);
    }

    @Test
    void serverErrorBecomesRemoteException() {
        String id = UUID.randomUUID().toString();
        wireMock.stubFor(get(urlEqualTo("/api/v1/products/" + id))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> productClient.getById(id))
                .isInstanceOf(RemoteProductServiceException.class);
    }

    public static class WireMockProductService implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            wireMock = new WireMockServer(0);
            wireMock.start();
            return Map.of("quarkus.rest-client.product-service.url", wireMock.baseUrl());
        }

        @Override
        public void stop() {
            if (wireMock != null) {
                wireMock.stop();
            }
        }
    }
}
