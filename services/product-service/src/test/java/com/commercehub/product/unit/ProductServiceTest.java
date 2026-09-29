package com.commercehub.product.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercehub.product.api.dto.CreateProductRequest;
import com.commercehub.product.api.dto.ProductResponse;
import com.commercehub.product.api.dto.UpdateProductRequest;
import com.commercehub.product.application.ProductService;
import com.commercehub.product.domain.entity.ProductEntity;
import com.commercehub.product.exception.ConflictException;
import com.commercehub.product.exception.ProductNotFoundException;
import com.commercehub.product.infrastructure.persistence.ProductRepository;
import com.commercehub.product.mapper.ProductMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repository;

    ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repository, new ProductMapper());
    }

    @Test
    void createNormalizesPriceToScaleFourAndDefaultsCurrency() {
        when(repository.findBySku("KB-001")).thenReturn(Optional.empty());

        ProductResponse response = service.create(
                new CreateProductRequest("KB-001", "  Keyboard ", null, "PERIPHERALS", new BigDecimal("10.5"), null));

        assertThat(response.price()).isEqualByComparingTo("10.5").hasToString("10.5000");
        assertThat(response.currencyCode()).isEqualTo("BRL");
        assertThat(response.name()).isEqualTo("Keyboard");
        assertThat(response.active()).isTrue();
        assertThat(response.id()).hasSize(36);
        verify(repository).persistAndFlush(any(ProductEntity.class));
    }

    @Test
    void createAcceptsZeroPrice() {
        when(repository.findBySku("FREE-1")).thenReturn(Optional.empty());

        ProductResponse response = service.create(
                new CreateProductRequest("FREE-1", "Gift", null, "GIFTS", BigDecimal.ZERO, "USD"));

        assertThat(response.price()).hasToString("0.0000");
    }

    @Test
    void createRejectsDuplicateSku() {
        when(repository.findBySku("KB-001")).thenReturn(Optional.of(ProductEntity.newProduct()));

        assertThatThrownBy(() -> service.create(
                new CreateProductRequest("KB-001", "Keyboard", null, "PERIPHERALS", BigDecimal.ONE, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("KB-001");
        verify(repository, never()).persistAndFlush(any());
    }

    @Test
    void updateRejectsStaleVersion() {
        ProductEntity existing = existing("KB-001");
        when(repository.findByIdOptional(existing.getId())).thenReturn(Optional.of(existing));

        UpdateProductRequest request = new UpdateProductRequest(
                "KB-001", "Keyboard", null, "PERIPHERALS", BigDecimal.ONE, "BRL", true, 7L);

        assertThatThrownBy(() -> service.update(existing.getId(), request))
                .isInstanceOf(ConflictException.class)
                .extracting("type").isEqualTo("stale-version");
    }

    @Test
    void updateRejectsSkuOwnedByAnotherProduct() {
        ProductEntity existing = existing("KB-001");
        when(repository.findByIdOptional(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.existsBySkuAndIdNot("MS-002", existing.getId())).thenReturn(true);

        UpdateProductRequest request = new UpdateProductRequest(
                "MS-002", "Keyboard", null, "PERIPHERALS", BigDecimal.ONE, "BRL", true, 0L);

        assertThatThrownBy(() -> service.update(existing.getId(), request))
                .isInstanceOf(ConflictException.class)
                .extracting("type").isEqualTo("duplicate-sku");
    }

    @Test
    void deactivateMarksProductInactive() {
        ProductEntity existing = existing("KB-001");
        when(repository.findByIdOptional(existing.getId())).thenReturn(Optional.of(existing));

        service.deactivate(existing.getId());

        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void unknownProductRaisesNotFound() {
        when(repository.findByIdOptional("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("missing")).isInstanceOf(ProductNotFoundException.class);
    }

    private static ProductEntity existing(String sku) {
        ProductEntity entity = ProductEntity.newProduct();
        entity.setSku(sku);
        entity.setName("Keyboard");
        entity.setCategoryCode("PERIPHERALS");
        entity.setPrice(new BigDecimal("100.0000"));
        entity.setCurrencyCode("BRL");
        return entity;
    }
}
