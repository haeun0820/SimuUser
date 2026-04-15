package com.example.simuuser.controller;

import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.entity.Document;
import com.example.simuuser.service.DocumentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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

    // [5] 문서 내용 수정 및 저장 (중복되었던 부분 통합)
    @ResponseBody
    @PutMapping("/api/documents/{id}")
    public ResponseEntity<?> saveDocument(@PathVariable Long id, @RequestBody Map<String, String> body) {
        documentService.saveContent(id, body.get("title"), body.get("content"));
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @DeleteMapping("/api/documents/{id}") // 프론트의 fetch 주소와 일치해야 함
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id); // 서비스에 삭제 로직 구현 필요
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("삭제 실패");
        }
    }
}