package com.commercehub.order.infrastructure.messaging.outbox;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class OutboxRepository implements PanacheRepositoryBase<OutboxEventEntity, String> {

    public List<OutboxEventEntity> findPending(int batchSize) {
        return find("status", Sort.by("createdAt").ascending(), OutboxEventEntity.STATUS_PENDING)
                .page(Page.ofSize(batchSize))
                .list();
    }

    public long countByStatus(String status) {
        return count("status", status);
    }
}
