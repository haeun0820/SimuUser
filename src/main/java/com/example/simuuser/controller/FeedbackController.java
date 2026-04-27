package com.example.simuuser.controller;

import com.example.simuuser.dto.FeedbackAnalysisResultSaveRequest;
import com.example.simuuser.dto.ProjectResponse;
import com.example.simuuser.service.FeedbackAnalysisResultService;
import com.example.simuuser.service.ProjectService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class FeedbackController {

    private final ProjectService projectService;
    private final FeedbackAnalysisResultService feedbackAnalysisResultService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String geminiApiKey;
    private final String geminiModel;
    private static final String GEMINI_FALLBACK_MODEL = "gemini-2.5-flash-lite";

    public FeedbackController(
            ProjectService projectService,
            FeedbackAnalysisResultService feedbackAnalysisResultService,
            @Value("${gemini.api.key:}") String geminiApiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String geminiModel
    ) {
        this.projectService = projectService;
        this.feedbackAnalysisResultService = feedbackAnalysisResultService;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
    }

    @GetMapping("/feedback")
    public String feedbackPage() {
        return "feedback/feedback";
    }

    @GetMapping("/feedback/result")
    public String feedbackResultPage(Model model) {
        Map<String, Object> result = emptyAnalysisData("분석할 기획 내용이 없습니다. 다시 분석을 실행해주세요.");
        model.addAttribute("result", result);
        model.addAttribute("feedbackResultData", result);
        model.addAttribute("analysisRequest", Map.of(
                "projectId", null,
                "sourceType", "project",
                "sourceContent", ""
        ));
        return "feedback/feedback_result";
    }

    @PostMapping("/feedbackresult")
    public String getFeedbackResult(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "textContent", required = false) String textContent,
            Model model,
            Authentication authentication
    ) {
        String sourceType = resolveSourceType(file, textContent);
        String sourceContent = "";

        try {
            ProjectResponse project = findProject(projectId, authentication);
            sourceContent = readPlanText(file, textContent, project);
            Map<String, Object> geminiResponse = callGemini(buildGeminiRequest(buildPrompt(project, sourceContent)));
            Map<String, Object> result = normalizeResult(parseJsonResult(extractText(geminiResponse)));
            applyResultModel(model, result, projectId, sourceType, sourceContent);
        } catch (RestClientException e) {
            applyResultModel(model, emptyAnalysisData(toGeminiErrorMessage(e)), projectId, sourceType, sourceContent);
        } catch (Exception e) {
            applyResultModel(model, emptyAnalysisData("기획 피드백 분석에 실패했습니다: " + e.getMessage()), projectId, sourceType, sourceContent);
        }

        return "feedback/feedback_result";
    }

    @PostMapping("/feedback/results")
    @ResponseBody
    public Map<String, Object> saveFeedbackResult(@RequestBody FeedbackAnalysisResultSaveRequest request, Authentication authentication) {
        try {
            return Map.of("saved", feedbackAnalysisResultService.save(request, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private void applyResultModel(Model model, Map<String, Object> result, Long projectId, String sourceType, String sourceContent) {
        model.addAttribute("result", result);
        model.addAttribute("feedbackResultData", result);
        model.addAttribute("analysisRequest", Map.of(
                "projectId", projectId,
                "sourceType", sourceType,
                "sourceContent", sourceContent == null ? "" : sourceContent
        ));
    }

    private ProjectResponse findProject(Long projectId, Authentication authentication) {
        if (projectId == null) {
            return null;
        }

        return projectService.findMine(authentication).stream()
                .filter(project -> projectId.equals(project.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found or not accessible."));
    }

    private String readPlanText(MultipartFile file, String textContent, ProjectResponse project) throws Exception {
        String directText = text(textContent, "");
        if (!directText.isBlank()) {
            return directText;
        }

        if (file != null && !file.isEmpty()) {
            return new String(file.getBytes(), StandardCharsets.UTF_8).trim();
        }

        if (project != null) {
            return """
                    프로젝트명: %s
                    설명: %s
                    타겟 사용자: %s
                    산업 분야: %s
                    """.formatted(
                    text(project.getTitle(), ""),
                    text(project.getDescription(), ""),
                    text(project.getTargetUser(), ""),
                    text(project.getIndustry(), "")
            ).trim();
        }

        throw new IllegalArgumentException("분석할 기획 내용이 필요합니다.");
    }

    private String resolveSourceType(MultipartFile file, String textContent) {
        if (textContent != null && !textContent.trim().isBlank()) {
            return "text";
        }
        if (file != null && !file.isEmpty()) {
            return "file";
        }
        return "project";
    }

    private Map<String, Object> callGemini(Map<String, Object> requestBody) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured.");
        }

        List<String> models = GEMINI_FALLBACK_MODEL.equals(geminiModel)
                ? List.of(geminiModel)
                : List.of(geminiModel, GEMINI_FALLBACK_MODEL);
        RestClientException lastException = null;

        for (String model : models) {
            try {
                return callGeminiModel(model, requestBody);
            } catch (RestClientException e) {
                lastException = e;
                if (!isTemporaryGeminiUnavailable(e)) {
                    throw e;
                }
            }
        }

        throw lastException == null
                ? new IllegalArgumentException("Gemini response is empty.")
                : lastException;
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

    private Map<String, Object> buildGeminiRequest(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "responseMimeType", "application/json"
                )
        );
    }

    private String buildPrompt(ProjectResponse project, String planText) {
        String projectContext = project == null
                ? "선택된 프로젝트 없음"
                : """
                - 프로젝트명: %s
                - 설명: %s
                - 타겟 사용자: %s
                - 산업 분야: %s
                """.formatted(
                text(project.getTitle(), "미입력"),
                text(project.getDescription(), "미입력"),
                text(project.getTargetUser(), "미입력"),
                text(project.getIndustry(), "미입력")
        );

        return """
                당신은 스타트업 사업기획서와 제품기획서를 검토하는 한국어 AI 피드백 전문가입니다.
                아래 기획 내용을 분석하고 JSON만 반환하세요. 마크다운, 코드블록, 설명 문장은 절대 넣지 마세요.

                [프로젝트 정보]
                %s

                [기획서 내용]
                %s

                반환 JSON 형식:
                {
                  "totalScore": 0,
                  "logicScore": 0,
                  "completionScore": 0,
                  "feasibilityScore": 0,
                  "strengths": ["강점"],
                  "weaknesses": ["약점"],
                  "missingElements": ["부족한 요소"],
                  "improvements": ["개선 방향"],
                  "risks": ["리스크"],
                  "scenarios": {
                    "best": "최상 시나리오와 대응방안",
                    "normal": "보통 시나리오와 대응방안",
                    "worst": "최악 시나리오와 대응방안"
                  }
                }

                규칙:
                - 모든 텍스트는 한국어로 작성하세요.
                - 점수는 0부터 100 사이 정수로 작성하세요.
                - totalScore는 logicScore, completionScore, feasibilityScore를 종합한 일관적인 점수로 작성하세요.
                - strengths는 2~5개, weaknesses는 3~6개로 작성하세요.
                - missingElements는 4~10개로 작성하세요.
                - improvements는 4~6개로 작성하세요.
                - risks는 3~5개로 작성하세요.
                - 입력 내용이 부족하면 낮은 점수를 주고, 어떤 정보가 부족한지 구체적으로 지적하세요.
                """.formatted(projectContext, planText);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeResult(Map<String, Object> raw) {
        Map<String, Object> scenarios = raw.get("scenarios") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();

        return Map.of(
                "totalScore", clamp(intNumber(raw.get("totalScore"), 0), 0, 100),
                "logicScore", clamp(intNumber(raw.get("logicScore"), 0), 0, 100),
                "completionScore", clamp(intNumber(raw.get("completionScore"), 0), 0, 100),
                "feasibilityScore", clamp(intNumber(raw.get("feasibilityScore"), 0), 0, 100),
                "strengths", limitList(raw.get("strengths"), 5),
                "weaknesses", limitList(raw.get("weaknesses"), 6),
                "missingElements", limitList(raw.get("missingElements"), 10),
                "improvements", limitList(raw.get("improvements"), 6),
                "risks", limitList(raw.get("risks"), 5),
                "scenarios", Map.of(
                        "best", text(scenarios.get("best"), ""),
                        "normal", text(scenarios.get("normal"), ""),
                        "worst", text(scenarios.get("worst"), "")
                )
        );
    }

    private Map<String, Object> emptyAnalysisData(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", 0);
        result.put("logicScore", 0);
        result.put("completionScore", 0);
        result.put("feasibilityScore", 0);
        result.put("strengths", List.of());
        result.put("weaknesses", List.of(message));
        result.put("missingElements", List.of());
        result.put("improvements", List.of("기획 내용을 입력하거나 텍스트 또는 파일을 업로드한 뒤 다시 분석을 실행하세요."));
        result.put("risks", List.of());
        result.put("scenarios", Map.of("best", "", "normal", "", "worst", ""));
        return result;
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
            return "Gemini model is currently busy. Please try again shortly.";
        }

        return "Gemini API call failed: " + e.getMessage();
    }

    private List<String> limitList(Object value, int max) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .filter(item -> item != null && !item.toString().isBlank())
                .map(Object::toString)
                .limit(max)
                .toList();
    }

    private String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
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
