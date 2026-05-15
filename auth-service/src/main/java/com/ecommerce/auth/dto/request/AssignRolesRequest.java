package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class AssignRolesRequest {

    @NotEmpty(message = "At least one role ID is required")
    private Set<Long> roleIds;
}