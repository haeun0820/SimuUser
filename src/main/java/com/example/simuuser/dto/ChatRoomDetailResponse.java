package com.example.simuuser.dto;

public class ChatRoomDetailResponse {

    private final Long id;
    private final String type;
    private final String name;
    private final boolean active;

    public ChatRoomDetailResponse(Long id, String type, String name, boolean active) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.active = active;
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

    public boolean isActive() {
        return active;
    }
}
