package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateRoleRequest {

    @Size(max = 255)
    private String description;

    private Set<Long> permissionIds;
}