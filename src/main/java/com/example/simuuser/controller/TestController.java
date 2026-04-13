package com.example.simuuser.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TestController {
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/dashboard";
    }

            // {projectId} 부분이 URL의 숫자를 받아주는 변수 역할을 합니다.
    @GetMapping("/project/detail/{projectId}")
    public String projectDetail(@PathVariable("projectId") String projectId, Model model) {
        // 받아온 ID를 타임리프 페이지로 넘겨줍니다.
        model.addAttribute("projectId", projectId);
        return "project/exact_project"; // 실제 html 파일 경로
    }
}
