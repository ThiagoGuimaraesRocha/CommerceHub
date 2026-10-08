package com.commercehub.order.infrastructure.persistence;

import com.commercehub.order.domain.entity.OrderItemEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderItemRepository implements PanacheRepositoryBase<OrderItemEntity, String> {
}
