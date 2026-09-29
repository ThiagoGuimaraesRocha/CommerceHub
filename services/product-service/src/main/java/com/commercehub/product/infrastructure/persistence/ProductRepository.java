package com.commercehub.product.infrastructure.persistence;

import com.commercehub.product.domain.entity.ProductEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ProductRepository implements PanacheRepositoryBase<ProductEntity, String> {

    public Optional<ProductEntity> findBySku(String sku) {
        return find("sku", sku).firstResultOptional();
    }

    public boolean existsBySkuAndIdNot(String sku, String id) {
        return count("sku = ?1 and id <> ?2", sku, id) > 0;
    }

    public PanacheQuery<ProductEntity> search(String categoryCode, Boolean active) {
        List<String> filters = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        if (categoryCode != null) {
            filters.add("categoryCode = :categoryCode");
            parameters.put("categoryCode", categoryCode);
        }
        if (active != null) {
            filters.add("active = :active");
            parameters.put("active", active);
        }
        Sort sort = Sort.by("name").and("id");
        if (filters.isEmpty()) {
            return findAll(sort);
        }
        return find(String.join(" and ", filters), sort, parameters);
    }
}
