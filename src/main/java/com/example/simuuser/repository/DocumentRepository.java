package com.example.simuuser.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.simuuser.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    // 특정 탭에 속한 모든 문서 찾기
    List<Document> findByProjectTabId(Long tabId);

    List<Document> findByProjectId(Long projectId);
}