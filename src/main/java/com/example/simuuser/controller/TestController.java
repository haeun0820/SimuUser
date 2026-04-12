package com.example.simuuser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/dashboard";
    }
}
