package com.example.simuuser.dto;

import com.example.simuuser.entity.AiSimulationResult;

import java.util.Map;

public class AiSimulationResultResponse {

    private final Long id;
    private final Long projectId;
    private final Integer personaCount;
    private final String gender;
    private final String ages;
    private final String job;
    private final Integer avgPurchaseIntent;
    private final String overallReaction;
    private final Map<String, Object> result;
    private final String createdAt;

    public AiSimulationResultResponse(AiSimulationResult savedResult, Map<String, Object> result) {
        this.id = savedResult.getId();
        this.projectId = savedResult.getProject().getId();
        this.personaCount = savedResult.getPersonaCount();
        this.gender = savedResult.getGender();
        this.ages = savedResult.getAges();
        this.job = savedResult.getJob();
        this.avgPurchaseIntent = savedResult.getAvgPurchaseIntent();
        this.overallReaction = savedResult.getOverallReaction();
        this.result = result;
        this.createdAt = savedResult.getCreatedAt() == null ? null : savedResult.getCreatedAt().toString();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Integer getPersonaCount() {
        return personaCount;
    }

    public String getGender() {
        return gender;
    }

    public String getAges() {
        return ages;
    }

    public String getJob() {
        return job;
    }

    public Integer getAvgPurchaseIntent() {
        return avgPurchaseIntent;
    }

    public String getOverallReaction() {
        return overallReaction;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
