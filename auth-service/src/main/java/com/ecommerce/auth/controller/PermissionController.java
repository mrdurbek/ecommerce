package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.request.CreatePermissionRequest;
import com.ecommerce.auth.dto.request.UpdatePermissionRequest;
import com.ecommerce.auth.dto.response.ApiResponse;
import com.ecommerce.auth.dto.response.PermissionResponse;
import com.ecommerce.auth.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_MANAGE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Permissions", description = "Permission management (Admin only)")
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @Operation(summary = "Create a new permission")
    public ResponseEntity<ApiResponse<PermissionResponse>> create(
            @Valid @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Permission created", permissionService.create(request)));
    }

    @GetMapping
    @Operation(summary = "Get all permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved", permissionService.getAll()));
    }

    @GetMapping("/grouped")
    @Operation(summary = "Get all permissions grouped by module")
    public ResponseEntity<ApiResponse<Map<String, List<PermissionResponse>>>> getAllGrouped() {
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved",
                permissionService.getAllGroupedByModule()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get permission by ID")
    public ResponseEntity<ApiResponse<PermissionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Permission retrieved", permissionService.getById(id)));
    }

    @GetMapping("/module/{module}")
    @Operation(summary = "Get permissions by module")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getByModule(@PathVariable String module) {
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved",
                permissionService.getByModule(module)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update permission description or module")
    public ResponseEntity<ApiResponse<PermissionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Permission updated",
                permissionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a permission")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Permission deleted"));
    }
}