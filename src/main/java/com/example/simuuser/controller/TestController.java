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

}
