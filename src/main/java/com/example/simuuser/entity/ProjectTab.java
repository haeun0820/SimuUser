package com.example.simuuser.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_tabs")
public class ProjectTab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ProjectTab() {
    }

    public ProjectTab(Project project, String name, int orderIndex) {
        this.project = project;
        this.name = name;
        this.orderIndex = orderIndex;
        this.createdAt = LocalDateTime.now();
    }

    // --- Getter 영역 ---

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}