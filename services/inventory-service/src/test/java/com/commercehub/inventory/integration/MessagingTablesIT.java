package com.commercehub.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Flyway-managed outbox and deduplication tables on a real Oracle Database Free container.
 */
@QuarkusTest
class MessagingTablesIT {

    private static final String VALID_ENVELOPE = """
            {"eventId":"%s","eventType":"InventoryReserved","messageKind":"EVENT","schemaVersion":1,
             "occurredAt":"2026-09-29T18:30:00Z","source":"inventory-service","aggregateType":"Inventory",
             "aggregateId":"%s","correlationId":"%s","causationId":null,"payload":{}}""";

    @Inject
    AgroalDataSource dataSource;

    @Test
    void outboxAcceptsAPendingEnvelope() throws SQLException {
        String eventId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();

        insertOutbox(eventId, orderId, VALID_ENVELOPE.formatted(eventId, orderId, orderId));

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT status, attempts, published_at FROM outbox_events WHERE event_id = ?")) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("status")).isEqualTo("PENDING");
                assertThat(rs.getInt("attempts")).isZero();
                assertThat(rs.getTimestamp("published_at")).isNull();
            }
        }
    }

    @Test
    void outboxRejectsPayloadThatIsNotJson() {
        String eventId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> insertOutbox(eventId, UUID.randomUUID().toString(), "not json"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("CK_OUTBOX_PAYLOAD_JSON");
    }

    @Test
    void outboxRejectsPublishedStatusWithoutTimestamp() throws SQLException {
        String eventId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        insertOutbox(eventId, orderId, VALID_ENVELOPE.formatted(eventId, orderId, orderId));

        assertThatThrownBy(() -> execute("UPDATE outbox_events SET status = 'PUBLISHED' WHERE event_id = ?", eventId))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("CK_OUTBOX_PUBLISHED");
    }

    @Test
    void processedEventsRejectsTheSameEventForTheSameConsumer() throws SQLException {
        String eventId = UUID.randomUUID().toString();
        String consumer = "inventory-service.commerce.order.events";
        insertProcessed(eventId, consumer);

        assertThatThrownBy(() -> insertProcessed(eventId, consumer))
                .isInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void processedEventsAllowsTheSameEventForAnotherConsumer() throws SQLException {
        String eventId = UUID.randomUUID().toString();
        insertProcessed(eventId, "inventory-service.commerce.order.events");

        insertProcessed(eventId, "inventory-service.commerce.inventory.commands");
    }

    private void insertOutbox(String eventId, String aggregateId, String payload) throws SQLException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     INSERT INTO outbox_events
                       (event_id, aggregate_type, aggregate_id, event_type, message_kind, topic, message_key, payload)
                     VALUES (?, 'Inventory', ?, 'InventoryReserved', 'EVENT', 'commerce.inventory.events', ?, ?)""")) {
            ps.setString(1, eventId);
            ps.setString(2, aggregateId);
            ps.setString(3, aggregateId);
            ps.setString(4, payload);
            ps.executeUpdate();
        }
    }

    private void insertProcessed(String eventId, String consumer) throws SQLException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO processed_events (event_id, consumer_name, event_type) VALUES (?, ?, 'OrderConfirmed')")) {
            ps.setString(1, eventId);
            ps.setString(2, consumer);
            ps.executeUpdate();
        }
    }

    private void execute(String sql, String param) throws SQLException {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            ps.executeUpdate();
        }
    }
}
