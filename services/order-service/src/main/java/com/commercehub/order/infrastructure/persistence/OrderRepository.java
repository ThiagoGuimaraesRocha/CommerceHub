package com.commercehub.order.infrastructure.persistence;

import com.commercehub.order.domain.entity.OrderEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class OrderRepository implements PanacheRepositoryBase<OrderEntity, String> {

    public List<OrderEntity> findByCustomerId(String customerId) {
        return list("customerId", Sort.by("createdAt").descending().and("id"), customerId);
    }
}
