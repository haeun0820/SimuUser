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
@Table(name = "ai_simulation_results")
public class AiSimulationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @Column(nullable = false)
    private Integer personaCount;

    @Column(length = 30)
    private String gender;

    @Column(length = 100)
    private String ages;

    @Column(length = 100)
    private String job;

    private Integer avgPurchaseIntent;

    @Lob
    private String overallReaction;

    @Lob
    @Column(nullable = false)
    private String resultJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AiSimulationResult() {
    }

    public AiSimulationResult(
            Project project,
            AppUser createdBy,
            Integer personaCount,
            String gender,
            String ages,
            String job,
            Integer avgPurchaseIntent,
            String overallReaction,
            String resultJson
    ) {
        this.project = project;
        this.createdBy = createdBy;
        this.personaCount = personaCount;
        this.gender = gender;
        this.ages = ages;
        this.job = job;
        this.avgPurchaseIntent = avgPurchaseIntent;
        this.overallReaction = overallReaction;
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

    public Integer getPersonaCount() {
        return personaCount;
    }

    public String getGender() {
        return gender;
    }

    public String getAges() {
        return ages;
    }

    public String getJob() {
        return job;
    }

    public Integer getAvgPurchaseIntent() {
        return avgPurchaseIntent;
    }

    public String getOverallReaction() {
        return overallReaction;
    }

    public String getResultJson() {
        return resultJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
