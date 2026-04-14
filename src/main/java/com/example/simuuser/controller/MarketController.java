package com.example.simuuser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MarketController {

    @GetMapping("/market/market")
    public String market() {
        return "market/market";  // market.html
    }

    @GetMapping("/market/result")
    public String marketResult() {
        return "market/market_result";  // market_result.html
    }
}