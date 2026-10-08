package com.commercehub.order.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.commercehub.order.infrastructure.messaging.outbox.OutboxEventEntity;
import com.commercehub.order.infrastructure.messaging.outbox.OutboxRepository;
import com.commercehub.order.infrastructure.observability.OutboxHealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxHealthCheckTest {

    @Mock
    OutboxRepository repository;

    @Test
    void upWhenThereAreNoFailedRows() {
        when(repository.countByStatus(OutboxEventEntity.STATUS_FAILED)).thenReturn(0L);
        when(repository.countByStatus(OutboxEventEntity.STATUS_PENDING)).thenReturn(2L);

        HealthCheckResponse response = new OutboxHealthCheck(repository).call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getName()).isEqualTo("outbox");
    }

    @Test
    void downWhenFailedRowsExist() {
        when(repository.countByStatus(OutboxEventEntity.STATUS_FAILED)).thenReturn(1L);
        when(repository.countByStatus(OutboxEventEntity.STATUS_PENDING)).thenReturn(0L);

        HealthCheckResponse response = new OutboxHealthCheck(repository).call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
    }
}
