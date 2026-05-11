package com.example.simuuser.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Document;
import com.example.simuuser.entity.DocumentComment;
import com.example.simuuser.entity.DocumentVersion;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ProjectTab;
import com.example.simuuser.repository.DocumentCommentRepository;
import com.example.simuuser.repository.DocumentRepository;
import com.example.simuuser.repository.DocumentVersionRepository;
import com.example.simuuser.repository.ProjectMemberRepository;
import com.example.simuuser.repository.ProjectTabRepository;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectTabRepository projectTabRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentCommentRepository documentCommentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;

    public DocumentService(
            DocumentRepository documentRepository,
            ProjectTabRepository projectTabRepository,
            DocumentVersionRepository documentVersionRepository,
            DocumentCommentRepository documentCommentRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectService projectService
    ) {
        this.documentRepository = documentRepository;
        this.projectTabRepository = projectTabRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.documentCommentRepository = documentCommentRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
    }

    @Transactional(readOnly = true)
    public DocumentResponse findById(Long id, Authentication authentication) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("臾몄꽌媛 議댁옱?섏? ?딆뒿?덈떎. id=" + id));
        ensureAccessible(doc, authentication);
        return new DocumentResponse(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> findByTabId(Long tabId, Authentication authentication) {
        ProjectTab tab = projectTabRepository.findById(tabId)
                .orElseThrow(() -> new IllegalArgumentException("??씠 議댁옱?섏? ?딆뒿?덈떎."));
        ensureAccessible(tab.getProject(), authentication);

        return documentRepository.findByProjectTabIdOrderByUpdatedAtDesc(tabId).stream()
                .map(DocumentResponse::new)
                .collect(Collectors.toList());
    }

    public Document createNewDocument(Long tabId, String title, String description, Authentication authentication) {
        ProjectTab tab = projectTabRepository.findById(tabId)
                .orElseThrow(() -> new IllegalArgumentException("??씠 議댁옱?섏? ?딆뒿?덈떎."));
        ensureAccessible(tab.getProject(), authentication);

        Document doc = Document.builder()
                .projectTab(tab)
                .project(tab.getProject())
                .title(title)
                .description(description)
                .content("")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return documentRepository.save(doc);
    }

    public void saveContent(Long id, String title, String content, Authentication authentication) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("臾몄꽌媛 議댁옱?섏? ?딆뒿?덈떎. id=" + id));
        ensureAccessible(doc, authentication);

        doc.updateContent(title, content);
        documentVersionRepository.save(new DocumentVersion(doc, content));
    }

    public void updatePureContent(Long id, String title, String content, Authentication authentication) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("臾몄꽌媛 議댁옱?섏? ?딆뒿?덈떎. id=" + id));
        ensureAccessible(doc, authentication);
        doc.updateContent(title, content);
    }

    public void deleteDocument(Long id, Authentication authentication) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("議댁옱?섏? ?딅뒗 臾몄꽌?낅땲??"));
        ensureAccessible(doc, authentication);
        documentRepository.delete(doc);
    }

    public void addComment(Long docId, String author, String content, Authentication authentication) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("臾몄꽌媛 議댁옱?섏? ?딆뒿?덈떎."));
        ensureAccessible(doc, authentication);
        documentCommentRepository.save(new DocumentComment(doc, author, content));
    }

    @Transactional(readOnly = true)
    public List<DocumentComment> findCommentsByDocumentId(Long docId, Authentication authentication) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("臾몄꽌媛 議댁옱?섏? ?딆뒿?덈떎."));
        ensureAccessible(doc, authentication);
        return documentCommentRepository.findByDocumentIdOrderByCreatedAtAsc(docId);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersion> findAllVersionsByDocumentId(Long docId, Authentication authentication) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("臾몄꽌媛 議댁옱?섏? ?딆뒿?덈떎."));
        ensureAccessible(doc, authentication);
        return documentVersionRepository.findByDocumentIdOrderByCreatedAtDesc(docId);
    }

    private void ensureAccessible(Document document, Authentication authentication) {
        Project project = document.getProject();
        if (project == null && document.getProjectTab() != null) {
            project = document.getProjectTab().getProject();
        }
        ensureAccessible(project, authentication);
    }

    private void ensureAccessible(Project project, Authentication authentication) {
        if (project == null) {
            throw new IllegalArgumentException("臾몄꽌?? ?꾨줈?앺듃 ?뺣낫媛 ?놁뒿?덈떎.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        if (!projectMemberRepository.existsByProjectAndUserAndStatus(project, currentUser, "ACCEPTED")) {
            throw new IllegalArgumentException("You do not have access to this document.");
        }
    }
}
