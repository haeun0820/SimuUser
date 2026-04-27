package com.example.simuuser.dto;

import com.example.simuuser.entity.FeedbackAnalysisResult;

import java.time.LocalDateTime;
import java.util.Map;

public class FeedbackAnalysisResultResponse {

    private final Long id;
    private final Long projectId;
    private final String sourceType;
    private final String sourceContent;
    private final LocalDateTime createdAt;
    private final Map<String, Object> result;

    public FeedbackAnalysisResultResponse(FeedbackAnalysisResult resultEntity, Map<String, Object> result) {
        this.id = resultEntity.getId();
        this.projectId = resultEntity.getProject().getId();
        this.sourceType = resultEntity.getSourceType();
        this.sourceContent = resultEntity.getSourceContent();
        this.createdAt = resultEntity.getCreatedAt();
        this.result = result;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceContent() {
        return sourceContent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Map<String, Object> getResult() {
        return result;
    }
}
