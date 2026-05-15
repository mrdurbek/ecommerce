package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.request.*;
import com.ecommerce.inventory.dto.response.*;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inventory", description = "Inventory management")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get inventory by product ID")
    public ResponseEntity<ApiResponse<InventoryResponse>> getByProductId(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(ApiResponse.success("OK", inventoryService.getByProductId(productId)));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get inventory by SKU")
    public ResponseEntity<ApiResponse<InventoryResponse>> getBySku(@PathVariable("sku") String sku) {
        return ResponseEntity.ok(ApiResponse.success("OK", inventoryService.getBySku(sku)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @Operation(summary = "[ADMIN] Create inventory item")
    public ResponseEntity<ApiResponse<InventoryResponse>> create(
            @Valid @RequestBody CreateInventoryRequest req,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created", inventoryService.create(req, userId)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('INVENTORY_VIEW','INVENTORY_MANAGE')")
    @Operation(summary = "[ADMIN] List all inventory items")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getAll(
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("OK", inventoryService.getAll(active, page, size)));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyAuthority('INVENTORY_VIEW','INVENTORY_MANAGE')")
    @Operation(summary = "[ADMIN] Get low stock items")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.success("OK", inventoryService.getLowStockItems()));
    }

    @PostMapping("/product/{productId}/add-stock")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @Operation(summary = "[ADMIN] Add stock")
    public ResponseEntity<ApiResponse<InventoryResponse>> addStock(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody AddStockRequest req,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(ApiResponse.success("Stock added", inventoryService.addStock(productId, req, userId)));
    }

    @PatchMapping("/product/{productId}/adjust")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @Operation(summary = "[ADMIN] Adjust stock (+ or -)")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody StockAdjustRequest req,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(ApiResponse.success("Adjusted", inventoryService.adjustStock(productId, req, userId)));
    }

    @PatchMapping("/product/{productId}/activate")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @Operation(summary = "[ADMIN] Activate inventory item")
    public ResponseEntity<ApiResponse<InventoryResponse>> activate(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Activated", inventoryService.setActive(productId, true)));
    }

    @PatchMapping("/product/{productId}/deactivate")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @Operation(summary = "[ADMIN] Deactivate inventory item")
    public ResponseEntity<ApiResponse<InventoryResponse>> deactivate(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Deactivated", inventoryService.setActive(productId, false)));
    }

    @GetMapping("/product/{productId}/movements")
    @PreAuthorize("hasAnyAuthority('INVENTORY_VIEW','INVENTORY_MANAGE')")
    @Operation(summary = "[ADMIN] Get stock movement history")
    public ResponseEntity<ApiResponse<PageResponse<StockMovementResponse>>> getMovements(
            @PathVariable("productId") Long productId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("OK", inventoryService.getMovements(productId, page, size)));
    }
}