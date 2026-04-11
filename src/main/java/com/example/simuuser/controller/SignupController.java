package com.example.simuuser.controller;

import com.example.simuuser.dto.SignupRequest;
import com.example.simuuser.service.AppUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class SignupController {

    private final AppUserService appUserService;

    public SignupController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/signup/check-user-id")
    @ResponseBody
    public Map<String, Object> checkUserId(@RequestParam String userId) {
        boolean available = appUserService.isUserIdAvailable(userId);
        String message = available ? "사용 가능한 아이디입니다." : "이미 사용 중이거나 입력할 수 없는 아이디입니다.";

        return Map.of(
                "available", available,
                "message", message
        );
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute SignupRequest request, Model model, RedirectAttributes redirectAttributes) {
        try {
            appUserService.signup(request);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("signupRequest", request);
            return "signup/signup";
        }

        redirectAttributes.addFlashAttribute("signupSuccessMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/login";
    }
}
