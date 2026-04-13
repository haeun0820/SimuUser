package com.example.simuuser.controller;

import com.example.simuuser.dto.ProfileResponse;
import com.example.simuuser.dto.ProfileUpdateRequest;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class MyPageController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public MyPageController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/mypage")
    public String mypage() {
        // src/main/resources/templates/mypage/mypage.html 파일과 연결
        return "mypage/mypage"; 
    }

    @ResponseBody
    @GetMapping("/api/me")
    public ProfileResponse me(Authentication authentication) {
        return new ProfileResponse(getCurrentUser(authentication));
    }

    @ResponseBody
    @PutMapping("/api/me")
    @Transactional
    public ResponseEntity<?> updateMe(@RequestBody ProfileUpdateRequest request, Authentication authentication) {
        try {
            AppUser user = getCurrentUser(authentication);
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
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }

        if (isBlank(request.getCurrentPassword())) {
            throw new IllegalArgumentException("현재 비밀번호를 입력하세요.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (isBlank(request.getNewPassword())) {
            throw new IllegalArgumentException("새 비밀번호를 입력하세요.");
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    private void validateProfile(ProfileUpdateRequest request) {
        if (request == null || isBlank(request.getName())) {
            throw new IllegalArgumentException("이름을 입력하세요.");
        }
        if (isBlank(request.getPhone())) {
            throw new IllegalArgumentException("전화번호를 입력하세요.");
        }
        if (request.getBirthDate() == null) {
            throw new IllegalArgumentException("생년월일을 입력하세요.");
        }
        if (isBlank(request.getGender())) {
            throw new IllegalArgumentException("성별을 선택하세요.");
        }
    }

    private AppUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            String provider = oauth2Authentication.getAuthorizedClientRegistrationId().toUpperCase();
            String providerId = extractOAuthProviderId(provider, oauth2Authentication.getPrincipal().getAttributes());

            return appUserRepository.findByProviderAndProviderId(provider, providerId)
                    .orElseThrow(() -> new IllegalStateException("소셜 로그인 사용자를 찾을 수 없습니다."));
        }

        return appUserRepository.findByUserId(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));
    }

    @SuppressWarnings("unchecked")
    private String extractOAuthProviderId(String provider, Map<String, Object> attributes) {
        Object value;

        if ("NAVER".equals(provider)) {
            Object response = attributes.get("response");
            if (!(response instanceof Map<?, ?> responseMap)) {
                throw new IllegalStateException("네이버 로그인 응답을 확인할 수 없습니다.");
            }
            value = ((Map<String, Object>) responseMap).get("id");
        } else if ("KAKAO".equals(provider)) {
            value = attributes.get("id");
        } else {
            value = attributes.get("sub");
        }

        if (value == null || value.toString().isBlank()) {
            throw new IllegalStateException("소셜 로그인 식별자를 확인할 수 없습니다.");
        }

        return value.toString().trim();
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
