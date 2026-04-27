package com.example.simuuser.dto;

import com.example.simuuser.entity.ScenarioComparisonResult;

import java.time.LocalDateTime;
import java.util.Map;

public class ScenarioComparisonResultResponse {

    private final Long id;
    private final Long projectId;
    private final String compareTitle;
    private final String recommendedScenarioTitle;
    private final LocalDateTime createdAt;
    private final Map<String, Object> result;

    public ScenarioComparisonResultResponse(ScenarioComparisonResult entity, Map<String, Object> result) {
        this.id = entity.getId();
        this.projectId = entity.getProject().getId();
        this.compareTitle = entity.getCompareTitle();
        this.recommendedScenarioTitle = entity.getRecommendedScenarioTitle();
        this.createdAt = entity.getCreatedAt();
        this.result = result;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getCompareTitle() {
        return compareTitle;
    }

    public String getRecommendedScenarioTitle() {
        return recommendedScenarioTitle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Map<String, Object> getResult() {
        return result;
    }
}
