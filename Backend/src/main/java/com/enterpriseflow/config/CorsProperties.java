package com.enterpriseflow.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.cors")
public record CorsProperties(@NotBlank String allowedOrigin) {
}
