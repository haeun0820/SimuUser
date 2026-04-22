package com.example.simuuser.dto;

import com.example.simuuser.entity.ChatMessage;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private final Long id;
    private final Long senderId;
    private final String senderName;
    private final String senderProfileImage;
    private final String content;
    private final LocalDateTime createdAt;
    private final boolean mine;

    public ChatMessageResponse(ChatMessage message, Long currentUserId) {
        this.id = message.getId();
        this.senderId = message.getSender().getId();
        this.senderName = message.getSender().getName();
        this.senderProfileImage = message.getSender().getProfileImage();
        this.content = message.getContent();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isMine() {
        return mine;
    }
}
