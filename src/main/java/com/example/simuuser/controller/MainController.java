package com.example.simuuser.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        model.addAttribute("isLoggedIn", isLoggedIn);

        if (isLoggedIn) {
            model.addAttribute("displayName", getDisplayName(authentication));
        }

        return "main/main";
    }

    private String getDisplayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User oauth2User) {
            String name = oauth2User.getAttribute("name");
            return name == null || name.isBlank() ? authentication.getName() : name;
        }

        return authentication.getName();
    }

    @GetMapping("/login")
    public String login() {
        return "login/login";
    }

    @GetMapping("/social-onboarding")
    public String socialOnboarding() {
        return "login/login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup/signup";
    }
}
