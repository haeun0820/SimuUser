package com.example.simuuser.dto;

import com.example.simuuser.entity.ProjectMember;

public class ProjectInviteResponse {

    private final Long id;
    private final Long projectId;
    private final String projectTitle;
    private final String inviterName;
    private final String inviterEmail;
    private final String role;
    private final String status;
    private final String createdAt;

    public ProjectInviteResponse(ProjectMember member) {
        this.id = member.getId();
        this.projectId = member.getProject().getId();
        this.projectTitle = member.getProject().getTitle();
        this.inviterName = member.getProject().getOwner().getName();
        this.inviterEmail = member.getProject().getOwner().getEmail();
        this.role = member.getRole();
        this.status = member.getStatus();
        this.createdAt = member.getCreatedAt() == null ? null : member.getCreatedAt().toString();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public String getInviterName() {
        return inviterName;
    }

    public String getInviterEmail() {
        return inviterEmail;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
