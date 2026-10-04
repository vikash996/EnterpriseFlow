package com.enterpriseflow.user.dto;

import com.enterpriseflow.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record AdminRoleUpdateRequest(@NotNull UserRole role) {
}
