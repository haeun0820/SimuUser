package com.example.simuuser.controller;

import com.example.simuuser.dto.ProjectResponse;
import com.example.simuuser.service.ProjectService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/aiuser")
public class AiUserController {

    private final ObjectMapper objectMapper;
    private final ProjectService projectService;
    private final RestTemplate restTemplate;
    private final String geminiApiKey;
    private final String geminiModel;

    public AiUserController(
            ObjectMapper objectMapper,
            ProjectService projectService,
            @Value("${gemini.api.key:}") String geminiApiKey,
            @Value("${gemini.model:gemini-1.5-flash}") String geminiModel
    ) {
        this.objectMapper = objectMapper;
        this.projectService = projectService;
        this.restTemplate = new RestTemplate();
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
    }

    @GetMapping("/ai_user")
    public String aiUserPage(Model model, Authentication authentication) {
        List<ProjectResponse> projects = projectService.findMine(authentication);
        model.addAttribute("projects", projects);
        return "aiuser/ai_user";
    }

    @GetMapping("/result")
    public String aiUserResultPage() {
        return "aiuser/ai_user_result";
    }

    @PostMapping("/simulate")
    @ResponseBody
    public ResponseEntity<?> simulate(@RequestBody Map<String, Object> body) {
        int personaCount = clamp(number(body.get("personaCount"), 3), 2, 3);

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "GEMINI_API_KEY가 설정되지 않았습니다."));
        }

        try {
            Map<String, Object> requestBody = buildGeminiRequest(buildPrompt(body, personaCount));
            Map<String, Object> geminiResponse = callGemini(requestBody);
            String resultText = extractText(geminiResponse);
            Map<String, Object> result = parseJsonResult(resultText);

            return ResponseEntity.ok(normalizeResult(result, personaCount));
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Gemini API 호출 중 오류가 발생했습니다: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "AI 응답 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    private Map<String, Object> callGemini(Map<String, Object> requestBody) {
        String url = UriComponentsBuilder
                .fromUriString("https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent")
                .queryParam("key", geminiApiKey)
                .toUriString();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);
        if (response == null) {
            throw new IllegalArgumentException("Gemini 응답이 비어 있습니다.");
        }
        return response;
    }

    private Map<String, Object> buildGeminiRequest(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "responseMimeType", "application/json"
                )
        );
    }

    private String extractText(Map<String, Object> geminiResponse) {
        Object candidatesObject = geminiResponse.get("candidates");
        if (!(candidatesObject instanceof List<?> candidates) || candidates.isEmpty()) {
            throw new IllegalArgumentException("Gemini 응답에 candidates가 없습니다.");
        }

        Object firstCandidate = candidates.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidate)) {
            throw new IllegalArgumentException("Gemini 응답 형식이 올바르지 않습니다.");
        }

        Object contentObject = candidate.get("content");
        if (!(contentObject instanceof Map<?, ?> content)) {
            throw new IllegalArgumentException("Gemini 응답에 content가 없습니다.");
        }

        Object partsObject = content.get("parts");
        if (!(partsObject instanceof List<?> parts) || parts.isEmpty()) {
            throw new IllegalArgumentException("Gemini 응답에 parts가 없습니다.");
        }

        Object firstPart = parts.get(0);
        if (!(firstPart instanceof Map<?, ?> part)) {
            throw new IllegalArgumentException("Gemini 응답 part 형식이 올바르지 않습니다.");
        }

        Object text = part.get("text");
        if (text == null || text.toString().isBlank()) {
            throw new IllegalArgumentException("Gemini 응답 text가 비어 있습니다.");
        }

        return text.toString();
    }

    private Map<String, Object> parseJsonResult(String text) throws Exception {
        return objectMapper.readValue(stripMarkdownFence(text), new TypeReference<>() {});
    }

    private String stripMarkdownFence(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstLineEnd = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFence <= firstLineEnd) {
            return trimmed;
        }

        return trimmed.substring(firstLineEnd + 1, lastFence).trim();
    }

    private Map<String, Object> normalizeResult(Map<String, Object> result, int personaCount) {
        List<Map<String, Object>> personas = asPersonaList(result.get("personas"), personaCount);
        return Map.of(
                "avgPurchaseIntent", clamp(number(result.get("avgPurchaseIntent"), 0), 0, 100),
                "overallReaction", text(result.get("overallReaction"), "AI가 전반적인 반응을 생성하지 못했습니다."),
                "personas", personas,
                "keyInsights", asStringList(result.get("keyInsights")),
                "improvements", asStringList(result.get("improvements"))
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asPersonaList(Object value, int personaCount) {
        if (!(value instanceof List<?> rawPersonas)) {
            throw new IllegalArgumentException("AI 응답에 personas 배열이 없습니다.");
        }

        List<Map<String, Object>> personas = rawPersonas.stream()
                .filter(Map.class::isInstance)
                .map(item -> normalizePersona((Map<String, Object>) item))
                .toList();

        if (personas.isEmpty()) {
            throw new IllegalArgumentException("AI 응답에 사용할 수 있는 페르소나가 없습니다.");
        }

        return personas.stream()
                .limit(personaCount)
                .toList();
    }

    private Map<String, Object> normalizePersona(Map<String, Object> persona) {
        return Map.of(
                "name", text(persona.get("name"), "가상 유저"),
                "age", clamp(number(persona.get("age"), 0), 0, 120),
                "job", text(persona.get("job"), "직업 미입력"),
                "consumerType", text(persona.get("consumerType"), "일반형"),
                "purchaseScore", clamp(number(persona.get("purchaseScore"), 0), 0, 100),
                "positiveReactions", asStringList(persona.get("positiveReactions")),
                "negativeReactions", asStringList(persona.get("negativeReactions")),
                "churnPoints", asStringList(persona.get("churnPoints"))
        );
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .filter(item -> item != null && !item.toString().isBlank())
                .map(Object::toString)
                .toList();
    }

    private String buildPrompt(Map<String, Object> body, int personaCount) {
        String serviceIdea = text(body.get("serviceIdea"), "미입력");
        String description = text(body.get("description"), "없음");
        String targetUser = text(body.get("targetUser"), "없음");
        String industry = text(body.get("industry"), "미지정");
        String gender = text(body.get("gender"), "전체");
        String ages = text(body.get("ages"), "전체");
        String job = text(body.get("job"), "미입력");

        return """
                당신은 스타트업 서비스 아이디어를 검증하는 AI 가상 유저 시뮬레이션 전문가입니다.

                아래 서비스 정보를 기준으로 실제 타겟 유저처럼 반응하는 가상 유저 %d명을 생성하고,
                구매 의사, 긍정 반응, 부정 반응, 이탈 포인트, 핵심 인사이트, 개선 제안을 작성해 주세요.

                [서비스 정보]
                - 서비스 아이디어: %s
                - 상세 설명: %s
                - 타겟 유저: %s
                - 산업 분야: %s

                [시뮬레이션 설정]
                - 페르소나 수: %d명
                - 성별: %s
                - 나이대: %s
                - 직업: %s

                반드시 아래 JSON 형식만 반환하세요. 설명 문장, 마크다운, 코드블록은 넣지 마세요.
                {
                  "avgPurchaseIntent": 0,
                  "overallReaction": "전반적인 반응 2~3문장",
                  "personas": [
                    {
                      "name": "한국어 이름",
                      "age": 28,
                      "job": "직업",
                      "consumerType": "신중형",
                      "purchaseScore": 80,
                      "positiveReactions": ["긍정 반응 1", "긍정 반응 2", "긍정 반응 3"],
                      "negativeReactions": ["부정 반응 1", "부정 반응 2", "부정 반응 3"],
                      "churnPoints": ["이탈 포인트 1", "이탈 포인트 2"]
                    }
                  ],
                  "keyInsights": ["인사이트 1", "인사이트 2", "인사이트 3", "인사이트 4"],
                  "improvements": ["개선 제안 1", "개선 제안 2", "개선 제안 3", "개선 제안 4"]
                }

                personas 배열 길이는 반드시 %d개여야 합니다.
                avgPurchaseIntent와 purchaseScore는 0부터 100 사이 정수여야 합니다.
                응답은 한국어로 작성하세요.
                """.formatted(
                personaCount,
                serviceIdea,
                description,
                targetUser,
                industry,
                personaCount,
                gender,
                ages,
                job,
                personaCount
        );
    }

    private String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }

    private int number(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
