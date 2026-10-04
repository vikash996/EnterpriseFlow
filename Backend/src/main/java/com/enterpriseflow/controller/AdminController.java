package com.enterpriseflow.controller;

import com.enterpriseflow.api.ApiResponse;
import com.enterpriseflow.service.NotificationService;
import com.enterpriseflow.service.WorkspaceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final WorkspaceService workspaceService;
    private final NotificationService notificationService;

    public AdminController(WorkspaceService workspaceService, NotificationService notificationService) {
        this.workspaceService = workspaceService;
        this.notificationService = notificationService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(Authentication authentication) {
        return ApiResponse.success("Company overview retrieved.", workspaceService.companyOverview(authentication.getName()));
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics(Authentication authentication) {
        return ApiResponse.success("Company analytics retrieved.", workspaceService.companyOverview(authentication.getName()));
    }

    @GetMapping("/activity")
    public ApiResponse<List<Map<String, Object>>> activity(Authentication authentication) {
        return ApiResponse.success("Company activity retrieved.", workspaceService.companyActivity(authentication.getName()));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> tasks(Authentication authentication) {
        return ApiResponse.success("Company tasks retrieved.", workspaceService.companyTasks(authentication.getName()));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<Map<String, Object>>> notifications(Authentication authentication) {
        return ApiResponse.success("Company notifications retrieved.", notificationService.companyOverview(authentication.getName()));
    }
}
