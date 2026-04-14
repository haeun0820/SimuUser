package com.example.simuuser.dto;

import com.example.simuuser.entity.MarketAnalysisResult;

import java.util.Map;

public class MarketAnalysisResultResponse {

    private final Long id;
    private final Long projectId;
    private final String title;
    private final String competitionLevel;
    private final Integer saturation;
    private final Integer competitorCount;
    private final Map<String, Object> result;
    private final String createdAt;

    public MarketAnalysisResultResponse(MarketAnalysisResult savedResult, Map<String, Object> result) {
        this.id = savedResult.getId();
        this.projectId = savedResult.getProject().getId();
        this.title = savedResult.getTitle();
        this.competitionLevel = savedResult.getCompetitionLevel();
        this.saturation = savedResult.getSaturation();
        this.competitorCount = savedResult.getCompetitorCount();
        this.result = result;
        this.createdAt = savedResult.getCreatedAt() == null ? null : savedResult.getCreatedAt().toString();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getCompetitionLevel() {
        return competitionLevel;
    }

    public Integer getSaturation() {
        return saturation;
    }

    public Integer getCompetitorCount() {
        return competitorCount;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
