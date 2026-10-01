package com.commercehub.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercehub.order.domain.Money;
import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.entity.OrderItemEntity;
import com.commercehub.order.domain.enumtype.CancellationReason;
import com.commercehub.order.domain.enumtype.CancelledBy;
import com.commercehub.order.domain.enumtype.OrderStatus;
import com.commercehub.order.infrastructure.persistence.OrderRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderRepositoryIT {

    @Inject
    OrderRepository repository;

    @Test
    void persistsOrderWithItemsAndReadsSnapshot() {
        String id = QuarkusTransaction.requiringNew().call(() -> {
            OrderEntity order = sampleOrder();
            repository.persist(order);
            return order.getId();
        });

        OrderEntity loaded = QuarkusTransaction.requiringNew().call(() -> repository.findById(id));
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(loaded.getTotalAmount()).isEqualByComparingTo("21.0000");
        assertThat(loaded.getItems()).hasSize(1);
        assertThat(loaded.getItems().getFirst().getSku()).isEqualTo("KB-001");
        assertThat(loaded.getItems().getFirst().getProductName()).isEqualTo("Keyboard");
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void databaseEnforcesUniqueProductPerOrder() {
        String orderId = QuarkusTransaction.requiringNew().call(() -> {
            OrderEntity order = sampleOrder();
            repository.persist(order);
            return order.getId();
        });
        String productId = QuarkusTransaction.requiringNew().call(() ->
                repository.findById(orderId).getItems().getFirst().getProductId());

        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> {
            OrderEntity order = repository.findById(orderId);
            OrderItemEntity duplicate = OrderItemEntity.newItem();
            duplicate.setProductId(productId);
            duplicate.setSku("OTHER");
            duplicate.setProductName("Other");
            duplicate.setQuantity(1);
            duplicate.setUnitPrice(Money.normalize(BigDecimal.ONE));
            duplicate.setLineTotal(Money.normalize(BigDecimal.ONE));
            order.addItem(duplicate);
            repository.flush();
        })).satisfies(e -> assertThat(findCause(e, ConstraintViolationException.class)).isNotNull());
    }

    @Test
    void databaseEnforcesCancellationConstraints() {
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> {
            OrderEntity order = sampleOrder();
            order.setStatus(OrderStatus.CANCELLED);
            // missing reason / cancelledBy / cancelledAt should fail ck_orders_cancel_fields
            repository.persistAndFlush(order);
        })).satisfies(e -> assertThat(findCause(e, ConstraintViolationException.class)).isNotNull());
    }

    @Test
    void cancelledOrderPersistsAuditFields() {
        String id = QuarkusTransaction.requiringNew().call(() -> {
            OrderEntity order = sampleOrder();
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason(CancellationReason.CHANGED_MIND);
            order.setCancelledBy(CancelledBy.CUSTOMER);
            order.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));
            repository.persist(order);
            return order.getId();
        });

        OrderEntity loaded = QuarkusTransaction.requiringNew().call(() -> repository.findById(id));
        assertThat(loaded.getCancellationReason()).isEqualTo(CancellationReason.CHANGED_MIND);
        assertThat(loaded.getCancelledBy()).isEqualTo(CancelledBy.CUSTOMER);
        assertThat(loaded.getCancelledAt()).isNotNull();
    }

    @Test
    void otherWithoutNoteIsRejectedByDatabase() {
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> {
            OrderEntity order = sampleOrder();
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason(CancellationReason.OTHER);
            order.setCancellationNote(null);
            order.setCancelledBy(CancelledBy.CUSTOMER);
            order.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));
            repository.persistAndFlush(order);
        })).satisfies(e -> assertThat(findCause(e, ConstraintViolationException.class)).isNotNull());
    }

    private static OrderEntity sampleOrder() {
        OrderEntity order = OrderEntity.newOrder();
        order.setCustomerId(UUID.randomUUID().toString());
        order.setCurrencyCode("BRL");
        order.setTotalAmount(Money.normalize(new BigDecimal("21")));

        OrderItemEntity item = OrderItemEntity.newItem();
        item.setProductId(UUID.randomUUID().toString());
        item.setSku("KB-001");
        item.setProductName("Keyboard");
        item.setQuantity(2);
        item.setUnitPrice(Money.normalize(new BigDecimal("10.5")));
        item.setLineTotal(Money.normalize(new BigDecimal("21")));
        order.addItem(item);
        return order;
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
