package com.commercehub.product.application;

import com.commercehub.product.api.dto.CreateProductRequest;
import com.commercehub.product.api.dto.PageResponse;
import com.commercehub.product.api.dto.ProductResponse;
import com.commercehub.product.api.dto.UpdateProductRequest;
import com.commercehub.product.domain.entity.ProductEntity;
import com.commercehub.product.exception.ConflictException;
import com.commercehub.product.exception.ProductNotFoundException;
import com.commercehub.product.infrastructure.persistence.ProductRepository;
import com.commercehub.product.mapper.ProductMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    public ProductService(ProductRepository repository, ProductMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (repository.findBySku(request.sku()).isPresent()) {
            throw ConflictException.duplicateSku(request.sku());
        }
        ProductEntity entity = mapper.toNewEntity(request);
        try {
            repository.persistAndFlush(entity);
        } catch (ConstraintViolationException e) {
            throw ConflictException.duplicateSku(request.sku());
        }
        return mapper.toResponse(entity);
    }

    public ProductResponse findById(String id) {
        return mapper.toResponse(load(id));
    }

    public PageResponse<ProductResponse> search(String categoryCode, Boolean active, int page, int size) {
        PanacheQuery<ProductEntity> query = repository.search(categoryCode, active).page(Page.of(page, size));
        return new PageResponse<>(
                query.list().stream().map(mapper::toResponse).toList(),
                page,
                size,
                query.count(),
                query.pageCount());
    }

    @Transactional
    public ProductResponse update(String id, UpdateProductRequest request) {
        ProductEntity entity = load(id);
        if (entity.getVersion() != request.version()) {
            throw ConflictException.staleVersion(id, request.version(), entity.getVersion());
        }
        if (!entity.getSku().equals(request.sku()) && repository.existsBySkuAndIdNot(request.sku(), id)) {
            throw ConflictException.duplicateSku(request.sku());
        }
        mapper.applyUpdate(request, entity);
        try {
            repository.flush();
        } catch (ConstraintViolationException e) {
            throw ConflictException.duplicateSku(request.sku());
        }
        return mapper.toResponse(entity);
    }

    /**
     * Soft delete: the product is deactivated so that historical references stay valid.
     */
    @Transactional
    public void deactivate(String id) {
        ProductEntity entity = load(id);
        if (entity.isActive()) {
            entity.setActive(false);
        }
    }

    private ProductEntity load(String id) {
        return repository.findByIdOptional(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
