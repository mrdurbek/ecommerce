package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePermissionRequest {

    @Size(max = 255)
    private String description;

    @Size(max = 50)
    private String module;
}