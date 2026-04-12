package com.example.simuuser.service;

import com.example.simuuser.entity.AppUser;
import com.example.simuuser.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AppUserRepository appUserRepository;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public CustomOAuth2UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        if ("GOOGLE".equals(provider) || "NAVER".equals(provider) || "KAKAO".equals(provider)) {
            saveOAuth2UserIfNew(provider, oauth2User.getAttributes());
        }

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                oauth2User.getAttributes(),
                userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
        );
    }

    private void saveOAuth2UserIfNew(String provider, Map<String, Object> attributes) {
        String providerId = extractProviderId(provider, attributes);

        appUserRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> appUserRepository.save(createOAuth2User(provider, providerId, attributes)));
    }

    private String extractProviderId(String provider, Map<String, Object> attributes) {
        Object value;

        if ("NAVER".equals(provider)) {
            value = naverResponse(attributes).get("id");
        } else if ("KAKAO".equals(provider)) {
            value = attributes.get("id");
        } else {
            value = attributes.get("sub");
        }

        if (value == null || value.toString().trim().isEmpty()) {
            throw new OAuth2AuthenticationException(provider + " provider id is missing.");
        }

        return value.toString().trim();
    }

    private AppUser createOAuth2User(String provider, String providerId, Map<String, Object> attributes) {
        String name = extractName(provider, attributes);
        String email = extractEmail(provider, attributes);
        String userId = provider.toLowerCase() + "_" + providerId;

        return new AppUser(
                name,
                userId,
                "{oauth2}",
                email,
                "SOCIAL",
                LocalDate.of(1900, 1, 1),
                "UNKNOWN",
                provider,
                providerId
        );
    }

    private String extractName(String provider, Map<String, Object> attributes) {
        if ("NAVER".equals(provider)) {
            String name = valueOrNull(naverResponse(attributes).get("name"));
            return name == null ? "NAVER User" : name;
        }

        if ("KAKAO".equals(provider)) {
            String nickname = valueOrNull(kakaoProperties(attributes).get("nickname"));
            return nickname == null ? "KAKAO User" : nickname;
        }

        String name = valueOrNull(attributes.get("name"));
        if (name != null) {
            return name;
        }

        return provider + " User";
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        if ("NAVER".equals(provider)) {
            return valueOrNull(naverResponse(attributes).get("email"));
        }

        if ("KAKAO".equals(provider)) {
            return valueOrNull(kakaoAccount(attributes).get("email"));
        }

        return valueOrNull(attributes.get("email"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> naverResponse(Map<String, Object> attributes) {
        Object response = attributes.get("response");
        if (response instanceof Map<?, ?>) {
            return (Map<String, Object>) response;
        }

        throw new OAuth2AuthenticationException("NAVER response is missing.");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> kakaoProperties(Map<String, Object> attributes) {
        Object properties = attributes.get("properties");
        if (properties instanceof Map<?, ?>) {
            return (Map<String, Object>) properties;
        }

        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> kakaoAccount(Map<String, Object> attributes) {
        Object account = attributes.get("kakao_account");
        if (account instanceof Map<?, ?>) {
            return (Map<String, Object>) account;
        }

        return Map.of();
    }

    private String valueOrNull(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
