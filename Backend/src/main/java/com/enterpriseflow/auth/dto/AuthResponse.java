package com.enterpriseflow.auth.dto;

public record AuthResponse(String accessToken, UserResponse user) {
}
