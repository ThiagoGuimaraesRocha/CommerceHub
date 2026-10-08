package com.commercehub.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercehub.inventory.application.InventoryService;
import com.commercehub.inventory.domain.entity.InventoryItemEntity;
import com.commercehub.inventory.infrastructure.messaging.EventEnvelope;
import com.commercehub.inventory.infrastructure.messaging.KafkaTopics;
import com.commercehub.inventory.infrastructure.messaging.MessageSerde;
import com.commercehub.inventory.infrastructure.messaging.outbox.OutboxRelay;
import com.commercehub.inventory.infrastructure.messaging.payload.OrderConfirmedPayload;
import com.commercehub.inventory.infrastructure.messaging.payload.ReleaseInventoryPayload;
import com.commercehub.inventory.infrastructure.persistence.InventoryRepository;
import com.commercehub.inventory.support.AwaitAssertions;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(KafkaBrokerTestProfile.class)
class KafkaIntegrationTest {

    @Inject
    InventoryService inventoryService;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    OutboxRelay outboxRelay;

    @Inject
    MessageSerde serde;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    @Test
    void orderConfirmedOverKafkaReservesAndDuplicateDoesNotDoubleBook() throws Exception {
        String productId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        inventoryService.setStock(productId, 10);

        String message = confirmed(orderId, productId, 3);
        try (KafkaProducer<String, String> producer = producer()) {
            producer.send(new ProducerRecord<>(KafkaTopics.ORDER_EVENTS, orderId, message)).get();
            producer.send(new ProducerRecord<>(KafkaTopics.ORDER_EVENTS, orderId, message)).get();
        }

        AwaitAssertions.untilAsserted(45, () -> {
            InventoryItemEntity item = inventoryRepository.findByProductId(productId).orElseThrow();
            assertThat(item.getReservedQty()).isEqualTo(3);
            assertThat(item.getAvailableQty()).isEqualTo(7);
        });

        try (KafkaProducer<String, String> producer = producer()) {
            producer.send(new ProducerRecord<>(KafkaTopics.INVENTORY_COMMANDS, orderId, release(orderId))).get();
        }

        AwaitAssertions.untilAsserted(45, () -> {
            InventoryItemEntity item = inventoryRepository.findByProductId(productId).orElseThrow();
            assertThat(item.getReservedQty()).isZero();
            assertThat(item.getAvailableQty()).isEqualTo(10);
        });

        outboxRelay.drain();
    }

    private KafkaProducer<String, String> producer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, Long.toString(Duration.ofSeconds(10).toMillis()));
        return new KafkaProducer<>(props);
    }

    private String confirmed(String orderId, String productId, long quantity) {
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID().toString(),
                "OrderConfirmed",
                EventEnvelope.KIND_EVENT,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "order-service",
                "Order",
                orderId,
                orderId,
                null,
                serde.toTree(new OrderConfirmedPayload(
                        orderId, "cust", List.of(new OrderConfirmedPayload.Item(productId, quantity)),
                        new BigDecimal("10.0000"), "BRL")));
        return serde.toJson(envelope);
    }

    private String release(String orderId) {
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID().toString(),
                "ReleaseInventory",
                EventEnvelope.KIND_COMMAND,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "order-service",
                "Order",
                orderId,
                orderId,
                null,
                serde.toTree(new ReleaseInventoryPayload(orderId, "CUSTOMER_CANCELLED")));
        return serde.toJson(envelope);
    }
}
