package com.example.simuuser.controller;

import org.springframework.stereotype.Controller; // 이 import가 있는지 확인하세요!
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // 💡 이 어노테이션이 없으면 컨트롤러로 작동하지 않습니다!
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/user/detail")
    public String userDetail(Model model) {
        return "admin/user/user_detail"; // templates/admin/user/user_detail.html
    }
}