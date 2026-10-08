package com.commercehub.order.infrastructure.messaging.outbox;

import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Publishes {@code PENDING} outbox rows to Kafka and marks them {@code PUBLISHED}. A row that keeps
 * failing becomes {@code FAILED} after {@code maxAttempts} and is surfaced by health/metrics (ADR 0006).
 * Delivery is at-least-once; consumers deduplicate with {@code PROCESSED_EVENTS}.
 */
@ApplicationScoped
public class OutboxRelay {

    private static final Logger LOG = Logger.getLogger(OutboxRelay.class);
    private static final String EVENT_TYPE_HEADER = "eventType";

    private final OutboxRepository repository;
    private final boolean enabled;
    private final String bootstrapServers;
    private final int batchSize;
    private final int maxAttempts;
    private final Duration sendTimeout;

    private volatile Producer<String, String> producer;

    public OutboxRelay(
            OutboxRepository repository,
            @ConfigProperty(name = "commercehub.outbox.relay.enabled", defaultValue = "true") boolean enabled,
            @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:29092") String bootstrapServers,
            @ConfigProperty(name = "commercehub.outbox.relay.batch-size", defaultValue = "50") int batchSize,
            @ConfigProperty(name = "commercehub.outbox.relay.max-attempts", defaultValue = "10") int maxAttempts,
            @ConfigProperty(name = "commercehub.outbox.relay.send-timeout", defaultValue = "PT5S") Duration sendTimeout) {
        this.repository = repository;
        this.enabled = enabled;
        this.bootstrapServers = bootstrapServers;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.sendTimeout = sendTimeout;
    }

    @PostConstruct
    void init() {
        if (enabled) {
            this.producer = new KafkaProducer<>(producerConfig());
        }
    }

    @PreDestroy
    void close() {
        if (producer != null) {
            producer.close(Duration.ofSeconds(5));
        }
    }

    @Scheduled(every = "${commercehub.outbox.relay.every:1s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void drain() {
        if (!enabled || producer == null) {
            return;
        }
        publishBatch(repository.findPending(batchSize));
    }

    void publishBatch(List<OutboxEventEntity> pending) {
        for (OutboxEventEntity event : pending) {
            try {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(event.getTopic(), event.getMessageKey(), event.getPayload());
                record.headers().add(EVENT_TYPE_HEADER, event.getEventType().getBytes(StandardCharsets.UTF_8));
                producer.send(record).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
                event.markPublished();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                event.recordFailure("interrupted while publishing", maxAttempts);
                return;
            } catch (Exception e) {
                LOG.warnf("Failed to publish outbox event %s (%s): %s",
                        event.getEventId(), event.getEventType(), e.getMessage());
                event.recordFailure(e.getMessage(), maxAttempts);
            }
        }
    }

    private Properties producerConfig() {
        Properties props = new Properties();
        props.putAll(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true",
                ProducerConfig.CLIENT_ID_CONFIG, "order-service-outbox-relay",
                ProducerConfig.MAX_BLOCK_MS_CONFIG, Long.toString(sendTimeout.toMillis())));
        return props;
    }
}
