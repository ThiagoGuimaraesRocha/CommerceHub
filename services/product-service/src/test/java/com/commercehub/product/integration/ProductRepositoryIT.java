package com.commercehub.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercehub.product.domain.entity.ProductEntity;
import com.commercehub.product.infrastructure.persistence.ProductRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

/**
 * Runs against a real Oracle Database Free container started by Quarkus Dev Services,
 * with the schema created by the Flyway migrations.
 */
@QuarkusTest
class ProductRepositoryIT {

    @Inject
    ProductRepository repository;

    @Inject
    EntityManager entityManager;

    @Test
    void persistsAndReadsAllColumns() {
        String id = QuarkusTransaction.requiringNew().call(() -> {
            ProductEntity entity = product(uniqueSku(), "12.3456");
            entity.setDescription("Oracle round trip");
            repository.persist(entity);
            return entity.getId();
        });

        ProductEntity loaded = QuarkusTransaction.requiringNew().call(() -> repository.findById(id));

        assertThat(loaded.getPrice()).isEqualByComparingTo("12.3456");
        assertThat(loaded.getDescription()).isEqualTo("Oracle round trip");
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getVersion()).isZero();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateTimestampAndVersionChangeOnUpdate() throws InterruptedException {
        String id = QuarkusTransaction.requiringNew().call(() -> {
            ProductEntity entity = product(uniqueSku(), "1");
            repository.persist(entity);
            return entity.getId();
        });
        OffsetDateTime firstUpdate = QuarkusTransaction.requiringNew().call(() -> repository.findById(id).getUpdatedAt());

        Thread.sleep(20);
        QuarkusTransaction.requiringNew().run(() -> repository.findById(id).setName("Renamed"));

        ProductEntity updated = QuarkusTransaction.requiringNew().call(() -> repository.findById(id));
        assertThat(updated.getVersion()).isEqualTo(1);
        assertThat(updated.getUpdatedAt()).isAfter(firstUpdate);
        assertThat(updated.getCreatedAt()).isBefore(updated.getUpdatedAt());
    }

    @Test
    void databaseEnforcesUniqueSku() {
        String sku = uniqueSku();
        QuarkusTransaction.requiringNew().run(() -> repository.persist(product(sku, "1")));

        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> repository.persistAndFlush(product(sku, "2"))))
                .satisfies(e -> assertThat(findCause(e, ConstraintViolationException.class)).isNotNull());
    }

    @Test
    void concurrentModificationRaisesOptimisticLock() {
        String id = QuarkusTransaction.requiringNew().call(() -> {
            ProductEntity entity = product(uniqueSku(), "1");
            repository.persist(entity);
            return entity.getId();
        });

        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> {
            ProductEntity stale = repository.findById(id);
            QuarkusTransaction.requiringNew().run(() -> repository.findById(id).setName("Changed elsewhere"));
            stale.setName("Stale write");
            entityManager.flush();
        })).satisfies(e -> assertThat(findCause(e, OptimisticLockException.class)).isNotNull());
    }

    @Test
    void searchFiltersByCategoryAndActive() {
        String category = "IT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        QuarkusTransaction.requiringNew().run(() -> {
            ProductEntity active = product(uniqueSku(), "1");
            active.setCategoryCode(category);
            ProductEntity inactive = product(uniqueSku(), "1");
            inactive.setCategoryCode(category);
            inactive.setActive(false);
            repository.persist(active);
            repository.persist(inactive);
        });

        long activeCount = QuarkusTransaction.requiringNew().call(() -> repository.search(category, true).count());
        long allCount = QuarkusTransaction.requiringNew().call(() -> repository.search(category, null).count());

        assertThat(activeCount).isEqualTo(1);
        assertThat(allCount).isEqualTo(2);
    }

    private static ProductEntity product(String sku, String price) {
        ProductEntity entity = ProductEntity.newProduct();
        entity.setSku(sku);
        entity.setName("Integration product");
        entity.setCategoryCode("PERIPHERALS");
        entity.setPrice(new BigDecimal(price));
        entity.setCurrencyCode("BRL");
        return entity;
    }

    private static <T extends Throwable> T findCause(Throwable error, Class<T> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
        }
        return null;
    }

    private static String uniqueSku() {
        return "IT-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
    }
}
