package com.example.simuuser.repository;

import com.example.simuuser.entity.DocumentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentCommentRepository extends JpaRepository<DocumentComment, Long> {
    List<DocumentComment> findByDocumentIdOrderByCreatedAtAsc(Long documentId);
}