package com.ecommerce.auth.service;

import com.ecommerce.auth.domain.entity.Permission;
import com.ecommerce.auth.domain.entity.Role;
import com.ecommerce.auth.domain.repository.PermissionRepository;
import com.ecommerce.auth.domain.repository.RoleRepository;
import com.ecommerce.auth.dto.request.CreateRoleRequest;
import com.ecommerce.auth.dto.request.UpdateRoleRequest;
import com.ecommerce.auth.dto.response.PermissionResponse;
import com.ecommerce.auth.dto.response.RoleResponse;
import com.ecommerce.auth.exception.BadRequestException;
import com.ecommerce.auth.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionService permissionService;

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new BadRequestException("Role already exists: " + request.getName());
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions = permissionRepository.findByIdIn(request.getPermissionIds());
        }

        Role role = Role.builder()
                .name(request.getName().toUpperCase())
                .description(request.getDescription())
                .permissions(permissions)
                .build();

        return toResponse(roleRepository.save(role));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        return toResponse(roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new UserNotFoundException("Role not found: " + id)));
    }

    @Transactional
    public RoleResponse update(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new UserNotFoundException("Role not found: " + id));

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = request.getPermissionIds().isEmpty()
                    ? new HashSet<>()
                    : permissionRepository.findByIdIn(request.getPermissionIds());
            role.setPermissions(permissions);
        }

        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse addPermissions(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new UserNotFoundException("Role not found: " + roleId));

        Set<Permission> toAdd = permissionRepository.findByIdIn(permissionIds);
        role.getPermissions().addAll(toAdd);
        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse removePermissions(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new UserNotFoundException("Role not found: " + roleId));

        role.getPermissions().removeIf(p -> permissionIds.contains(p.getId()));
        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Role not found: " + id));

        if (role.getName().equals("ROLE_USER") || role.getName().equals("ROLE_ADMIN")) {
            throw new BadRequestException("Cannot delete system role: " + role.getName());
        }

        roleRepository.delete(role);
    }

    public RoleResponse toResponse(Role role) {
        Set<PermissionResponse> permissionResponses = role.getPermissions() == null
                ? Set.of()
                : role.getPermissions().stream()
                .map(permissionService::toResponse)
                .collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissionResponses)
                .createdAt(role.getCreatedAt())
                .build();
    }
}