package com.enterpriseflow.controller;

import com.enterpriseflow.api.ApiResponse;
import com.enterpriseflow.auth.dto.AuthResponse;
import com.enterpriseflow.auth.dto.LoginRequest;
import com.enterpriseflow.auth.dto.RegisterRequest;
import com.enterpriseflow.auth.dto.UserResponse;
import com.enterpriseflow.auth.dto.CompanyRegistrationRequest;
import com.enterpriseflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Registration successful.", user));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful.", authService.login(request));
    }

    @PostMapping("/register-company")
    public ResponseEntity<ApiResponse<UserResponse>> registerCompany(@Valid @RequestBody CompanyRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Company created and owner account activated.", authService.registerCompany(request)));
    }
}
