package com.example.simuuser.controller;

import com.example.simuuser.dto.ProfileResponse;
import com.example.simuuser.dto.ProfileUpdateRequest;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.service.AppUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class MyPageController {

    private final PasswordEncoder passwordEncoder;
    private final AppUserService appUserService;

    public MyPageController(PasswordEncoder passwordEncoder, AppUserService appUserService) {
        this.passwordEncoder = passwordEncoder;
        this.appUserService = appUserService;
    }

    @GetMapping("/mypage")
    public String mypage() {
        return "mypage/mypage";
    }

    @ResponseBody
    @GetMapping("/api/me")
    public ProfileResponse me(Authentication authentication) {
        return new ProfileResponse(appUserService.getCurrentUser(authentication));
    }

    @ResponseBody
    @PutMapping("/api/me")
    @Transactional
    public ResponseEntity<?> updateMe(@RequestBody ProfileUpdateRequest request, Authentication authentication) {
        try {
            AppUser user = appUserService.getCurrentUser(authentication);
            validateProfile(request);

            user.updateProfile(
                    request.getName().trim(),
                    request.getPhone().trim(),
                    request.getBirthDate(),
                    request.getGender(),
                    normalize(request.getProfileImage())
            );

            updatePasswordIfRequested(user, request);

            return ResponseEntity.ok(new ProfileResponse(user));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private void updatePasswordIfRequested(AppUser user, ProfileUpdateRequest request) {
        boolean passwordChangeRequested =
                !isBlank(request.getCurrentPassword()) ||
                !isBlank(request.getNewPassword()) ||
                !isBlank(request.getNewPasswordConfirm());

        if (!passwordChangeRequested) {
            return;
        }

        if (!isLocalUser(user)) {
            throw new IllegalArgumentException("?뚯뀥 濡쒓렇??怨꾩젙? 鍮꾨?踰덊샇瑜?蹂寃쏀븷 ???놁뒿?덈떎.");
        }

        if (isBlank(request.getCurrentPassword())) {
            throw new IllegalArgumentException("?꾩옱 鍮꾨?踰덊샇瑜??낅젰?섏꽭??");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("?꾩옱 鍮꾨?踰덊샇媛 ?쇱튂?섏? ?딆뒿?덈떎.");
        }

        if (isBlank(request.getNewPassword())) {
            throw new IllegalArgumentException("??鍮꾨?踰덊샇瑜??낅젰?섏꽭??");
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("??鍮꾨?踰덊샇媛 ?쇱튂?섏? ?딆뒿?덈떎.");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    private void validateProfile(ProfileUpdateRequest request) {
        if (request == null || isBlank(request.getName())) {
            throw new IllegalArgumentException("?대쫫???낅젰?섏꽭??");
        }
        if (isBlank(request.getPhone())) {
            throw new IllegalArgumentException("?꾪솕踰덊샇瑜??낅젰?섏꽭??");
        }
        if (request.getBirthDate() == null) {
            throw new IllegalArgumentException("?앸뀈?붿씪???낅젰?섏꽭??");
        }
        if (isBlank(request.getGender())) {
            throw new IllegalArgumentException("?깅퀎???좏깮?섏꽭??");
        }
    }

    private boolean isLocalUser(AppUser user) {
        return user.getProvider() == null || "LOCAL".equalsIgnoreCase(user.getProvider());
    }

    private String normalize(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
