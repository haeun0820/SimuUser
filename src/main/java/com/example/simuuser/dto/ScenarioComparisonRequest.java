package com.example.simuuser.dto;

import java.util.List;

public class ScenarioComparisonRequest {

    private Long projectId;
    private Long promptId;
    private String compareTitle;
    private List<ScenarioComparisonInput> scenarios;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getPromptId() {
        return promptId;
    }

    public void setPromptId(Long promptId) {
        this.promptId = promptId;
    }

    public String getCompareTitle() {
        return compareTitle;
    }

    public void setCompareTitle(String compareTitle) {
        this.compareTitle = compareTitle;
    }

    public List<ScenarioComparisonInput> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<ScenarioComparisonInput> scenarios) {
        this.scenarios = scenarios;
    }
}
