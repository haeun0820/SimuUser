package com.example.simuuser.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LlmApiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // JSON 데이터를 파싱하기 위한 Jackson 라이브러리 객체 (Spring Boot 기본 내장)
    private final ObjectMapper objectMapper;

    public String generateText(String prompt) {
        // 최신 빠르고 가벼운 모델인 gemini-2.5-flash 사용
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Gemini API가 요구하는 JSON 구조에 맞게 데이터 구성
        // { "contents": [ { "parts": [ { "text": "프롬프트 내용" } ] } ] }
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(parts));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            // API로 POST 요청 전송
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // 반환된 JSON에서 생성된 텍스트 부분만 추출
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            
            // 주의: 이 아래부분이 끊기지 않고 .asText(); 까지 잘 연결되어야 합니다!
            String generatedText = rootNode.path("candidates")
                                           .get(0)
                                           .path("content")
                                           .path("parts")
                                           .get(0)
                                           .path("text")
                                           .asText();
            
            return generatedText;

        } catch (Exception e) {
            e.printStackTrace();
            return "AI 문서 생성에 실패했습니다. 오류 내용: " + e.getMessage();
        }
    }
}