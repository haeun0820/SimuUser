package com.example.simuuser.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.simuuser.dto.DocumentRequest;
import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.service.AiDocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentApiController {

    private final AiDocumentService aiDocumentService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateDocument(
            @RequestBody DocumentRequest request,
            Authentication authentication
    ) {
        try {
            return ResponseEntity.ok(aiDocumentService.generateDocument(request, authentication));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "문서 생성 중 오류가 발생했습니다. " + e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getDocumentsByProject(
            @PathVariable("projectId") Long projectId,
            Authentication authentication
    ) {
        try {
            return ResponseEntity.ok(aiDocumentService.getDocumentsByProjectId(projectId, authentication));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "문서 목록 조회 중 오류가 발생했습니다. " + e.getMessage()));
        }
    }

    @PatchMapping("/{documentId}/star")
    public ResponseEntity<?> toggleDocumentStar(@PathVariable("documentId") Long documentId, Authentication authentication) {
        return ResponseEntity.ok(Map.of("starred", aiDocumentService.toggleStarred(documentId, authentication)));
    }
}
