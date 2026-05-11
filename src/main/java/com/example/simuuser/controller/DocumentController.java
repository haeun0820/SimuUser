package com.example.simuuser.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.entity.Document;
import com.example.simuuser.entity.DocumentVersion;
import com.example.simuuser.service.DocumentService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/document")
    public String documentPage() {
        return "document/document";
    }

    @GetMapping("/document/editor")
    public String openEditor(@RequestParam Long id) {
        return "document/document_editor";
    }

    @ResponseBody
    @GetMapping("/api/documents/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(documentService.findById(id, authentication));
    }

    @ResponseBody
    @GetMapping("/api/tabs/{tabId}/documents")
    public List<DocumentResponse> getDocumentsByTab(@PathVariable Long tabId, Authentication authentication) {
        return documentService.findByTabId(tabId, authentication);
    }

    @ResponseBody
    @PostMapping("/api/tabs/{tabId}/documents")
    public ResponseEntity<?> createDocument(
            @PathVariable Long tabId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        Document savedDoc = documentService.createNewDocument(tabId, body.get("title"), body.get("description"), authentication);
        return ResponseEntity.ok(Map.of("id", savedDoc.getId()));
    }

    @ResponseBody
    @PutMapping("/api/documents/{id}")
    public ResponseEntity<?> saveDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        documentService.saveContent(id, body.get("title"), body.get("content"), authentication);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @PutMapping("/api/documents/{id}/restore")
    public ResponseEntity<?> restoreDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        documentService.updatePureContent(id, body.get("title"), body.get("content"), authentication);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @DeleteMapping("/api/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id, Authentication authentication) {
        try {
            documentService.deleteDocument(id, authentication);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("??젣 ?ㅽ뙣");
        }
    }

    @ResponseBody
    @GetMapping("/api/documents/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(documentService.findCommentsByDocumentId(id, authentication));
    }

    @ResponseBody
    @PostMapping("/api/documents/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Principal principal,
            Authentication authentication
    ) {
        String author = "Anonymous user";

        if (principal instanceof OAuth2AuthenticationToken token) {
            author = token.getPrincipal().getAttribute("email");
        } else if (principal != null) {
            author = principal.getName();
        }

        documentService.addComment(id, author, body.get("content"), authentication);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @GetMapping("/api/documents/{id}/versions")
    public ResponseEntity<List<DocumentVersion>> getDocumentVersions(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(documentService.findAllVersionsByDocumentId(id, authentication));
    }

    @GetMapping("/api/documents/download/{id}")
    public void downloadDocument(
            @PathVariable Long id,
            @RequestParam String format,
            Authentication authentication,
            HttpServletResponse response
    ) {
        try {
            DocumentResponse doc = documentService.findById(id, authentication);
            String rawFileName = doc.getTitle();
            String encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            String content = doc.getContent();
            if (content == null) {
                content = "?댁슜???놁뒿?덈떎.";
            }

            if ("hwp".equalsIgnoreCase(format)) {
                response.setContentType("application/x-hwp; charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + ".hwp\"");

                String hwpWrapper =
                        "<!DOCTYPE html><html lang='ko'><head><meta charset='UTF-8'></head><body>"
                                + content.replace("\n", "<br>")
                                + "</body></html>";

                response.getOutputStream().write(0xEF);
                response.getOutputStream().write(0xBB);
                response.getOutputStream().write(0xBF);
                response.getOutputStream().write(hwpWrapper.getBytes(StandardCharsets.UTF_8));
                response.getOutputStream().flush();
                response.getOutputStream().close();
            }
        } catch (Exception e) {
            log.error("Download error", e);
        }
    }
}
