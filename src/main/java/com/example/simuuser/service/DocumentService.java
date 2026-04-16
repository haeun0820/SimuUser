package com.example.simuuser.service;

import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.entity.Document;
import com.example.simuuser.entity.ProjectTab;
import com.example.simuuser.repository.DocumentRepository;
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

    public DocumentService(DocumentRepository documentRepository, ProjectTabRepository projectTabRepository) {
        this.documentRepository = documentRepository;
        this.projectTabRepository = projectTabRepository;
    }

    // --- [추가된 부분] 문서 단건 조회 ---
    @Transactional(readOnly = true)
    public DocumentResponse findById(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문서가 존재하지 않습니다. id=" + id));
        return new DocumentResponse(doc);
    }

    // 1. 특정 탭의 문서 목록 조회
    @Transactional(readOnly = true)
    public List<DocumentResponse> findByTabId(Long tabId) {
        return documentRepository.findByProjectTabId(tabId).stream()
                .map(DocumentResponse::new)
                .collect(Collectors.toList());
    }

    // 2. 새 문서 생성
    public Document createNewDocument(Long tabId, String title, String description) {
        ProjectTab tab = projectTabRepository.findById(tabId)
                .orElseThrow(() -> new IllegalArgumentException("탭이 존재하지 않습니다."));
        
        Document doc = new Document(tab, title, description, ""); 
        return documentRepository.save(doc);
    }

    // 3. 기존 문서 내용 업데이트 (에디터 저장용)
    public void saveContent(Long id, String title, String content) {
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
}