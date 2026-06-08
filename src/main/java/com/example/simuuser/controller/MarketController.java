package com.example.simuuser.controller;

import com.example.simuuser.dto.MarketAnalysisResultSaveRequest;
import com.example.simuuser.dto.ProjectResponse;
import com.example.simuuser.service.AdminLogService;
import com.example.simuuser.service.AiPromptService;
import com.example.simuuser.service.MarketAnalysisResultService;
import com.example.simuuser.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Controller
public class MarketController {

    private final ProjectService projectService;
    private final AiPromptService aiPromptService;
    private final MarketAnalysisResultService marketAnalysisResultService;
    private final AdminLogService adminLogService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String geminiApiKey;
    private final String geminiModel;
    private static final String GEMINI_FALLBACK_MODEL = "gemini-2.5-flash-lite";
    private static final int MAX_RETRIES_PER_MODEL = 2;

    public MarketController(
            ProjectService projectService,
            AiPromptService aiPromptService,
            MarketAnalysisResultService marketAnalysisResultService,
            AdminLogService adminLogService,
            @Value("${gemini.api.key:}") String geminiApiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String geminiModel
    ) {
        this.projectService = projectService;
        this.aiPromptService = aiPromptService;
        this.marketAnalysisResultService = marketAnalysisResultService;
        this.adminLogService = adminLogService;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
    }

    @GetMapping({"/market", "/market/market"})
    public String market() {
        return "market/market";  // market.html
    }

    @GetMapping("/market/result")
    public String marketResult() {
        return "market/market_result";  // market_result.html
    }

