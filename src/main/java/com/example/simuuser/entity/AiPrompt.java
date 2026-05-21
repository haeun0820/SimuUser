package com.example.simuuser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_prompts")
public class AiPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, length = 80)
    private String model;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String systemPrompt;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String userPromptTemplate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AiPrompt() {
    }

    public AiPrompt(String name, String category, String model, String systemPrompt, String userPromptTemplate) {
        this.name = name;
        this.category = category;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.userPromptTemplate = userPromptTemplate;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getModel() { return model; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getUserPromptTemplate() { return userPromptTemplate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void update(String name, String category, String model, String systemPrompt, String userPromptTemplate) {
        this.name = name;
        this.category = category;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.userPromptTemplate = userPromptTemplate;
    }
}
