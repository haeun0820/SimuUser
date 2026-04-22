package com.example.simuuser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/scenario")
    public String scenario() {
        return "scenario/scenario";
    }

    @PostMapping("/scenario/result")
    public String scenarioResultPost(@RequestParam(value = "compareTitle", required = false) String compareTitle, Model model) {
        model.addAttribute("title", compareTitle);
        return "scenario/scenario_result";
    }
}
