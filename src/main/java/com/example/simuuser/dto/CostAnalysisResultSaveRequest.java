package com.example.simuuser.dto;

import java.util.List;
import java.util.Map;

public class CostAnalysisResultSaveRequest {

    private Long projectId;
    private Long promptId;
    private List<String> revenueModels;
    private Integer expectedUsers;
    private Integer pricePerUser;
    private Map<String, Object> result;

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

    public List<String> getRevenueModels() {
        return revenueModels;
    }

    public void setRevenueModels(List<String> revenueModels) {
        this.revenueModels = revenueModels;
    }

    public Integer getExpectedUsers() {
        return expectedUsers;
    }

    public void setExpectedUsers(Integer expectedUsers) {
        this.expectedUsers = expectedUsers;
    }

    public Integer getPricePerUser() {
        return pricePerUser;
    }

    public void setPricePerUser(Integer pricePerUser) {
        this.pricePerUser = pricePerUser;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }
}
