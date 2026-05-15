package com.ecommerce.auth.controller;

import com.ecommerce.auth.domain.entity.Role;
import com.ecommerce.auth.domain.entity.User;
import com.ecommerce.auth.domain.repository.RoleRepository;
import com.ecommerce.auth.domain.repository.UserRepository;
import com.ecommerce.auth.dto.request.AssignRolesRequest;
import com.ecommerce.auth.dto.response.ApiResponse;
import com.ecommerce.auth.dto.response.UserResponse;
import com.ecommerce.auth.exception.BadRequestException;
import com.ecommerce.auth.exception.UserNotFoundException;
import com.ecommerce.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_MANAGE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "User management (Admin only)")
public class UserAdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserResponse> users = userRepository.findAll(pageable)
                .map(authService::mapToUserResponse);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        return ResponseEntity.ok(ApiResponse.success("User retrieved", authService.mapToUserResponse(user)));
    }

    @PutMapping("/{userId}/roles")
    @Operation(summary = "Replace all roles of a user")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRolesRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        Set<Role> roles = roleRepository.findByIdIn(request.getRoleIds());
        if (roles.size() != request.getRoleIds().size()) {
            throw new BadRequestException("One or more role IDs are invalid");
        }

        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Roles assigned", authService.mapToUserResponse(user)));
    }

    @PostMapping("/{userId}/roles/add")
    @Operation(summary = "Add roles to a user")
    public ResponseEntity<ApiResponse<UserResponse>> addRoles(
            @PathVariable Long userId,
            @RequestBody Set<Long> roleIds
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        Set<Role> toAdd = roleRepository.findByIdIn(roleIds);
        user.getRoles().addAll(toAdd);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Roles added", authService.mapToUserResponse(user)));
    }

    @PostMapping("/{userId}/roles/remove")
    @Operation(summary = "Remove roles from a user")
    public ResponseEntity<ApiResponse<UserResponse>> removeRoles(
            @PathVariable Long userId,
            @RequestBody Set<Long> roleIds
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        user.getRoles().removeIf(r -> roleIds.contains(r.getId()));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Roles removed", authService.mapToUserResponse(user)));
    }

    @PatchMapping("/{userId}/enable")
    @Operation(summary = "Enable a user account")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        user.setIsEnabled(true);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User enabled"));
    }

    @PatchMapping("/{userId}/disable")
    @Operation(summary = "Disable a user account")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        user.setIsEnabled(false);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User disabled"));
    }
}