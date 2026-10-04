package com.enterpriseflow.controller;

import com.enterpriseflow.api.ApiResponse;
import com.enterpriseflow.auth.dto.UserResponse;
import com.enterpriseflow.service.UserService;
import com.enterpriseflow.user.dto.AdminRoleUpdateRequest;
import com.enterpriseflow.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(Authentication authentication) {
        return ApiResponse.success("User profile retrieved.", userService.getCurrentUser(authentication.getName()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success("User profile updated.", userService.updateCurrentUser(authentication.getName(), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllUsers(Authentication authentication) {
        return ApiResponse.success("Users retrieved.", userService.getAllUsers(authentication.getName()));
    }

    @GetMapping("/join-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<java.util.Map<String, Object>>> pendingJoinRequests(Authentication authentication) {
        return ApiResponse.success("Pending join requests retrieved.", userService.pendingRequests(authentication.getName()));
    }

    @PatchMapping("/join-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> approveJoinRequest(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success("Join request approved.", userService.decideJoinRequest(authentication.getName(), id, true));
    }

    @PatchMapping("/join-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> rejectJoinRequest(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success("Join request rejected.", userService.decideJoinRequest(authentication.getName(), id, false));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getUser(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success("User retrieved.", userService.getUser(authentication.getName(), id));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUserRole(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody AdminRoleUpdateRequest request
    ) {
        return ApiResponse.success("User role updated.", userService.updateUserRole(authentication.getName(), id, request.role()));
    }
}
