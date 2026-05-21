package com.example.simuuser.dto;

import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Inquiry;

public class InquiryResponse {

    private final Long id;
    private final String category;
    private final String title;
    private final String content;
    private final String status;
    private final String answer;
    private final String createdAt;
    private final String answeredAt;
    private final String authorName;
    private final String authorEmail;

    public InquiryResponse(Inquiry inquiry) {
        AppUser user = inquiry.getUser();
        this.id = inquiry.getId();
        this.category = inquiry.getCategory();
        this.title = inquiry.getTitle();
        this.content = inquiry.getContent();
        this.status = inquiry.getStatus();
        this.answer = inquiry.getAnswer();
        this.createdAt = inquiry.getCreatedAt() == null ? null : inquiry.getCreatedAt().toString();
        this.answeredAt = inquiry.getAnsweredAt() == null ? null : inquiry.getAnsweredAt().toString();
        this.authorName = user == null ? "" : user.getName();
        this.authorEmail = user == null ? "" : user.getEmail();
    }

    public Long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getStatus() {
        return status;
    }

    public String getAnswer() {
        return answer;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getAnsweredAt() {
        return answeredAt;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }
}
