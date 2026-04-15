package com.example.simuuser.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // --- [추가] 탭 삭제 시 해당 탭에 속한 문서들도 자동으로 삭제되도록 설정 ---
    // 만약 문서 엔티티 이름이 Document라면 아래와 같이 작성합니다.
    // orphanRemoval = true는 탭에서 문서가 제거되면 DB에서도 삭제하라는 뜻입니다.

    // @OneToMany(mappedBy = "projectTab", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Document> documents = new ArrayList<>();

    protected ProjectTab() {
    }

    public ProjectTab(Project project, String name, int orderIndex) {
        this.project = project;
        this.name = name;
        this.orderIndex = orderIndex;
        this.createdAt = LocalDateTime.now();
    }

    // --- [추가] 이름을 수정하기 위한 비즈니스 메서드 ---
    public void updateName(String newName) {
        this.name = newName;
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

    // 문서 리스트 Getter (필요 시)
    // public List<Document> getDocuments() {
    //     return documents;
    // }
}