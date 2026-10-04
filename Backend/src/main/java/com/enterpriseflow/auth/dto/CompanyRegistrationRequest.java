package com.enterpriseflow.auth.dto;
import jakarta.validation.constraints.*;
public record CompanyRegistrationRequest(
        @NotBlank @Size(max = 160) String companyName,
        @NotBlank @Size(max = 150) String ownerName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password) {}
