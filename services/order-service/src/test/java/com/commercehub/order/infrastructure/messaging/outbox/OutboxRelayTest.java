package com.commercehub.order.infrastructure.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    OutboxRepository repository;

    @Mock
    Producer<String, String> producer;

    @Test
    void successfulSendMarksPublished() {
        OutboxEventEntity event = OutboxEventEntity.pending(
                "e1", "Order", "o1", "OrderConfirmed", "EVENT", "commerce.order.events", "o1", "{\"eventId\":\"e1\"}");
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("commerce.order.events", 0), 0, 0, 0, 0, 0);
        when(producer.send(any())).thenReturn(CompletableFuture.completedFuture(metadata));

        OutboxRelay relay = new OutboxRelay(repository, true, "localhost:9092", 50, 10, Duration.ofSeconds(1));
        relay.useProducer(producer);
        relay.publishBatch(List.of(event));

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.STATUS_PUBLISHED);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(captor.capture());
        assertThat(captor.getValue().topic()).isEqualTo("commerce.order.events");
        assertThat(captor.getValue().key()).isEqualTo("o1");
        assertThat(new String(captor.getValue().headers().lastHeader("eventType").value())).isEqualTo("OrderConfirmed");
    }

    @Test
    void repeatedFailuresMarkFailedAfterMaxAttempts() {
        OutboxEventEntity event = OutboxEventEntity.pending(
                "e1", "Order", "o1", "OrderConfirmed", "EVENT", "commerce.order.events", "o1", "{}");
        when(producer.send(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        OutboxRelay relay = new OutboxRelay(repository, true, "localhost:9092", 50, 3, Duration.ofMillis(50));
        relay.useProducer(producer);
        relay.publishBatch(List.of(event, event, event));

        assertThat(event.getAttempts()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.STATUS_FAILED);
        verify(producer, times(3)).send(any());
    }
}
