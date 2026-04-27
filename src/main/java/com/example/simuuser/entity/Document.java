package com.example.simuuser.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "documents")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tab_id")
    private ProjectTab projectTab;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project; // 이 필드 이름이 builder의 .project()가 됩니다.

    private String type;

    @Column(nullable = false)
    private String title;

    private String description; // 문서에 대한 짧은 설명 (필요 시)

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    // Document.java 내부 추가/수정
    @Builder.Default
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentVersion> versions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentComment> comments = new ArrayList<>(); // DocumentComment로 변경

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // [중요] 새 문서를 만들 때 사용할 생성자
    public Document(ProjectTab projectTab, String title, String description, String content) {
        this.projectTab = projectTab;
        this.title = title;
        this.description = description;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 문서 내용 업데이트 메서드 (기존 유지)
    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}