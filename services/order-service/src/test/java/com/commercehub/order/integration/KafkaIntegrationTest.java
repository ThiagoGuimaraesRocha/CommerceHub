package com.commercehub.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.commercehub.order.api.dto.CreateOrderRequest;
import com.commercehub.order.api.dto.OrderItemRequest;
import com.commercehub.order.api.dto.OrderResponse;
import com.commercehub.order.application.OrderApplicationService;
import com.commercehub.order.infrastructure.client.ProductClient;
import com.commercehub.order.infrastructure.client.ProductSnapshotResponse;
import com.commercehub.order.infrastructure.messaging.EventEnvelope;
import com.commercehub.order.infrastructure.messaging.KafkaTopics;
import com.commercehub.order.infrastructure.messaging.MessageSerde;
import com.commercehub.order.infrastructure.messaging.outbox.OutboxRelay;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(KafkaBrokerTestProfile.class)
class KafkaIntegrationTest {

    @InjectMock
    @RestClient
    ProductClient productClient;

    @Inject
    OrderApplicationService orders;

    @Inject
    OutboxRelay outboxRelay;

    @Inject
    MessageSerde serde;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    @BeforeEach
    void products() {
        lenient().when(productClient.getById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return new ProductSnapshotResponse(id, "SKU-IT", "IT product", new BigDecimal("10.5"), "BRL", true);
        });
    }

    @Test
    void confirmPublishesExactlyOneOrderConfirmedOnTheBroker() {
        String productId = UUID.randomUUID().toString();
        OrderResponse created = orders.create("11111111-1111-1111-1111-111111111111",
                new CreateOrderRequest(List.of(new OrderItemRequest(productId, 2L))));

        try (KafkaConsumer<String, String> consumer = consumer()) {
            consumer.subscribe(List.of(KafkaTopics.ORDER_EVENTS));
            consumer.poll(Duration.ofMillis(200));

            orders.confirm(created.id(), created.customerId());
            outboxRelay.drain();

            EventEnvelope found = pollFor(consumer, created.id(), "OrderConfirmed", Duration.ofSeconds(20));
            assertThat(found.eventType()).isEqualTo("OrderConfirmed");
            assertThat(found.aggregateId()).isEqualTo(created.id());
            assertThat(found.messageKind()).isEqualTo("EVENT");
            assertThat(found.correlationId()).isEqualTo(created.id());
        }
    }

    private EventEnvelope pollFor(KafkaConsumer<String, String> consumer, String orderId, String eventType, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (var record : records) {
                if (!orderId.equals(record.key())) {
                    continue;
                }
                EventEnvelope envelope = serde.parse(record.value());
                if (eventType.equals(envelope.eventType())) {
                    return envelope;
                }
            }
        }
        throw new AssertionError("Did not observe " + eventType + " for order " + orderId);
    }

    private KafkaConsumer<String, String> consumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        return new KafkaConsumer<>(props);
    }
}
