package com.example.simuuser.dto;

import com.example.simuuser.entity.AppUser;

public class UserSearchResponse {

    private final Long id;
    private final String name;
    private final String userId;
    private final String email;

    public UserSearchResponse(AppUser user) {
        this.id = user.getId();
        this.name = user.getName();
        this.userId = user.getUserId();
        this.email = user.getEmail();
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
}
