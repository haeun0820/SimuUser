package com.example.simuuser.controller;

import com.example.simuuser.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @ResponseBody
    @GetMapping("/api/dashboard/analytics")
    public ResponseEntity<?> analytics(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getAnalytics(authentication));
    }
}
