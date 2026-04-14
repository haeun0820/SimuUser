package com.example.simuuser.dto;

import java.util.List;
import java.util.Map;

public class AiSimulationResultSaveRequest {

    private Long projectId;
    private Integer personaCount;
    private String gender;
    private String ages;
    private String job;
    private Map<String, Object> result;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Integer getPersonaCount() {
        return personaCount;
    }

    public void setPersonaCount(Integer personaCount) {
        this.personaCount = personaCount;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAges() {
        return ages;
    }

    public void setAges(Object ages) {
        if (ages instanceof List<?> list) {
            this.ages = String.join(", ", list.stream().map(String::valueOf).toList());
            return;
        }

        this.ages = ages == null ? null : ages.toString();
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }
}
