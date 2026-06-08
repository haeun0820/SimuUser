package com.example.simuuser.controller;

import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.entity.Document;
import com.example.simuuser.entity.DocumentVersion;
import com.example.simuuser.service.AdminLogService;
import com.example.simuuser.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Controller
public class DocumentController {

    private final DocumentService documentService;
    private final AdminLogService adminLogService;

    public DocumentController(DocumentService documentService, AdminLogService adminLogService) {
        this.documentService = documentService;
        this.adminLogService = adminLogService;
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
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable("id") Long id, Authentication authentication) {
        return ResponseEntity.ok(documentService.findById(id, authentication));
    }

    @ResponseBody
    @GetMapping("/api/tabs/{tabId}/documents")
    public List<DocumentResponse> getDocumentsByTab(@PathVariable("tabId") Long tabId, Authentication authentication) {
        return documentService.findByTabId(tabId, authentication);
    }

    @ResponseBody
    @PostMapping("/api/tabs/{tabId}/documents")
    public ResponseEntity<?> createDocument(
            @PathVariable("tabId") Long tabId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        Document savedDoc = documentService.createNewDocument(tabId, body.get("title"), body.get("description"), authentication);
        return ResponseEntity.ok(Map.of("id", savedDoc.getId()));
    }

    @ResponseBody
    @PutMapping("/api/documents/{id}")
    public ResponseEntity<?> saveDocument(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        documentService.saveContent(id, body.get("title"), body.get("content"), authentication);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @PutMapping("/api/documents/{id}/restore")
    public ResponseEntity<?> restoreDocument(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        documentService.updatePureContent(id, body.get("title"), body.get("content"), authentication);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @DeleteMapping("/api/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") Long id, Authentication authentication) {
        try {
            documentService.deleteDocument(id, authentication);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            adminLogService.logSystemError("문서 삭제 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("문서 삭제 중 오류가 발생했습니다.");
        }
    }

    @ResponseBody
    @GetMapping("/api/documents/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable("id") Long id, Authentication authentication) {
        return ResponseEntity.ok(documentService.findCommentsByDocumentId(id, authentication));
    }

    @ResponseBody
    @PostMapping("/api/documents/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable("id") Long id,
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
    public ResponseEntity<List<DocumentVersion>> getDocumentVersions(@PathVariable("id") Long id, Authentication authentication) {
        return ResponseEntity.ok(documentService.findAllVersionsByDocumentId(id, authentication));
    }

    @GetMapping("/api/documents/download/{id}")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable("id") Long id,
            @RequestParam("format") String format,
            Authentication authentication
    ) {
        try {
            DocumentResponse doc = documentService.findById(id, authentication);
            String fileName = safeFileName(doc.getTitle());
            String content = safeText(doc.getContent(), "내용이 없습니다.");

            if ("docx".equalsIgnoreCase(format)) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, buildDisposition(fileName + ".docx").toString())
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                        .body(buildDocxBytes(doc, content));
            }

            if ("hwp".equalsIgnoreCase(format)) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, buildDisposition(fileName + ".hwp").toString())
                        .contentType(MediaType.parseMediaType("application/x-hwp"))
                        .body(buildHwpCompatibleBytes(doc, content));
            }

            return ResponseEntity.badRequest()
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body("지원하지 않는 다운로드 형식입니다.".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            adminLogService.logSystemError("문서 다운로드 실패: " + e.getMessage());
            log.error("Document download failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body("문서 다운로드 중 오류가 발생했습니다.".getBytes(StandardCharsets.UTF_8));
        }
    }

