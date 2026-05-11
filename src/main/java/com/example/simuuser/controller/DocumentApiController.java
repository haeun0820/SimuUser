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
    public ResponseEntity<DocumentResponse> generateDocument(
            @RequestBody DocumentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(aiDocumentService.generateDocument(request, authentication));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByProject(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(aiDocumentService.getDocumentsByProjectId(projectId, authentication));
    }

    @PatchMapping("/{documentId}/star")
    public ResponseEntity<?> toggleDocumentStar(@PathVariable Long documentId, Authentication authentication) {
        return ResponseEntity.ok(Map.of("starred", aiDocumentService.toggleStarred(documentId, authentication)));
    }
}
