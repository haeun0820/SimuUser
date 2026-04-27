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
@Table(name = "feedback_analysis_results")
public class FeedbackAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @Column(nullable = false, length = 20)
    private String sourceType;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String sourceContent;

    @Column(nullable = false)
    private Integer totalScore;

    @Column(nullable = false)
    private Integer logicScore;

    @Column(nullable = false)
    private Integer completionScore;

    @Column(nullable = false)
    private Integer feasibilityScore;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected FeedbackAnalysisResult() {
    }

    public FeedbackAnalysisResult(
            Project project,
            AppUser createdBy,
            String sourceType,
            String sourceContent,
            Integer totalScore,
            Integer logicScore,
            Integer completionScore,
            Integer feasibilityScore,
            String resultJson
    ) {
        this.project = project;
        this.createdBy = createdBy;
        this.sourceType = sourceType;
        this.sourceContent = sourceContent;
        this.totalScore = totalScore;
        this.logicScore = logicScore;
        this.completionScore = completionScore;
        this.feasibilityScore = feasibilityScore;
        this.resultJson = resultJson;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceContent() {
        return sourceContent;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public Integer getLogicScore() {
        return logicScore;
    }

    public Integer getCompletionScore() {
        return completionScore;
    }

    public Integer getFeasibilityScore() {
        return feasibilityScore;
    }

    public String getResultJson() {
        return resultJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
