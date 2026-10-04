package com.enterpriseflow.controller;

import com.enterpriseflow.api.ApiResponse;
import com.enterpriseflow.commandcenter.dto.CommandCenterResponse;
import com.enterpriseflow.service.CommandCenterService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/command-center")
public class CommandCenterController {
    private final CommandCenterService service;

    public CommandCenterController(CommandCenterService service) { this.service = service; }

    @GetMapping
    public ApiResponse<CommandCenterResponse> get(Authentication authentication) {
        return ApiResponse.success("Command Center retrieved.", service.get(authentication.getName()));
    }
}