    @PostMapping("/market/analyze")
    @ResponseBody
    public ResponseEntity<?> analyze(@RequestBody Map<String, Object> body, Authentication authentication) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            adminLogService.logApiError("시장 분석 Gemini API 키 미설정");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "GEMINI_API_KEY is not configured."));
        }

        try {
            Long projectId = number(body.get("projectId"));
            ProjectResponse project = findProject(projectId, authentication);
            Long promptId = number(body.get("promptId"));
            String customPrompt = aiPromptService.renderPrompt(promptId, projectPromptValues(project));
            String selectedModel = aiPromptService.resolveModel(promptId);
            Map<String, Object> geminiResponse = callGemini(buildGeminiRequest(customPrompt == null ? buildPrompt(project) : customPrompt), selectedModel);
            Map<String, Object> result = parseJsonResult(extractText(geminiResponse));

            return ResponseEntity.ok(normalizeResult(result));
        } catch (RestClientException e) {
            adminLogService.logApiError("시장 분석 Gemini 호출 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", toGeminiErrorMessage(e)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            adminLogService.logSystemError("시장 분석 처리 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Market analysis failed: " + e.getMessage()));
        }
    }

    @PostMapping("/market/results")
    @ResponseBody
    public ResponseEntity<?> saveResult(@RequestBody MarketAnalysisResultSaveRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(marketAnalysisResultService.save(request, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            adminLogService.logSystemError("시장 분석 저장 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Market analysis save failed: " + e.getMessage()));
        }
    }

    @GetMapping("/market/results/{resultId}")
    @ResponseBody
    public ResponseEntity<?> result(@PathVariable("resultId") Long resultId, Authentication authentication) {
        try {
            return ResponseEntity.ok(marketAnalysisResultService.findOne(resultId, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            adminLogService.logSystemError("시장 분석 단건 조회 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Market analysis load failed: " + e.getMessage()));
        }
    }

    @GetMapping("/market/results/project/{projectId}")
    @ResponseBody
    public ResponseEntity<?> projectResults(@PathVariable("projectId") Long projectId, Authentication authentication) {
        try {
            return ResponseEntity.ok(marketAnalysisResultService.findByProject(projectId, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            adminLogService.logSystemError("시장 분석 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Market analysis list load failed: " + e.getMessage()));
        }
    }

    @PatchMapping("/market/results/{resultId}/star")
    @ResponseBody
    public ResponseEntity<?> toggleStar(@PathVariable("resultId") Long resultId, Authentication authentication) {
        try {
            return ResponseEntity.ok(Map.of("starred", marketAnalysisResultService.toggleStarred(resultId, authentication)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private ProjectResponse findProject(Long projectId, Authentication authentication) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is required.");
        }

        return projectService.findMine(authentication).stream()
                .filter(project -> projectId.equals(project.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Project not found or not accessible."));
    }

    private Map<String, Object> callGemini(Map<String, Object> requestBody, String preferredModel) {
        List<String> models = buildModelCandidates(preferredModel);
        RestClientException lastException = null;

        for (String model : models) {
            for (int attempt = 1; attempt <= MAX_RETRIES_PER_MODEL; attempt++) {
                try {
                    return callGeminiModel(model, requestBody);
                } catch (RestClientException e) {
                    lastException = e;
                    if (!isTemporaryGeminiUnavailable(e) || attempt == MAX_RETRIES_PER_MODEL) {
                        break;
                    }
                    sleepBeforeRetry(attempt);
                }
            }
        }

        throw lastException == null
                ? new IllegalArgumentException("Gemini response is empty.")
                : lastException;
    }

    private List<String> buildModelCandidates(String preferredModel) {
        String normalizedPreferred = normalizeModel(preferredModel);
        String normalizedDefault = normalizeModel(geminiModel);

        if (normalizedPreferred != null && GEMINI_FALLBACK_MODEL.equals(normalizedPreferred)) {
            return List.of(normalizedPreferred);
        }
        if (normalizedPreferred != null && normalizedPreferred.equals(normalizedDefault)) {
            return GEMINI_FALLBACK_MODEL.equals(normalizedDefault)
                    ? List.of(normalizedDefault)
                    : List.of(normalizedDefault, GEMINI_FALLBACK_MODEL);
        }
        if (normalizedPreferred != null) {
            return List.of(normalizedPreferred, GEMINI_FALLBACK_MODEL);
        }
        return GEMINI_FALLBACK_MODEL.equals(normalizedDefault)
                ? List.of(normalizedDefault)
                : List.of(normalizedDefault, GEMINI_FALLBACK_MODEL);
    }

    private String normalizeModel(String model) {
        if (model == null) {
            return null;
        }

        String normalized = model.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Map<String, Object> callGeminiModel(String model, Map<String, Object> requestBody) {
        String url = UriComponentsBuilder
                .fromUriString("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
                .queryParam("key", geminiApiKey)
                .toUriString();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);
        if (response == null) {
            throw new IllegalArgumentException("Gemini response is empty.");
        }
        return response;
    }

    private boolean isTemporaryGeminiUnavailable(RestClientException e) {
        String message = e.getMessage();
        return message != null
                && (message.contains("503")
                || message.contains("UNAVAILABLE")
                || message.contains("Service Unavailable")
                || message.contains("high demand"));
    }

    private String toGeminiErrorMessage(RestClientException e) {
        if (isTemporaryGeminiUnavailable(e)) {
            return "현재 Gemini 응답이 몰려 잠시 지연되고 있습니다. 잠시 후 다시 시도해주세요.";
        }

        return "AI 분석 요청 중 오류가 발생했습니다. " + e.getMessage();
    }

    private Map<String, Object> buildGeminiRequest(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.5,
                        "responseMimeType", "application/json"
                )
        );
    }

    private String buildPrompt(ProjectResponse project) {
        return """
                You are a market and competitor research analyst for an early-stage product team.

                Analyze the product below and return only valid JSON. Do not include markdown or code fences.

                [Product]
                - Name: %s
                - Description: %s
                - Target user: %s
                - Industry: %s

                Return this exact JSON shape:
                {
                  "competitionLevel": "낮음 | 중간 | 높음",
                  "saturation": 0,
                  "competitorCount": 0,
                  "marketSize": {
                    "tam": { "value": "string", "desc": "string" },
                    "sam": { "value": "string", "desc": "string" },
                    "som": { "value": "string", "desc": "string" }
                  },
                  "keywords": ["#keyword"],
                  "competitors": [
                    { "name": "string", "tags": ["string"], "strength": "string", "weakness": "string" }
                  ],
                  "differentiation": ["string"],
                  "risks": ["string"],
                  "opportunity": "string"
                }

                Rules:
                - Write all content in Korean.
                - competitionLevel must be one of "낮음", "중간", "높음".
                - saturation must be an integer from 0 to 100.
                - competitorCount must match the competitors array length.
                - Include exactly 3 to 5 keywords.
                - Include exactly 3 to 5 competitors.
                - For each competitor, include one concise strength and one concise weakness.
                - Competitor strength should describe what the competitor does well in the market.
                - Competitor weakness should describe the gap or limitation compared with the product.
                - Include exactly 4 differentiation points and 4 risks.
                - Use realistic but clearly estimated TAM/SAM/SOM values when live market data is not available.
                """.formatted(
                text(project.getTitle(), "Untitled product"),
                text(project.getDescription(), "No description"),
                text(project.getTargetUser(), "Not specified"),
                text(project.getIndustry(), "Not specified")
        );
    }

    private String appendMarketJsonContract(String prompt) {
        return prompt + """

                반드시 아래 JSON 형식만 반환하세요. 설명 문장, 마크다운, 코드블록은 넣지 마세요.
                화면 표시를 위해 모든 필드를 채워야 합니다.
                {
                  "competitionLevel": "낮음 | 중간 | 높음",
                  "saturation": 0,
                  "competitorCount": 0,
                  "marketSize": {
                    "tam": { "value": "string", "desc": "string" },
                    "sam": { "value": "string", "desc": "string" },
                    "som": { "value": "string", "desc": "string" }
                  },
                  "keywords": ["#keyword"],
                  "competitors": [
                    { "name": "string", "tags": ["string"], "strength": "string", "weakness": "string" }
                  ],
                  "differentiation": ["string"],
                  "risks": ["string"],
                  "opportunity": "string"
                }

                규칙:
                - 모든 내용은 한국어로 작성하세요.
                - competitionLevel은 "낮음", "중간", "높음" 중 하나여야 합니다.
                - saturation은 0~100 사이 정수여야 합니다.
                - competitorCount는 competitors 배열 길이와 같아야 합니다.
                - keywords는 3~5개 작성하세요.
                - competitors는 3~5개 작성하세요.
                - differentiation과 risks는 각각 3~5개 작성하세요.
                """;
    }

    private Map<String, Object> projectPromptValues(ProjectResponse project) {
        return Map.of(
                "projectTitle", text(project.getTitle(), ""),
                "title", text(project.getTitle(), ""),
                "projectDescription", text(project.getDescription(), ""),
                "description", text(project.getDescription(), ""),
                "targetUser", text(project.getTargetUser(), ""),
                "industry", text(project.getIndustry(), "")
        );
    }

    private String extractText(Map<String, Object> geminiResponse) {
        Object candidatesObject = geminiResponse.get("candidates");
        if (!(candidatesObject instanceof List<?> candidates) || candidates.isEmpty()) {
            throw new IllegalArgumentException("Gemini response has no candidates.");
        }

        Object firstCandidate = candidates.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidate)) {
            throw new IllegalArgumentException("Gemini candidate format is invalid.");
        }

        Object contentObject = candidate.get("content");
        if (!(contentObject instanceof Map<?, ?> content)) {
            throw new IllegalArgumentException("Gemini response has no content.");
        }

        Object partsObject = content.get("parts");
        if (!(partsObject instanceof List<?> parts) || parts.isEmpty()) {
            throw new IllegalArgumentException("Gemini response has no parts.");
        }

        Object firstPart = parts.get(0);
        if (!(firstPart instanceof Map<?, ?> part)) {
            throw new IllegalArgumentException("Gemini part format is invalid.");
        }

        Object text = part.get("text");
        if (text == null || text.toString().isBlank()) {
            throw new IllegalArgumentException("Gemini response text is empty.");
        }

        return text.toString();
    }

    private Map<String, Object> parseJsonResult(String text) throws Exception {
        Object parsed = objectMapper.readValue(stripMarkdownFence(text), Object.class);
        return extractRootObject(parsed);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractRootObject(Object parsed) {
        if (parsed instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        if (parsed instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> firstItem) {
            return (Map<String, Object>) firstItem;
        }
        throw new IllegalArgumentException("AI 응답 최상위 형식이 올바르지 않습니다. JSON 객체로 응답하도록 프롬프트를 확인해주세요.");
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(500L * attempt);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeResult(Map<String, Object> result) {
        List<Map<String, Object>> competitors = asMapList(result.get("competitors")).stream()
                .map(competitor -> Map.of(
                        "name", text(competitor.get("name"), "Unknown"),
                        "tags", asStringList(competitor.get("tags")).stream().limit(4).toList(),
                        "strength", text(competitor.get("strength"), defaultCompetitorStrength(competitor)),
                        "weakness", text(competitor.get("weakness"), "")
                ))
                .limit(5)
                .toList();

        Map<String, Object> marketSize = result.get("marketSize") instanceof Map<?, ?> rawMarketSize
                ? (Map<String, Object>) rawMarketSize
                : Map.of();

        return Map.of(
                "competitionLevel", text(result.get("competitionLevel"), "중간"),
                "saturation", clamp(intNumber(result.get("saturation"), 50), 0, 100),
                "competitorCount", competitors.isEmpty() ? intNumber(result.get("competitorCount"), 0) : competitors.size(),
                "marketSize", Map.of(
                        "tam", normalizeMarketSize(marketSize.get("tam")),
                        "sam", normalizeMarketSize(marketSize.get("sam")),
                        "som", normalizeMarketSize(marketSize.get("som"))
                ),
                "keywords", asStringList(result.get("keywords")).stream().limit(5).toList(),
                "competitors", competitors,
                "differentiation", asStringList(result.get("differentiation")).stream().limit(4).toList(),
                "risks", asStringList(result.get("risks")).stream().limit(4).toList(),
                "opportunity", text(result.get("opportunity"), "")
        );
    }

    private String defaultCompetitorStrength(Map<String, Object> competitor) {
        String name = text(competitor.get("name"), "해당 경쟁사");
        List<String> tags = asStringList(competitor.get("tags"));
        if (!tags.isEmpty()) {
            return name + "는 " + String.join(", ", tags.stream().limit(2).toList()) + " 영역에서 강점이 있습니다.";
        }
        return name + "는 기존 시장 인지도와 운영 경험이 강점입니다.";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeMarketSize(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of("value", "-", "desc", "");
        }

        Map<String, Object> typed = (Map<String, Object>) map;
        return Map.of(
                "value", text(typed.get("value"), "-"),
                "desc", text(typed.get("desc"), "")
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
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

    private String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }

    private Long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int intNumber(Object value, int fallback) {
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
