package com.example.simuuser.dto;

import com.example.simuuser.entity.AiPrompt;

public class AiPromptResponse {
    private final Long id;
    private final String name;
    private final String category;
    private final String model;
    private final String systemPrompt;
    private final String userPromptTemplate;
    private final String createdAt;

    public AiPromptResponse(AiPrompt prompt) {
        this.id = prompt.getId();
        this.name = prompt.getName();
        this.category = prompt.getCategory();
        this.model = prompt.getModel();
        this.systemPrompt = prompt.getSystemPrompt();
        this.userPromptTemplate = prompt.getUserPromptTemplate();
        this.createdAt = prompt.getCreatedAt() == null ? null : prompt.getCreatedAt().toString();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getModel() { return model; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getUserPromptTemplate() { return userPromptTemplate; }
    public String getCreatedAt() { return createdAt; }
}
