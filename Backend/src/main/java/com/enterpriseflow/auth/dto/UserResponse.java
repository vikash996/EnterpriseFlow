package com.enterpriseflow.auth.dto;

import com.enterpriseflow.entity.User;
import com.enterpriseflow.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, UserRole role, String membershipStatus, UUID companyId, String companyName, Instant createdAt) {

    public UserResponse(UUID id, String name, String email, UserRole role, Instant createdAt) {
        this(id, name, email, role, "ACTIVE", null, null, createdAt);
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getMembershipStatus().name(), user.getCompany() == null ? null : user.getCompany().getId(), user.getCompany() == null ? null : user.getCompany().getName(), user.getCreatedAt());
    }
}
