package com.example.simuuser.repository;

import com.example.simuuser.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
