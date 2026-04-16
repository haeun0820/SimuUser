package com.example.simuuser.service;

import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.entity.Document;
import com.example.simuuser.entity.DocumentComment;
import com.example.simuuser.entity.DocumentVersion;
import com.example.simuuser.entity.ProjectTab;
import com.example.simuuser.repository.DocumentCommentRepository;
import com.example.simuuser.repository.DocumentRepository;
import com.example.simuuser.repository.DocumentVersionRepository;
import com.example.simuuser.repository.ProjectTabRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectTabRepository projectTabRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentCommentRepository documentCommentRepository;

    public DocumentService(DocumentRepository documentRepository, 
                           ProjectTabRepository projectTabRepository,
                           DocumentVersionRepository documentVersionRepository,
                           DocumentCommentRepository documentCommentRepository) {
        this.documentRepository = documentRepository;
        this.projectTabRepository = projectTabRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.documentCommentRepository = documentCommentRepository;
    }

    @Transactional(readOnly = true)
    public DocumentResponse findById(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문서가 존재하지 않습니다. id=" + id));
        return new DocumentResponse(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> findByTabId(Long tabId) {
        return documentRepository.findByProjectTabId(tabId).stream()
                .map(DocumentResponse::new)
                .collect(Collectors.toList());
    }

    public Document createNewDocument(Long tabId, String title, String description) {
        ProjectTab tab = projectTabRepository.findById(tabId)
                .orElseThrow(() -> new IllegalArgumentException("탭이 존재하지 않습니다."));
        
        Document doc = new Document(tab, title, description, ""); 
        return documentRepository.save(doc);
    }

    // [1] 일반 저장: 내용 업데이트 + 버전 기록 생성
    public void saveContent(Long id, String title, String content) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문서가 존재하지 않습니다. id=" + id));
        
        doc.updateContent(title, content); 
        
        DocumentVersion version = new DocumentVersion(doc, content);
        documentVersionRepository.save(version);
    }

    // [2] 복구용 저장: 내용만 업데이트 (버전 기록 생성 X)
    public void updatePureContent(Long id, String title, String content) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문서가 존재하지 않습니다. id=" + id));
        
        doc.updateContent(title, content);
    }

    public void deleteDocument(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 문서입니다.");
        }
        documentRepository.deleteById(id);
    }

    // --- 댓글 관련 로직 ---
    public void addComment(Long docId, String author, String content) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("문서가 존재하지 않습니다."));
                
        DocumentComment comment = new DocumentComment(doc, author, content);
        documentCommentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<DocumentComment> findCommentsByDocumentId(Long docId) {
        return documentCommentRepository.findByDocumentIdOrderByCreatedAtAsc(docId);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersion> findAllVersionsByDocumentId(Long docId) {
        return documentVersionRepository.findByDocumentIdOrderByCreatedAtDesc(docId);
    }
}