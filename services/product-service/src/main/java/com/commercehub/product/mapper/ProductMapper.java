package com.commercehub.product.mapper;

import com.commercehub.product.api.dto.CreateProductRequest;
import com.commercehub.product.api.dto.ProductResponse;
import com.commercehub.product.api.dto.UpdateProductRequest;
import com.commercehub.product.domain.Money;
import com.commercehub.product.domain.entity.ProductEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ApplicationScoped
public class ProductMapper {

    public ProductEntity toNewEntity(CreateProductRequest request) {
        ProductEntity entity = ProductEntity.newProduct();
        entity.setSku(request.sku());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setCategoryCode(request.categoryCode());
        entity.setPrice(Money.normalize(request.price()));
        entity.setCurrencyCode(currencyOrDefault(request.currencyCode()));
        entity.setActive(true);
        return entity;
    }

    public void applyUpdate(UpdateProductRequest request, ProductEntity entity) {
        entity.setSku(request.sku());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setCategoryCode(request.categoryCode());
        entity.setPrice(Money.normalize(request.price()));
        entity.setCurrencyCode(currencyOrDefault(request.currencyCode()));
        entity.setActive(request.active());
    }

    public ProductResponse toResponse(ProductEntity entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getSku(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategoryCode(),
                Money.normalize(entity.getPrice()),
                entity.getCurrencyCode(),
                entity.isActive(),
                entity.getVersion(),
                utc(entity.getCreatedAt()),
                utc(entity.getUpdatedAt()));
    }

    private static String currencyOrDefault(String currencyCode) {
        return currencyCode == null ? Money.DEFAULT_CURRENCY : currencyCode;
    }

    private static OffsetDateTime utc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
