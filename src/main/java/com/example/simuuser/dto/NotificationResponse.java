package com.example.simuuser.dto;

import com.example.simuuser.entity.Notification;
import com.example.simuuser.entity.ProjectMember;

public class NotificationResponse {

    private final Long id;
    private final String type;
    private final Long referenceId;
    private final String title;
    private final String message;
    private final String createdAt;
    private final boolean read;
    private final ProjectInviteResponse invite;

    public NotificationResponse(Notification notification, ProjectMember inviteMember) {
        this.id = notification.getId();
        this.type = notification.getType();
        this.referenceId = notification.getReferenceId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.createdAt = notification.getCreatedAt() == null ? null : notification.getCreatedAt().toString();
        this.read = notification.getReadAt() != null;
        this.invite = inviteMember == null ? null : new ProjectInviteResponse(inviteMember);
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public ProjectInviteResponse getInvite() {
        return invite;
    }
}
