package com.example.simuuser.dto;

import java.time.LocalDateTime;

public class ChatRoomSummaryResponse {

    private final Long id;
    private final String type;
    private final String name;
    private final String profileImage;
    private final String lastMessage;
    private final LocalDateTime lastMessageAt;

    public ChatRoomSummaryResponse(Long id, String type, String name, String profileImage, String lastMessage, LocalDateTime lastMessageAt) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.profileImage = profileImage;
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }
}
