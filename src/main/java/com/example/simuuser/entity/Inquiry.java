package com.example.simuuser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
public class Inquiry {

    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_ANSWERED = "answered";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 120)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false, length = 20)
    private String status;

    @Lob
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by_id")
    private AppUser answeredBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime answeredAt;

    protected Inquiry() {
    }

    public Inquiry(AppUser user, String category, String title, String content) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.content = content;
        this.status = STATUS_WAITING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
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

    public AppUser getAnsweredBy() {
        return answeredBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void answer(String answer, AppUser answeredBy) {
        this.answer = answer;
        this.answeredBy = answeredBy;
        this.status = STATUS_ANSWERED;
        this.answeredAt = LocalDateTime.now();
    }

    public boolean isAnswered() {
        return STATUS_ANSWERED.equals(this.status);
    }

    public void update(String category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }
}
