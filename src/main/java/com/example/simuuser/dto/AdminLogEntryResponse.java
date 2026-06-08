package com.example.simuuser.dto;

public class AdminLogEntryResponse {

    private final String type;
    private final String typeLabel;
    private final String message;
    private final String createdAt;

    public AdminLogEntryResponse(String type, String typeLabel, String message, String createdAt) {
        this.type = type;
        this.typeLabel = typeLabel;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
