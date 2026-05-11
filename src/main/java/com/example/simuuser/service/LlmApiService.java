package com.example.simuuser.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LlmApiService {

    private static final String GEMINI_FALLBACK_MODEL = "gemini-2.5-flash-lite";
    private static final int MAX_RETRIES_PER_MODEL = 2;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public String generateText(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured.");
        }

        RestClientException lastException = null;
        List<String> models = GEMINI_FALLBACK_MODEL.equals(geminiModel)
                ? List.of(geminiModel)
                : List.of(geminiModel, GEMINI_FALLBACK_MODEL);

        for (String model : models) {
            for (int attempt = 1; attempt <= MAX_RETRIES_PER_MODEL; attempt++) {
                try {
                    return callGemini(model, prompt);
                } catch (RestClientException e) {
                    lastException = e;
                    if (!isTemporaryGeminiUnavailable(e) || attempt == MAX_RETRIES_PER_MODEL) {
                        break;
                    }
                    sleepBeforeRetry(attempt);
                }
            }
        }

        if (lastException != null && isTemporaryGeminiUnavailable(lastException)) {
            throw new IllegalStateException("Gemini is currently busy. Please try again shortly.", lastException);
        }

        throw new IllegalStateException(
                lastException == null ? "Gemini request failed." : "Gemini request failed: " + lastException.getMessage(),
                lastException
        );
    }

    private String callGemini(String model, String prompt) {
        String url = UriComponentsBuilder
                .fromUriString("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
                .queryParam("key", apiKey)
                .toUriString();

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);
        if (response == null) {
            throw new IllegalStateException("Gemini response is empty.");
        }

        JsonNode rootNode = objectMapper.valueToTree(response);
        JsonNode textNode = rootNode.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        String generatedText = textNode.asText("");
        if (generatedText.isBlank()) {
            throw new IllegalStateException("Gemini response text is empty.");
        }

        return generatedText;
    }

    private boolean isTemporaryGeminiUnavailable(RestClientException e) {
        String message = e.getMessage();
        return message != null
                && (message.contains("503")
                || message.contains("UNAVAILABLE")
                || message.contains("Service Unavailable")
                || message.contains("high demand"));
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(500L * attempt);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
