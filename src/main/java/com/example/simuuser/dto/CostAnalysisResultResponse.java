package com.example.simuuser.dto;

import com.example.simuuser.entity.CostAnalysisResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CostAnalysisResultResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Long id;
    private final Long projectId;
    private final String projectTitle;
    private final Map<String, Object> formData;
    private final Map<String, Object> result;
    private final LocalDateTime createdAt;

    public CostAnalysisResultResponse(CostAnalysisResult entity, Map<String, Object> resultData) {
        this.id = entity.getId();
        this.projectId = entity.getProject().getId();
        this.projectTitle = entity.getProject().getTitle();
        this.formData = buildFormData(entity);
        this.result = resultData;
        this.createdAt = entity.getCreatedAt();
    }

    public CostAnalysisResultResponse(CostAnalysisResult entity, String formJson, String resultJson) {
        this.id = entity.getId();
        this.projectId = entity.getProject().getId();
        this.projectTitle = entity.getProject().getTitle();
        this.formData = fromJson(formJson);
        this.result = fromJson(resultJson);
        this.createdAt = entity.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    private Map<String, Object> buildFormData(CostAnalysisResult entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("projectId", entity.getProject().getId());
        data.put("projectTitle", entity.getProject().getTitle());
        data.put("revenueModels", parseStringList(entity.getRevenueModels()));
        data.put("expectedUsers", entity.getExpectedUsers());
        data.put("pricePerUser", entity.getPricePerUser());
        return data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }

            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> parseStringList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }

            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
