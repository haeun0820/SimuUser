package com.example.simuuser.dto;

import java.time.LocalDateTime;

import com.example.simuuser.entity.Document;

public class DocumentResponse {
    private Long id;
    private String title;
    private String description;
    private String content;
    private LocalDateTime updatedAt;

    public DocumentResponse(Document doc) {
        this.id = doc.getId();
        this.title = doc.getTitle();
        this.description = doc.getDescription();
        this.content = doc.getContent();
        this.updatedAt = doc.getUpdatedAt();
    }

    // Getter들 (반드시 있어야 JSON으로 변환됩니다)
    public String getDescription() { return description; }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}