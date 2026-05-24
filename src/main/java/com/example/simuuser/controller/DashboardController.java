package com.example.simuuser.controller;

import com.example.simuuser.service.DashboardService;
import com.example.simuuser.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final AdminDashboardService adminDashboardService;

    public DashboardController(DashboardService dashboardService, AdminDashboardService adminDashboardService) {
        this.dashboardService = dashboardService;
        this.adminDashboardService = adminDashboardService;
    }

    @ResponseBody
    @GetMapping("/api/dashboard/analytics")
    public ResponseEntity<?> analytics(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getAnalytics(authentication));
    }

    @ResponseBody
    @GetMapping("/api/admin/dashboard/analytics")
    public ResponseEntity<?> adminAnalytics() {
        return ResponseEntity.ok(adminDashboardService.getAnalytics());
    }
}
