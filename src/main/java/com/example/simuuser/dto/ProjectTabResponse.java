package com.example.simuuser.dto;

import java.time.LocalDateTime;

import com.example.simuuser.entity.ProjectTab;

public class ProjectTabResponse {

    private final Long id;
    private final String name;
    private final int orderIndex;
    private final LocalDateTime createdAt;

    public ProjectTabResponse(ProjectTab tab) {
        this.id = tab.getId();
        this.name = tab.getName();
        this.orderIndex = tab.getOrderIndex();
        this.createdAt = tab.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
