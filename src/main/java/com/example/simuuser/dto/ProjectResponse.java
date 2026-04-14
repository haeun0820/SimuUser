package com.example.simuuser.dto;

import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ProjectMember;

import java.util.List;

public class ProjectResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final String targetUser;
    private final String industry;
    private final String type;
    private final List<String> members;
    private final String createdAt;

    public ProjectResponse(Project project, List<ProjectMember> members, Long currentUserId) {
        this.id = project.getId();
        this.title = project.getTitle();
        this.description = project.getDescription();
        this.targetUser = project.getTargetUser();
        this.industry = project.getIndustry();
        this.type = project.getType();
        this.members = members.stream()
                .filter(member -> "ACCEPTED".equals(member.getStatus()))
                .filter(member -> currentUserId == null || !member.getUser().getId().equals(currentUserId))
                .map(member -> member.getUser().getEmail())
                .filter(email -> email != null && !email.isBlank())
                .toList();
        this.createdAt = project.getCreatedAt() == null ? null : project.getCreatedAt().toString();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public String getIndustry() {
        return industry;
    }

    public String getType() {
        return type;
    }

    public List<String> getMembers() {
        return members;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
