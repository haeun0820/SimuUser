package com.example.simuuser.dto;

import java.util.Map;

public class ScenarioComparisonResultSaveRequest {

    private Long projectId;
    private String compareTitle;
    private Map<String, Object> result;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getCompareTitle() {
        return compareTitle;
    }

    public void setCompareTitle(String compareTitle) {
        this.compareTitle = compareTitle;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }
}
