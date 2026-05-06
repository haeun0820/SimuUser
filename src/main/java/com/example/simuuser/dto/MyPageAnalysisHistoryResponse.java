package com.example.simuuser.dto;

import java.time.LocalDateTime;

public class MyPageAnalysisHistoryResponse {

    private final Long id;
    private final String type;
    private final String title;
    private final Long projectId;
    private final String projectTitle;
    private final LocalDateTime createdAt;
    private final boolean starred;
    private final String detailUrl;

    public MyPageAnalysisHistoryResponse(
            Long id,
            String type,
            String title,
            Long projectId,
            String projectTitle,
            LocalDateTime createdAt,
            boolean starred,
            String detailUrl
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.createdAt = createdAt;
        this.starred = starred;
        this.detailUrl = detailUrl;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public Long getProjectId() { return projectId; }
    public String getProjectTitle() { return projectTitle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isStarred() { return starred; }
    public String getDetailUrl() { return detailUrl; }
}
