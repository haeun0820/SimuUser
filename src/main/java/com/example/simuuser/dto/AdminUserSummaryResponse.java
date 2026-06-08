package com.example.simuuser.dto;

import com.example.simuuser.entity.AppUser;

public class AdminUserSummaryResponse {

    private final Long id;
    private final String name;
    private final String userId;
    private final String email;
    private final String loginMethod;
    private final String createdAt;
    private final String role;

    public AdminUserSummaryResponse(AppUser user) {
        this.id = user.getId();
        this.name = user.getName();
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.loginMethod = normalizeLoginMethod(user.getProvider());
        this.createdAt = user.getCreatedAt() == null ? null : user.getCreatedAt().toString();
        this.role = user.getRole();
    }

    private String normalizeLoginMethod(String provider) {
        if (provider == null || provider.isBlank()) {
            return "site";
        }

        String normalized = provider.trim().toLowerCase();
        return "local".equals(normalized) ? "site" : normalized;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getLoginMethod() {
        return loginMethod;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getRole() {
        return role;
    }
}
