package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 50)
    @Pattern(
            regexp = "^ROLE_[A-Z][A-Z0-9_]*$",
            message = "Role name must start with ROLE_ and be uppercase (e.g. ROLE_MANAGER)"
    )
    private String name;

    @Size(max = 255)
    private String description;

    private Set<Long> permissionIds;
}