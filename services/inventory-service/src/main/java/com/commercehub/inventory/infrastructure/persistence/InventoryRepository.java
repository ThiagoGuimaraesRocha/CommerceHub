package com.commercehub.inventory.infrastructure.persistence;

import com.commercehub.inventory.domain.entity.InventoryItemEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class InventoryRepository implements PanacheRepositoryBase<InventoryItemEntity, String> {

    public Optional<InventoryItemEntity> findByProductId(String productId) {
        return find("productId", productId).firstResultOptional();
    }
}
