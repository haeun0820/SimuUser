package com.example.simuuser.controller;

import com.example.simuuser.dto.DocumentRequest;
import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.service.AiDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // JSON 데이터를 주고받는 API 컨트롤러임을 명시
@RequestMapping("/api/documents") // 기본 URL 주소 설정
@RequiredArgsConstructor
public class DocumentApiController {

    private final AiDocumentService aiDocumentService;

    // 프론트엔드의 fetch('/api/documents/generate', { method: 'POST' }) 요청을 여기서 받습니다!
    @PostMapping("/generate")
    public ResponseEntity<DocumentResponse> generateDocument(@RequestBody DocumentRequest request) {
        // 우리가 정성껏 만든 Service 로직을 호출!
        DocumentResponse response = aiDocumentService.generateDocument(request);
        
        // 생성된 결과를 프론트엔드로 반환
        return ResponseEntity.ok(response);
    }
}