package com.example.simuuser.service;

import com.example.simuuser.dto.SignupRequest;
import com.example.simuuser.dto.SocialOnboardingRequest;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean isUserIdAvailable(String userId) {
        return !isBlank(userId) && !appUserRepository.existsByUserId(userId.trim());
    }

    @Transactional
    public void signup(SignupRequest request) {
        validate(request);

        AppUser appUser = new AppUser(
                request.getName().trim(),
                request.getUserId().trim(),
                passwordEncoder.encode(request.getPassword()),
                normalize(request.getEmail()),
                request.getPhone().trim(),
                request.getBirthDate(),
                request.getGender()
        );

        appUserRepository.save(appUser);
    }

    @Transactional(readOnly = true)
    public AppUser getCurrentUser(Authentication authentication) {
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

    @Transactional
    public AppUser completeSocialOnboarding(SocialOnboardingRequest request, Authentication authentication) {
        AppUser user = getCurrentUser(authentication);

        if (user.getProvider() == null || "LOCAL".equalsIgnoreCase(user.getProvider())) {
            throw new IllegalArgumentException("소셜 로그인 사용자만 추가 정보를 입력할 수 있습니다.");
        }

        validateSocialOnboarding(request);
        user.completeSocialProfile(
                request.getNickname().trim(),
                request.getEmail().trim(),
                request.getPhone().trim(),
                request.getBirthDate(),
                request.getGender().trim()
        );

        return user;
    }

    private void validate(SignupRequest request) {
        if (isBlank(request.getName())) {
            throw new IllegalArgumentException("?대쫫???낅젰?섏꽭??");
        }
        if (isBlank(request.getUserId())) {
            throw new IllegalArgumentException("?꾩씠?붾? ?낅젰?섏꽭??");
        }
        if (appUserRepository.existsByUserId(request.getUserId().trim())) {
            throw new IllegalArgumentException("?대? ?ъ슜 以묒씤 ?꾩씠?붿엯?덈떎.");
        }
        if (isBlank(request.getPassword())) {
            throw new IllegalArgumentException("鍮꾨?踰덊샇瑜??낅젰?섏꽭??");
        }
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("鍮꾨?踰덊샇媛 ?쇱튂?섏? ?딆뒿?덈떎.");
        }
        if (isBlank(request.getEmail())) {
            throw new IllegalArgumentException("?대찓?쇱쓣 ?낅젰?섏꽭??");
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
        if (!request.isPrivacyAgree() || !request.isTermsAgree()) {
            throw new IllegalArgumentException("?꾩닔 ?쎄????숈쓽?섏꽭??");
        }
    }

    private void validateSocialOnboarding(SocialOnboardingRequest request) {
        if (request == null || isBlank(request.getEmail())) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        if (isBlank(request.getNickname())) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }
        if (isBlank(request.getPhone())) {
            throw new IllegalArgumentException("전화번호를 입력해주세요.");
        }
        if (!request.getPhone().trim().matches("^\\d{3}-\\d{3,4}-\\d{4}$")) {
            throw new IllegalArgumentException("전화번호 형식이 올바르지 않습니다.");
        }
        if (request.getBirthDate() == null || request.getBirthDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("생년월일을 올바르게 입력해주세요.");
        }
        if (isBlank(request.getGender()) || (!"male".equals(request.getGender()) && !"female".equals(request.getGender()))) {
            throw new IllegalArgumentException("성별을 선택해주세요.");
        }
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
            throw new IllegalStateException("소셜 로그인 계정을 확인할 수 없습니다.");
        }

        return value.toString().trim();
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
