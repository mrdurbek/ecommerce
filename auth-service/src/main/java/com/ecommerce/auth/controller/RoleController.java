package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.request.CreateRoleRequest;
import com.ecommerce.auth.dto.request.UpdateRoleRequest;
import com.ecommerce.auth.dto.response.ApiResponse;
import com.ecommerce.auth.dto.response.RoleResponse;
import com.ecommerce.auth.service.RoleService;
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
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_MANAGE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles", description = "Role management (Admin only)")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @Operation(summary = "Create a new role")
    public ResponseEntity<ApiResponse<RoleResponse>> create(
            @Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created", roleService.create(request)));
    }

    @GetMapping
    @Operation(summary = "Get all roles with their permissions")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved", roleService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Role retrieved", roleService.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role description and/or permissions")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Role updated", roleService.update(id, request)));
    }

    @PostMapping("/{id}/permissions/add")
    @Operation(summary = "Add permissions to a role")
    public ResponseEntity<ApiResponse<RoleResponse>> addPermissions(
            @PathVariable Long id,
            @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(ApiResponse.success("Permissions added",
                roleService.addPermissions(id, permissionIds)));
    }

    @PostMapping("/{id}/permissions/remove")
    @Operation(summary = "Remove permissions from a role")
    public ResponseEntity<ApiResponse<RoleResponse>> removePermissions(
            @PathVariable Long id,
            @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(ApiResponse.success("Permissions removed",
                roleService.removePermissions(id, permissionIds)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a role (system roles cannot be deleted)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Role deleted"));
    }
}