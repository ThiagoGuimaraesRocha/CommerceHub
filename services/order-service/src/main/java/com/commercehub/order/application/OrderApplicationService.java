package com.commercehub.order.application;

import com.commercehub.order.api.dto.CancelOrderRequest;
import com.commercehub.order.api.dto.CancellationReasonResponse;
import com.commercehub.order.api.dto.CreateOrderRequest;
import com.commercehub.order.api.dto.OrderItemRequest;
import com.commercehub.order.api.dto.OrderResponse;
import com.commercehub.order.domain.Money;
import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.entity.OrderItemEntity;
import com.commercehub.order.domain.enumtype.CancellationReason;
import com.commercehub.order.exception.CurrencyMismatchException;
import com.commercehub.order.exception.DuplicateProductInOrderException;
import com.commercehub.order.exception.OrderNotFoundException;
import com.commercehub.order.exception.OrderNotOwnedException;
import com.commercehub.order.exception.RemoteProductServiceException;
import com.commercehub.order.domain.enumtype.OrderStatus;
import com.commercehub.order.exception.UnknownProductException;
import com.commercehub.order.infrastructure.client.ProductClient;
import com.commercehub.order.infrastructure.client.ProductSnapshotResponse;
import com.commercehub.order.infrastructure.messaging.KafkaTopics;
import com.commercehub.order.infrastructure.messaging.outbox.OutboxWriter;
import com.commercehub.order.infrastructure.messaging.payload.OrderCancelledPayload;
import com.commercehub.order.infrastructure.messaging.payload.OrderConfirmedPayload;
import com.commercehub.order.infrastructure.messaging.payload.ReleaseInventoryPayload;
import com.commercehub.order.infrastructure.persistence.OrderRepository;
import com.commercehub.order.mapper.OrderMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class OrderApplicationService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OrderCalculator calculator;
    private final OrderStateTransitionService transitions;
    private final OrderCancellationService cancellationService;
    private final OutboxWriter outboxWriter;
    private final ProductClient productClient;

    public OrderApplicationService(
            OrderRepository repository,
            OrderMapper mapper,
            OrderCalculator calculator,
            OrderStateTransitionService transitions,
            OrderCancellationService cancellationService,
            OutboxWriter outboxWriter,
            @RestClient ProductClient productClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.calculator = calculator;
        this.transitions = transitions;
        this.cancellationService = cancellationService;
        this.outboxWriter = outboxWriter;
        this.productClient = productClient;
    }

    @Transactional
    public OrderResponse create(String customerId, CreateOrderRequest request) {
        assertUniqueProducts(request.items());

        OrderEntity order = OrderEntity.newOrder();
        order.setCustomerId(customerId.trim());

        String currency = null;
        for (OrderItemRequest itemRequest : request.items()) {
            ProductSnapshotResponse product = fetchActiveProduct(itemRequest.productId());
            if (currency == null) {
                currency = product.currencyCode() == null ? Money.DEFAULT_CURRENCY : product.currencyCode();
            } else if (!currency.equals(product.currencyCode())) {
                throw new CurrencyMismatchException(currency, product.currencyCode());
            }

            BigDecimal unitPrice = Money.normalize(product.price());
            BigDecimal lineTotal = calculator.lineTotal(unitPrice, itemRequest.quantity());

            OrderItemEntity item = OrderItemEntity.newItem();
            item.setProductId(product.id());
            item.setSku(product.sku());
            item.setProductName(product.name());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(unitPrice);
            item.setLineTotal(lineTotal);
            order.addItem(item);
        }

        order.setCurrencyCode(currency);
        order.setTotalAmount(calculator.orderTotal(order.getItems().stream().map(OrderItemEntity::getLineTotal).toList()));
        repository.persistAndFlush(order);
        return mapper.toResponse(order);
    }

    public OrderResponse findById(String id, String customerId) {
        return mapper.toResponse(loadOwned(id, customerId));
    }

    public List<OrderResponse> findByCustomerId(String customerId) {
        return mapper.toResponses(repository.findByCustomerId(customerId));
    }

    @Transactional
    public OrderResponse confirm(String id, String customerId) {
        OrderEntity order = loadOwned(id, customerId);
        transitions.confirm(order);
        outboxWriter.writeEvent("OrderConfirmed", KafkaTopics.ORDER_EVENTS, order.getId(), null, toConfirmedPayload(order));
        return mapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(String id, String customerId, CancelOrderRequest request) {
        OrderEntity order = loadOwned(id, customerId);
        OrderStatus previousStatus = order.getStatus();
        cancellationService.cancelByCustomer(order, request);
        outboxWriter.writeEvent("OrderCancelled", KafkaTopics.ORDER_EVENTS, order.getId(), null,
                new OrderCancelledPayload(order.getId(), previousStatus.name(),
                        order.getCancellationReason().name(), "CUSTOMER"));
        if (previousStatus == OrderStatus.INVENTORY_RESERVED) {
            outboxWriter.writeCommand("ReleaseInventory", KafkaTopics.INVENTORY_COMMANDS, order.getId(), null,
                    new ReleaseInventoryPayload(order.getId(), ReleaseInventoryPayload.REASON_CUSTOMER_CANCELLED));
        }
        return mapper.toResponse(order);
    }

    private static OrderConfirmedPayload toConfirmedPayload(OrderEntity order) {
        List<OrderConfirmedPayload.Item> items = order.getItems().stream()
                .map(item -> new OrderConfirmedPayload.Item(item.getProductId(), item.getQuantity()))
                .toList();
        return new OrderConfirmedPayload(order.getId(), order.getCustomerId(), items,
                Money.normalize(order.getTotalAmount()), order.getCurrencyCode());
    }

    public List<CancellationReasonResponse> cancellationReasons() {
        return CancellationReason.customerSelectableReasons().stream()
                .map(reason -> new CancellationReasonResponse(reason.name(), reason.label(), reason.isNoteRequired()))
                .toList();
    }

    private ProductSnapshotResponse fetchActiveProduct(String productId) {
        try {
            ProductSnapshotResponse product = productClient.getById(productId);
            if (product == null || !product.active()) {
                throw new UnknownProductException(productId);
            }
            return product;
        } catch (UnknownProductException e) {
            throw new UnknownProductException(productId);
        } catch (TimeoutException | CircuitBreakerOpenException e) {
            throw new RemoteProductServiceException("Product Service is unavailable", e);
        } catch (RemoteProductServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RemoteProductServiceException("Product Service is unavailable", e);
        }
    }

    private void assertUniqueProducts(List<OrderItemRequest> items) {
        Set<String> seen = new HashSet<>();
        for (OrderItemRequest item : items) {
            if (!seen.add(item.productId())) {
                throw new DuplicateProductInOrderException(item.productId());
            }
        }
    }

    private OrderEntity load(String id) {
        return repository.findByIdOptional(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private OrderEntity loadOwned(String id, String customerId) {
        OrderEntity order = load(id);
        if (!order.getCustomerId().equals(customerId)) {
            throw new OrderNotOwnedException(order.getId());
        }
        return order;
    }
}
