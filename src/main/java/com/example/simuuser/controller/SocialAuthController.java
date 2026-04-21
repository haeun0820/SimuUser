package com.example.simuuser.controller;

import com.example.simuuser.dto.ProfileResponse;
import com.example.simuuser.dto.SocialOnboardingRequest;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.service.AppUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class SocialAuthController {

    private final AppUserService appUserService;

    public SocialAuthController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @ResponseBody
    @PostMapping("/api/auth/social-onboarding")
    public ResponseEntity<?> completeSocialOnboarding(
            @RequestBody SocialOnboardingRequest request,
            Authentication authentication
    ) {
        try {
            AppUser user = appUserService.completeSocialOnboarding(request, authentication);
            return ResponseEntity.ok(new ProfileResponse(user));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
