package com.example.simuuser.controller;

import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.entity.Document;
import com.example.simuuser.entity.DocumentVersion;
import com.example.simuuser.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // [1] 페이지 이동 전용
    @GetMapping("/document")
    public String documentPage() {
        return "document/document";
    }

    @GetMapping("/document/editor")
    public String openEditor(@RequestParam Long id) {
        return "document/document_editor";
    }

    // [2] 문서 단건 조회 (에디터 데이터 채우기용)
    @ResponseBody
    @GetMapping("/api/documents/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.findById(id));
    }

    // [3] 특정 탭의 문서 목록 조회
    @ResponseBody
    @GetMapping("/api/tabs/{tabId}/documents")
    public List<DocumentResponse> getDocumentsByTab(@PathVariable Long tabId) {
        return documentService.findByTabId(tabId);
    }

    // [4] 새 문서 생성
    @ResponseBody
    @PostMapping("/api/tabs/{tabId}/documents")
    public ResponseEntity<?> createDocument(@PathVariable Long tabId, @RequestBody Map<String, String> body) {
        Document savedDoc = documentService.createNewDocument(tabId, body.get("title"), body.get("description"));
        return ResponseEntity.ok(Map.of("id", savedDoc.getId()));
    }

    // [5] 문서 내용 수정 및 저장 (버전 기록 자동 생성 포함)
    @ResponseBody
    @PutMapping("/api/documents/{id}")
    public ResponseEntity<?> saveDocument(@PathVariable Long id, @RequestBody Map<String, String> body) {
        documentService.saveContent(id, body.get("title"), body.get("content"));
        return ResponseEntity.ok().build();
    }

    // [6] 문서 복구 (버전 기록 생성 없이 내용만 업데이트)
    @ResponseBody
    @PutMapping("/api/documents/{id}/restore")
    public ResponseEntity<?> restoreDocument(@PathVariable Long id, @RequestBody Map<String, String> body) {
        documentService.updatePureContent(id, body.get("title"), body.get("content"));
        return ResponseEntity.ok().build();
    }

    // [7] 문서 삭제
    @ResponseBody
    @DeleteMapping("/api/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("삭제 실패");
        }
    }

    // [8] 특정 문서의 모든 댓글 가져오기
    @ResponseBody
    @GetMapping("/api/documents/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.findCommentsByDocumentId(id));
    }

    // [9] 댓글 작성 (OAuth2 이메일 연동)
    @ResponseBody
    @PostMapping("/api/documents/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id, 
                                        @RequestBody Map<String, String> body,
                                        Principal principal) {
        String author = "익명 사용자";
        
        if (principal instanceof OAuth2AuthenticationToken token) {
            // 구글 로그인 사용자의 경우 이메일을 작성자로 설정
            author = token.getPrincipal().getAttribute("email"); 
        } else if (principal != null) {
            author = principal.getName();
        }
        
        documentService.addComment(id, author, body.get("content"));
        return ResponseEntity.ok().build();
    }

    // [10] 특정 문서의 모든 버전 기록 가져오기
    @ResponseBody
    @GetMapping("/api/documents/{id}/versions")
    public ResponseEntity<List<DocumentVersion>> getDocumentVersions(@PathVariable Long id) {
        List<DocumentVersion> versions = documentService.findAllVersionsByDocumentId(id);
        return ResponseEntity.ok(versions);
    }
}