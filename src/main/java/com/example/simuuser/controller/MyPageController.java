package com.example.simuuser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MyPageController {

    @GetMapping("/mypage")
    public String mypage() {
        // src/main/resources/templates/mypage/mypage.html 파일과 연결
        return "mypage/mypage"; 
    }
}