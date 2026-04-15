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
@Table(name = "cost_analysis_results")
public class CostAnalysisResult {

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

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String revenueModels;

    @Column(nullable = false)
    private Integer expectedUsers;

    @Column(nullable = false)
    private Integer pricePerUser;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String formJson;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected CostAnalysisResult() {
    }

    public CostAnalysisResult(
            Project project,
            AppUser createdBy,
            String title,
            String revenueModels,
            Integer expectedUsers,
            Integer pricePerUser,
            String formJson,
            String resultJson
    ) {
        this.project = project;
        this.createdBy = createdBy;
        this.title = title;
        this.revenueModels = revenueModels;
        this.expectedUsers = expectedUsers;
        this.pricePerUser = pricePerUser;
        this.formJson = formJson;
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

    public String getRevenueModels() {
        return revenueModels;
    }

    public Integer getExpectedUsers() {
        return expectedUsers;
    }

    public Integer getPricePerUser() {
        return pricePerUser;
    }

    public String getFormJson() {
        return formJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
