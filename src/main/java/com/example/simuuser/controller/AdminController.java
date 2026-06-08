package com.example.simuuser.controller;

import com.example.simuuser.service.AdminUserDetailService;
import com.example.simuuser.service.AdminUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;

@Controller
public class AdminController {

    private final AdminUserService adminUserService;
    private final AdminUserDetailService adminUserDetailService;

    public AdminController(AdminUserService adminUserService, AdminUserDetailService adminUserDetailService) {
        this.adminUserService = adminUserService;
        this.adminUserDetailService = adminUserDetailService;
    }

    @ResponseBody
    @GetMapping("/api/admin/users")
    public ResponseEntity<?> users() {
        return ResponseEntity.ok(adminUserService.findAllUsers());
    }

    @ResponseBody
    @GetMapping("/api/admin/users/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "loginMethod", required = false) String loginMethod
    ) {
        byte[] file = adminUserService.exportUsers(query, loginMethod);
        String filename = adminUserService.exportFilename();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @ResponseBody
    @GetMapping("/api/admin/users/{userId}")
    public ResponseEntity<?> userDetailData(@PathVariable("userId") Long userId) {
        try {
            return ResponseEntity.ok(adminUserDetailService.getUserDetail(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/admin/user/detail/{userId}")
    public String userDetail(@PathVariable("userId") Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "admin/user/user_detail";
    }
}
