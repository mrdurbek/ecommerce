package com.ecommerce.inventory.service;

import com.ecommerce.inventory.domain.entity.InventoryItem;
import com.ecommerce.inventory.domain.entity.StockMovement;
import com.ecommerce.inventory.domain.enums.MovementType;
import com.ecommerce.inventory.domain.repository.InventoryItemRepository;
import com.ecommerce.inventory.domain.repository.StockMovementRepository;
import com.ecommerce.inventory.dto.request.*;
import com.ecommerce.inventory.dto.response.*;
import com.ecommerce.inventory.exception.BadRequestException;
import com.ecommerce.inventory.exception.DuplicateResourceException;
import com.ecommerce.inventory.exception.ResourceNotFoundException;
import com.ecommerce.inventory.messaging.event.OrderCancelledEvent;
import com.ecommerce.inventory.messaging.event.OrderCreatedEvent;
import com.ecommerce.inventory.messaging.event.StockReservationFailedEvent;
import com.ecommerce.inventory.messaging.event.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository movementRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${messaging.exchange.inventory}")
    private String inventoryExchange;

    @Value("${messaging.routing-key.stock-reserved}")
    private String stockReservedKey;

    @Value("${messaging.routing-key.stock-reservation-failed}")
    private String stockReservationFailedKey;

    @Transactional
    public InventoryResponse create(CreateInventoryRequest req, Long userId) {
        if (inventoryItemRepository.existsByProductId(req.getProductId()))
            throw new DuplicateResourceException("Inventory already exists for product: " + req.getProductId());
        if (inventoryItemRepository.existsBySku(req.getSku()))
            throw new DuplicateResourceException("SKU already exists: " + req.getSku());

        InventoryItem item = InventoryItem.builder()
                .productId(req.getProductId())
                .productName(req.getProductName())
                .sku(req.getSku())
                .quantityAvailable(req.getInitialQuantity())
                .lowStockThreshold(req.getLowStockThreshold() != null ? req.getLowStockThreshold() : 10)
                .build();
        item = inventoryItemRepository.save(item);

        if (req.getInitialQuantity() > 0)
            logMovement(item, MovementType.IN, req.getInitialQuantity(), "INITIAL", null, "Initial stock", userId);

        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(Long productId) {
        return toResponse(findOrThrow(productId));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getBySku(String sku) {
        return toResponse(inventoryItemRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for sku: " + sku)));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getAll(Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<InventoryItem> result = active != null
                ? inventoryItemRepository.findByIsActive(active, pageable)
                : inventoryItemRepository.findAll(pageable);
        return PageResponse.of(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockItems() {
        return inventoryItemRepository.findLowStockItems().stream().map(this::toResponse).toList();
    }

    @Transactional
    public InventoryResponse addStock(Long productId, AddStockRequest req, Long userId) {
        InventoryItem item = inventoryItemRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + productId));
        item.addStock(req.getQuantity());
        item = inventoryItemRepository.save(item);
        logMovement(item, MovementType.IN, req.getQuantity(), "RESTOCK", null, req.getNote(), userId);
        return toResponse(item);
    }

    @Transactional
    public InventoryResponse adjustStock(Long productId, StockAdjustRequest req, Long userId) {
        InventoryItem item = inventoryItemRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + productId));
        try {
            item.adjust(req.getDelta());
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
        item = inventoryItemRepository.save(item);
        logMovement(item, MovementType.ADJUST, Math.abs(req.getDelta()), "ADJUSTMENT", null, req.getReason(), userId);
        return toResponse(item);
    }

    @Transactional
    public InventoryResponse setActive(Long productId, boolean active) {
        InventoryItem item = findOrThrow(productId);
        item.setIsActive(active);
        return toResponse(inventoryItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> getMovements(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.of(movementRepository
                .findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toMovementResponse));
    }

    private InventoryItem findOrThrow(Long productId) {
        return inventoryItemRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
    }

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        List<String> failures = new ArrayList<>();

        for(OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
            InventoryItem inv = inventoryItemRepository.findByProductIdWithLock(item.getProductId()).orElse(null);
            if(inv == null) {
                failures.add("Product not found : "+ item.getProductId());
            } else if(!inv.getIsActive()) {
                failures.add("Product inactive: " + inv.getSku());
            } else if(!inv.hasEnoughStock(item.getQuantity())) {
                failures.add(String.format("Insufficient stock: sku=%s available=%d required=%d",
                        inv.getSku(), inv.getQuantityAvailable(), item.getQuantity()));
            }
        }

        if(!failures.isEmpty()) {
            String reason = String.join("; ", failures);

            log.warn("Stock reservation failed for order {}: {}", event.getOrderNumber(), reason);

            rabbitTemplate.convertAndSend(inventoryExchange, stockReservationFailedKey,
                    StockReservationFailedEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .orderId(event.getOrderId())
                            .orderNumber(event.getOrderNumber())
                            .reason(reason)
                            .failedAt(LocalDateTime.now())
                            .build()
            );
            return;
        }

        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
            InventoryItem inv = inventoryItemRepository.findByProductIdWithLock(item.getProductId()).orElseThrow();
            inv.reserve(item.getQuantity());
            inventoryItemRepository.save(inv);

            logMovement(inv, MovementType.RESERVE, item.getQuantity(), "ORDER", event.getOrderNumber(), "Reserved for order");

            if (inv.isLowStock()) {
                log.warn("LOW STOCK: sku={}, available={}", inv.getSku(), inv.getQuantityAvailable());
            }
        }

        log.info("Stock reserved for order: {}", event.getOrderNumber());

        List<StockReservedEvent.StockItemEvent> reservedItems = event.getItems().stream()
                .map(item -> StockReservedEvent.StockItemEvent.builder()
                        .productId(item.getProductId())
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        rabbitTemplate.convertAndSend(inventoryExchange, stockReservedKey,
                StockReservedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderNumber(event.getOrderNumber())
                        .orderId(event.getOrderId())
                        .success(true)
                        .items(reservedItems)
                        .reservedAt(LocalDateTime.now())
                        .build());

    }

    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        List<StockMovement> reservations = movementRepository.findByReferenceTypeAndReferenceId("ORDER", event.getOrderNumber())
                .stream()
                .filter(m -> m.getMovementType() == MovementType.RESERVE)
                .toList();

        if(reservations.isEmpty()) {
            log.warn("No reservations found for cancelled order: {}", event.getOrderNumber());
            return;
        }

        for(StockMovement reservation: reservations) {
            InventoryItem inv = inventoryItemRepository.findByProductIdWithLock(reservation.getProductId()).orElse(null);
            if(inv == null) continue;

            inv.release(reservation.getQuantity());
            inventoryItemRepository.save(inv);
            logMovement(inv, MovementType.RELEASE, reservation.getQuantity(),
                    "ORDER", event.getOrderNumber(), "Released - order cancelled");

            log.info("Stock released for cancelled order: {}", event.getOrderNumber());
        }
    }

    private void logMovement(InventoryItem item, MovementType type, int qty,
                             String refType, String refId, String note, Long userId) {
        movementRepository.save(StockMovement.builder()
                .inventoryItem(item)
                .productId(item.getProductId())
                .movementType(type)
                .quantity(qty)
                .referenceType(refType)
                .referenceId(refId)
                .note(note)
                .build());
    }

    private void logMovement(InventoryItem item, MovementType type, int qty,
                             String refType, String refId, String note) {
        logMovement(item, type, qty, refType, refId, note, null);
    }

    public InventoryResponse toResponse(InventoryItem item) {
        return InventoryResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .sku(item.getSku())
                .quantityAvailable(item.getQuantityAvailable())
                .quantityReserved(item.getQuantityReserved())
                .lowStockThreshold(item.getLowStockThreshold())
                .isActive(item.getIsActive())
                .isLowStock(item.isLowStock())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private StockMovementResponse toMovementResponse(StockMovement m) {
        return StockMovementResponse.builder()
                .id(m.getId())
                .productId(m.getProductId())
                .movementType(m.getMovementType())
                .quantity(m.getQuantity())
                .referenceType(m.getReferenceType())
                .referenceId(m.getReferenceId())
                .note(m.getNote())
                .createdAt(m.getCreatedAt())
                .build();
    }
}