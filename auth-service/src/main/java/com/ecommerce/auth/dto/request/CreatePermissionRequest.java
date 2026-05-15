package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePermissionRequest {

    @NotBlank(message = "Permission name is required")
    @Size(max = 100)
    @Pattern(
            regexp = "^[A-Z][A-Z0-9_]*$",
            message = "Permission name must be uppercase with underscores (e.g. PRODUCT_CREATE)"
    )
    private String name;

    @Size(max = 255)
    private String description;

    @NotBlank(message = "Module is required")
    @Size(max = 50)
    private String module;
}