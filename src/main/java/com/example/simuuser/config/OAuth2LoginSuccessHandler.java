package com.example.simuuser.config;

import com.example.simuuser.entity.AppUser;
import com.example.simuuser.repository.AppUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AppUserRepository appUserRepository;

    public OAuth2LoginSuccessHandler(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            String provider = oauth2Authentication.getAuthorizedClientRegistrationId().toUpperCase();
            String providerId = extractOAuthProviderId(provider, oauth2Authentication.getPrincipal().getAttributes());

            AppUser user = appUserRepository.findByProviderAndProviderId(provider, providerId)
                    .orElseThrow(() -> new IllegalStateException("소셜 로그인 사용자를 찾을 수 없습니다."));

            if (!user.isProfileCompleted()) {
                String targetUrl = UriComponentsBuilder.fromPath("/social-onboarding")
                        .queryParam("socialSignupRequired", "true")
                        .queryParam("provider", provider.toLowerCase())
                        .build()
                        .toUriString();
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
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
}
