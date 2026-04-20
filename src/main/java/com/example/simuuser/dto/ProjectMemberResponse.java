package com.example.simuuser.dto;

import com.example.simuuser.entity.ProjectMember;

public class ProjectMemberResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String profileImage;
    private final String role;
    private final String status;
    private final boolean currentUser;

    public ProjectMemberResponse(ProjectMember member, Long currentUserId) {
        this.id = member.getId();
        this.name = member.getUser().getName();
        this.email = member.getUser().getEmail();
        this.profileImage = member.getUser().getProfileImage();
        this.role = member.getRole();
        this.status = member.getStatus();
        this.currentUser = currentUserId != null && currentUserId.equals(member.getUser().getId());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public boolean isCurrentUser() {
        return currentUser;
    }
}