    private byte[] buildDocxBytes(DocumentResponse doc, String content) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            writeZipEntry(zipOutputStream, "[Content_Types].xml", contentTypesXml());
            writeZipEntry(zipOutputStream, "_rels/.rels", rootRelsXml());
            writeZipEntry(zipOutputStream, "word/document.xml", wordDocumentXml(doc, content));
            writeZipEntry(zipOutputStream, "word/_rels/document.xml.rels", documentRelsXml());
            writeZipEntry(zipOutputStream, "word/styles.xml", stylesXml());
            zipOutputStream.finish();
            return outputStream.toByteArray();
        }
    }

    private byte[] buildHwpCompatibleBytes(DocumentResponse doc, String content) {
        Charset hwpCharset = Charset.forName("MS949");
        String html = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta http-equiv="Content-Type" content="text/html; charset=ks_c_5601-1987">
                  <title>%s</title>
                  <style>
                    body { font-family: 'Malgun Gothic', '맑은 고딕', sans-serif; line-height: 1.75; font-size: 10.5pt; margin: 32px; }
                    h1 { text-align: center; font-size: 18pt; margin-bottom: 18pt; }
                    .meta { color: #666666; font-size: 9pt; margin-bottom: 18pt; }
                    p { margin: 0 0 10pt 0; }
                  </style>
                </head>
                <body>
                  <h1>%s</h1>
                  <div class="meta">문서 유형: %s</div>
                  %s
                </body>
                </html>
                """.formatted(
                escapeHtml(safeText(doc.getTitle(), "문서")),
                escapeHtml(safeText(doc.getTitle(), "문서")),
                escapeHtml(safeText(doc.getType(), "미지정")),
                toHtmlParagraphs(content)
        );
        return html.getBytes(hwpCharset);
    }

    private String toHtmlParagraphs(String content) {
        String[] lines = content.replace("\r\n", "\n").split("\n", -1);
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line.isBlank()) {
                builder.append("<p>&nbsp;</p>");
            } else {
                builder.append("<p>").append(escapeHtml(line)).append("</p>");
            }
        }
        return builder.toString();
    }

    private ContentDisposition buildDisposition(String fileName) {
        return ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
    }

    private String safeFileName(String value) {
        return safeText(value, "document").replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void writeZipEntry(ZipOutputStream zipOutputStream, String path, String content) throws Exception {
        zipOutputStream.putNextEntry(new ZipEntry(path));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String contentTypesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
                </Types>
                """;
    }

    private String rootRelsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
                """;
    }

    private String documentRelsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """;
    }

    private String stylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
                    <w:name w:val="Normal"/>
                    <w:qFormat/>
                  </w:style>
                </w:styles>
                """;
    }

    private String wordDocumentXml(DocumentResponse doc, String content) {
        StringBuilder body = new StringBuilder();
        body.append(centerParagraph(safeText(doc.getTitle(), "문서"), true, 32));
        body.append(normalParagraph("문서 유형: " + safeText(doc.getType(), "미지정"), 18));
        if (doc.getCreatedAt() != null) {
            body.append(normalParagraph("생성일: " + doc.getCreatedAt(), 18));
        }

        for (String line : content.replace("\r\n", "\n").split("\n", -1)) {
            body.append(normalParagraph(line.isBlank() ? " " : line, 22));
        }

        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    %s
                    <w:sectPr>
                      <w:pgSz w:w="11906" w:h="16838"/>
                      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="708" w:footer="708" w:gutter="0"/>
                    </w:sectPr>
                  </w:body>
                </w:document>
                """.formatted(body);
    }

    private String centerParagraph(String text, boolean bold, int fontSize) {
        return """
                <w:p>
                  <w:pPr><w:jc w:val="center"/></w:pPr>
                  <w:r>
                    <w:rPr>%s<w:sz w:val="%d"/></w:rPr>
                    <w:t xml:space="preserve">%s</w:t>
                  </w:r>
                </w:p>
                """.formatted(bold ? "<w:b/>" : "", fontSize, escapeXml(text));
    }

    private String normalParagraph(String text, int fontSize) {
        return """
                <w:p>
                  <w:r>
                    <w:rPr><w:sz w:val="%d"/></w:rPr>
                    <w:t xml:space="preserve">%s</w:t>
                  </w:r>
                </w:p>
                """.formatted(fontSize, escapeXml(text));
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
