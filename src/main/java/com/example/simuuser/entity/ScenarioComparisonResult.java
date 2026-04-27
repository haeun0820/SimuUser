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
@Table(name = "scenario_comparison_results")
public class ScenarioComparisonResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @Column(nullable = false, length = 200)
    private String compareTitle;

    @Column(nullable = false, length = 200)
    private String recommendedScenarioTitle;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ScenarioComparisonResult() {
    }

    public ScenarioComparisonResult(
            Project project,
            AppUser createdBy,
            String compareTitle,
            String recommendedScenarioTitle,
            String resultJson
    ) {
        this.project = project;
        this.createdBy = createdBy;
        this.compareTitle = compareTitle;
        this.recommendedScenarioTitle = recommendedScenarioTitle;
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

    public String getCompareTitle() {
        return compareTitle;
    }

    public String getRecommendedScenarioTitle() {
        return recommendedScenarioTitle;
    }

    public String getResultJson() {
        return resultJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
