package com.ecommerce.auth.service;

import com.ecommerce.auth.domain.entity.Permission;
import com.ecommerce.auth.domain.repository.PermissionRepository;
import com.ecommerce.auth.dto.request.CreatePermissionRequest;
import com.ecommerce.auth.dto.request.UpdatePermissionRequest;
import com.ecommerce.auth.dto.response.PermissionResponse;
import com.ecommerce.auth.exception.BadRequestException;
import com.ecommerce.auth.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional
    public PermissionResponse create(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new BadRequestException("Permission already exists: " + request.getName());
        }
        Permission permission = Permission.builder()
                .name(request.getName().toUpperCase())
                .description(request.getDescription())
                .module(request.getModule().toUpperCase())
                .build();
        return toResponse(permissionRepository.save(permission));
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAll() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getByModule(String module) {
        return permissionRepository.findByModule(module.toUpperCase()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Map<String, List<PermissionResponse>> getAllGroupedByModule() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.groupingBy(PermissionResponse::getModule));
    }

    @Transactional
    public PermissionResponse update(Long id, UpdatePermissionRequest request) {
        Permission permission = findOrThrow(id);
        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }
        if (request.getModule() != null) {
            permission.setModule(request.getModule().toUpperCase());
        }
        return toResponse(permissionRepository.save(permission));
    }

    @Transactional
    public void delete(Long id) {
        Permission permission = findOrThrow(id);
        permissionRepository.delete(permission);
    }

    private Permission findOrThrow(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Permission not found: " + id));
    }

    public PermissionResponse toResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .module(p.getModule())
                .createdAt(p.getCreatedAt())
                .build();
    }
}