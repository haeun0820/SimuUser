package com.example.simuuser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TestController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/dashboard";
    }

    @GetMapping("/project/detail/{projectId}")
    public String projectDetail(@PathVariable("projectId") String projectId, Model model) {
        model.addAttribute("projectId", projectId);
        return "project/exact_project";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard/dashboard";
    }

    @GetMapping("/admin/prompt")
    public String adminPrompt() {
        return "admin/aiprompt/prompt_management";
    }

    @GetMapping("/admin/user")
    public String adminUser() {
        return "admin/user/user";
    }

    @GetMapping("/admin/log")
    public String adminLog() {
        return "admin/log/log";
    }

    @GetMapping("/admin/inquiry")
    public String adminInquiry() {
        return "admin/inquiry/inquiry";
    }

    @GetMapping("/admin/inquiry/detail")
    public String inquiryDetail(Model model) {
        return "admin/inquiry/inquiry_detail";
    }

    @GetMapping("/admin/user/detail")
    public String userDetail(Model model) {
        // 실제 개발 시에는 @PathVariable로 userId를 받아와서 DB에서 유저 정보를 조회한 후 넘겨줍니다.
        return "admin/user/user_detail";
    }
}
