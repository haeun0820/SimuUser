package com.example.simuuser.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "documents")
@Getter 
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tab_id", nullable = false)
    private ProjectTab projectTab;

    @Column(nullable = false)
    private String title;

    private String description; // 문서에 대한 짧은 설명 (필요 시)

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

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