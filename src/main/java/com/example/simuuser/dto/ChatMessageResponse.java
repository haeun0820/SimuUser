package com.example.simuuser.dto;

import com.example.simuuser.entity.ChatMessage;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private final Long id;
    private final Long senderId;
    private final String senderName;
    private final String senderProfileImage;
    private final String content;
    private final String messageType;
    private final String attachmentUrl;
    private final String attachmentName;
    private final String attachmentContentType;
    private final LocalDateTime createdAt;
    private final boolean mine;

    public ChatMessageResponse(ChatMessage message, Long currentUserId) {
        this.id = message.getId();
        this.senderId = message.getSender().getId();
        this.senderName = message.getSender().getName();
        this.senderProfileImage = message.getSender().getProfileImage();
        this.content = message.getContent();
        this.messageType = message.getMessageType();
        this.attachmentUrl = message.getAttachmentUrl();
        this.attachmentName = message.getAttachmentName();
        this.attachmentContentType = message.getAttachmentContentType();
        this.createdAt = message.getCreatedAt();
        this.mine = currentUserId != null && currentUserId.equals(message.getSender().getId());
    }

    public Long getId() {
        return id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderProfileImage() {
        return senderProfileImage;
    }

    public String getContent() {
        return content;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isMine() {
        return mine;
    }
}
