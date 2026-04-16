package com.example.simuuser.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "document_versions")
@Getter 
@NoArgsConstructor
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [중요] JSON 변환 시 무한 루프 방지 및 지연 로딩
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Lob // TEXT보다 더 큰 용량을 담을 수 있게 설정
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private LocalDateTime createdAt;

    // 편의를 위한 생성자
    public DocumentVersion(Document document, String content) {
        this.document = document;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}