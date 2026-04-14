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
@Table(name = "market_analysis_results")
public class MarketAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 30)
    private String competitionLevel;

    private Integer saturation;

    private Integer competitorCount;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected MarketAnalysisResult() {
    }

    public MarketAnalysisResult(
            Project project,
            AppUser createdBy,
            String title,
            String competitionLevel,
            Integer saturation,
            Integer competitorCount,
            String resultJson
    ) {
        this.project = project;
        this.createdBy = createdBy;
        this.title = title;
        this.competitionLevel = competitionLevel;
        this.saturation = saturation;
        this.competitorCount = competitorCount;
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

    public String getTitle() {
        return title;
    }

    public String getCompetitionLevel() {
        return competitionLevel;
    }

    public Integer getSaturation() {
        return saturation;
    }

    public Integer getCompetitorCount() {
        return competitorCount;
    }

    public String getResultJson() {
        return resultJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
