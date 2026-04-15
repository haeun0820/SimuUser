package com.example.simuuser.repository;

import com.example.simuuser.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    // 특정 탭에 속한 모든 문서 찾기
    List<Document> findByProjectTabId(Long tabId);
}