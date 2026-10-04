package com.enterpriseflow.controller;

import com.enterpriseflow.api.ApiResponse;
import com.enterpriseflow.service.WorkspaceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final WorkspaceService service;

    public DashboardController(WorkspaceService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(Authentication authentication) {
        return ApiResponse.success("Dashboard retrieved.", service.dashboard(authentication.getName()));
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics(Authentication authentication) {
        return ApiResponse.success("Analytics retrieved.", service.analytics(authentication.getName()));
    }

    @GetMapping("/activity")
    public ApiResponse<List<Map<String, Object>>> activity(Authentication authentication) {
        return ApiResponse.success("Activity retrieved.", service.activity(authentication.getName()));
    }
}
