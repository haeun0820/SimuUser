package com.example.simuuser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProjectController {

    @GetMapping("/project/new")
    public String newProject() {
        return "project/new_project";
    }

    @GetMapping("/project/all")
    public String allProject() {
        return "project/all_project";
    }
}