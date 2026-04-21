package com.example.simuuser.dto;

import com.example.simuuser.entity.AppUser;

import java.time.LocalDate;

public class ProfileResponse {

    private final String name;
    private final String userId;
    private final String loginLabel;
    private final String email;
    private final String phone;
    private final LocalDate birthDate;
    private final String gender;
    private final String provider;
    private final String profileImage;
    private final boolean localLogin;
    private final boolean profileImageEditable;

    public ProfileResponse(AppUser user) {
        this.name = user.getName();
        this.userId = user.getUserId();
        this.provider = user.getProvider();
        this.loginLabel = loginLabel(user.getProvider(), user.getUserId());
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.birthDate = user.getBirthDate();
        this.gender = user.getGender();
        this.profileImage = user.getProfileImage();
        this.localLogin = user.getProvider() == null || "LOCAL".equalsIgnoreCase(user.getProvider());
        this.profileImageEditable = !"GOOGLE".equalsIgnoreCase(user.getProvider());
    }

    private String loginLabel(String provider, String userId) {
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            return "구글 로그인";
        }
        if ("NAVER".equalsIgnoreCase(provider)) {
            return "네이버 로그인";
        }
        if ("KAKAO".equalsIgnoreCase(provider)) {
            return "카카오 로그인";
        }
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getLoginLabel() {
        return loginLabel;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getGender() {
        return gender;
    }

    public String getProvider() {
        return provider;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public boolean isLocalLogin() {
        return localLogin;
    }

    public boolean isProfileImageEditable() {
        return profileImageEditable;
    }
}
