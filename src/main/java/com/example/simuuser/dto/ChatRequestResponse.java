package com.example.simuuser.dto;

import java.time.LocalDateTime;

public class ChatRequestResponse {

    private final Long roomId;
    private final String requesterName;
    private final String requesterEmail;
    private final String requesterProfileImage;
    private final String message;
    private final LocalDateTime requestedAt;

    public ChatRequestResponse(
            Long roomId,
            String requesterName,
            String requesterEmail,
            String requesterProfileImage,
            String message,
            LocalDateTime requestedAt
    ) {
        this.roomId = roomId;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.requesterProfileImage = requesterProfileImage;
        this.message = message;
        this.requestedAt = requestedAt;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public String getRequesterProfileImage() {
        return requesterProfileImage;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
}
