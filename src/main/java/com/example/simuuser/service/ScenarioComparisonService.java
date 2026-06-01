package com.example.simuuser.service;

import com.example.simuuser.dto.ScenarioComparisonInput;
import com.example.simuuser.dto.ScenarioComparisonRequest;
import com.example.simuuser.dto.ScenarioComparisonResultResponse;
import com.example.simuuser.dto.ScenarioComparisonResultSaveRequest;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ScenarioComparisonResult;
import com.example.simuuser.repository.ProjectMemberRepository;
import com.example.simuuser.repository.ProjectRepository;
import com.example.simuuser.repository.ScenarioComparisonResultRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScenarioComparisonService {

    private static final List<String> CRITERIA = List.of(
            "사업성",
            "사용자 가치",
            "구현 가능성",
            "명확성",
            "시장 경쟁력"
    );

    private final ScenarioComparisonResultRepository scenarioComparisonResultRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final AiPromptService aiPromptService;
    private final LlmApiService llmApiService;
    private final ObjectMapper objectMapper;

    public ScenarioComparisonService(
            ScenarioComparisonResultRepository scenarioComparisonResultRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectService projectService,
            AiPromptService aiPromptService,
            LlmApiService llmApiService,
            ObjectMapper objectMapper
    ) {
        this.scenarioComparisonResultRepository = scenarioComparisonResultRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
        this.aiPromptService = aiPromptService;
        this.llmApiService = llmApiService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generate(ScenarioComparisonRequest request, Authentication authentication) {
        if (request == null || request.getProjectId() == null) {
            throw new IllegalArgumentException("projectId is required.");
        }
        if (request.getScenarios() == null || request.getScenarios().size() < 2) {
            throw new IllegalArgumentException("At least two scenarios are required.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = findAccessibleProject(request.getProjectId(), currentUser);
        String compareTitle = normalizeText(request.getCompareTitle(), "시나리오 비교");
        List<ScenarioComparisonInput> inputs = request.getScenarios();

        String prompt = aiPromptService.renderPrompt(request.getPromptId(), scenarioPromptValues(project, compareTitle, inputs));
        if (prompt == null) {
            prompt = buildPrompt(project, compareTitle, inputs);
        }

        String selectedModel = aiPromptService.resolveModel(request.getPromptId());
        String generatedText = llmApiService.generateText(prompt, selectedModel);
        Map<String, Object> parsed = parseJsonResult(generatedText);
        return normalizeResult(parsed, compareTitle, inputs);
    }

    @Transactional
    public ScenarioComparisonResultResponse save(ScenarioComparisonResultSaveRequest request, Authentication authentication) {
        if (request == null || request.getProjectId() == null) {
            throw new IllegalArgumentException("projectId is required.");
        }
        if (request.getResult() == null || request.getResult().isEmpty()) {
            throw new IllegalArgumentException("result is required.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = findAccessibleProject(request.getProjectId(), currentUser);
        String recommendedTitle = normalizeText(String.valueOf(request.getResult().get("recommendedScenarioTitle")), "추천 시나리오");

        ScenarioComparisonResult saved = scenarioComparisonResultRepository.save(new ScenarioComparisonResult(
                project,
                currentUser,
                normalizeText(request.getCompareTitle(), "시나리오 비교"),
                recommendedTitle,
                toJson(request.getResult())
        ));

        return new ScenarioComparisonResultResponse(saved, request.getResult());
    }

    @Transactional(readOnly = true)
    public List<ScenarioComparisonResultResponse> findByProject(Long projectId, Authentication authentication) {
        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = findAccessibleProject(projectId, currentUser);

        return scenarioComparisonResultRepository.findByProjectOrderByCreatedAtDesc(project)
                .stream()
                .map(result -> new ScenarioComparisonResultResponse(result, fromJson(result.getResultJson())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ScenarioComparisonResultResponse findOne(Long resultId, Authentication authentication) {
        AppUser currentUser = projectService.getCurrentUser(authentication);
        ScenarioComparisonResult result = scenarioComparisonResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Scenario result not found."));

        findAccessibleProject(result.getProject().getId(), currentUser);
        return new ScenarioComparisonResultResponse(result, fromJson(result.getResultJson()));
    }

    @Transactional
    public boolean toggleStarred(Long resultId, Authentication authentication) {
        AppUser currentUser = projectService.getCurrentUser(authentication);
        ScenarioComparisonResult result = scenarioComparisonResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Scenario result not found."));

        findAccessibleProject(result.getProject().getId(), currentUser);
        return result.toggleStarred();
    }

    private Project findAccessibleProject(Long projectId, AppUser currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found."));

        if (!projectMemberRepository.existsByProjectAndUserAndStatus(project, currentUser, "ACCEPTED")) {
            throw new IllegalArgumentException("You do not have access to this project.");
        }

        return project;
    }

    private Map<String, Object> scenarioPromptValues(Project project, String compareTitle, List<ScenarioComparisonInput> scenarios) {
        return Map.of(
                "projectTitle", text(project.getTitle(), ""),
                "projectDescription", text(project.getDescription(), ""),
                "compareTitle", compareTitle,
                "scenarioSummaries", buildScenarioSummaries(scenarios)
        );
    }

    private String buildScenarioSummaries(List<ScenarioComparisonInput> scenarios) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < scenarios.size(); i++) {
            ScenarioComparisonInput scenario = scenarios.get(i);
            builder.append("시나리오 ").append(i + 1).append('\n')
                    .append("- key: S").append(i + 1).append('\n')
                    .append("- title: ").append(normalizeText(scenario.getTitle(), "시나리오 " + (i + 1))).append('\n')
                    .append("- mode: ").append(normalizeMode(scenario.getMode())).append('\n')
                    .append("- summary: ").append(text(trimLongText(scenario.getSummary()), "요약 없음")).append('\n')
                    .append("- features: ").append(joinOrFallback(normalizeList(scenario.getFeatures()), "없음")).append('\n')
                    .append("- references: ").append(joinOrFallback(normalizeList(scenario.getReferences()), "없음"));

            if (i < scenarios.size() - 1) {
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }

    private String buildPrompt(Project project, String compareTitle, List<ScenarioComparisonInput> scenarios) {
        return """
                당신은 스타트업 기획안 비교를 수행하는 한국어 전략 분석가다.
                아래 프로젝트와 시나리오들을 비교 평가하고 반드시 JSON 객체 하나만 반환하라.
                설명 문장, 마크다운, 코드블록은 절대 포함하지 마라.

                [프로젝트]
                - 제목: %s
                - 설명: %s
                - 타겟 사용자: %s
                - 산업: %s

                [비교 제목]
                %s

                [시나리오 목록]
                %s

                반드시 아래 JSON 구조만 반환하라.
                {
                  "compareTitle": "string",
                  "recommendedScenarioKey": "S1",
                  "recommendedScenarioTitle": "string",
                  "recommendationReason": "string",
                  "scenarios": [
                    {
                      "key": "S1",
                      "title": "string",
                      "mode": "upload | project | direct",
                      "summary": "string",
                      "features": ["string"],
                      "references": ["string"],
                      "totalScore": 0,
                      "scores": {
                        "사업성": 0,
                        "사용자 가치": 0,
                        "구현 가능성": 0,
                        "명확성": 0,
                        "시장 경쟁력": 0
                      },
                      "pros": ["string"],
                      "cons": ["string"]
                    }
                  ],
                  "criteria": [
                    {
                      "name": "사업성",
                      "winnerScenarioKey": "S1",
                      "winnerScenarioTitle": "string",
                      "values": [
                        { "scenarioKey": "S1", "scenarioTitle": "string", "score": 0 }
                      ]
                    }
                  ],
                  "finalSuggestion": "string",
                  "hybridSuggestion": "string"
                }

                규칙:
                - scenarios 길이는 입력된 시나리오 개수와 반드시 같아야 한다.
                - criteria는 반드시 사업성, 사용자 가치, 구현 가능성, 명확성, 시장 경쟁력 5개를 모두 포함해야 한다.
                - totalScore와 scores 값은 모두 0~100 정수여야 한다.
                - pros와 cons는 각각 2~4개로 작성하라.
                - recommendationReason, finalSuggestion, hybridSuggestion은 바로 의사결정에 쓸 수 있게 구체적으로 작성하라.
                - 모든 텍스트는 한국어로 작성하라.
                """.formatted(
                text(project.getTitle(), "프로젝트"),
                text(project.getDescription(), "설명 없음"),
                text(project.getTargetUser(), "미정"),
                text(project.getIndustry(), "미정"),
                compareTitle,
                buildScenarioSummaries(scenarios)
        );
    }

    private Map<String, Object> parseJsonResult(String text) {
        try {
            Object parsed = objectMapper.readValue(stripMarkdownFence(text), Object.class);
            return extractRootObject(parsed);
        } catch (Exception e) {
            throw new IllegalArgumentException("AI 응답을 시나리오 비교 결과 JSON으로 해석하지 못했습니다.");
        }
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

    private String stripMarkdownFence(String text) {
        String trimmed = text == null ? "" : text.trim();
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
    private Map<String, Object> normalizeResult(Map<String, Object> raw, String compareTitle, List<ScenarioComparisonInput> inputs) {
        List<Map<String, Object>> rawScenarios = asMapList(raw.get("scenarios"));
        List<Map<String, Object>> scenarios = new ArrayList<>();
        List<String> scenarioKeys = new ArrayList<>();

        for (int i = 0; i < inputs.size(); i++) {
            ScenarioComparisonInput input = inputs.get(i);
            Map<String, Object> rawScenario = i < rawScenarios.size() ? rawScenarios.get(i) : Map.of();
            String key = "S" + (i + 1);
            String title = normalizeText(text(rawScenario.get("title"), input.getTitle()), "시나리오 " + (i + 1));
            String mode = normalizeMode(text(rawScenario.get("mode"), input.getMode()));
            String summary = text(rawScenario.get("summary"), text(trimLongText(input.getSummary()), ""));
            List<String> features = limitList(asStringList(rawScenario.get("features")), normalizeList(input.getFeatures()), 6);
            List<String> references = limitList(asStringList(rawScenario.get("references")), normalizeList(input.getReferences()), 6);
            Map<String, Integer> scores = normalizeScores((Map<String, Object>) (rawScenario.get("scores") instanceof Map<?, ?> map ? map : Map.of()));
            int totalScore = clamp(intNumber(rawScenario.get("totalScore"), averageScore(scores)), 0, 100);
            List<String> pros = ensureMinItems(limitList(asStringList(rawScenario.get("pros")), List.of(), 4), 2,
                    title + "의 강점 보완 포인트를 추가로 정리할 수 있습니다.");
            List<String> cons = ensureMinItems(limitList(asStringList(rawScenario.get("cons")), List.of(), 4), 2,
                    title + "은 추가 검증이 필요한 리스크가 남아 있습니다.");

            Map<String, Object> scenario = new LinkedHashMap<>();
            scenario.put("key", key);
            scenario.put("title", title);
            scenario.put("mode", mode);
            scenario.put("summary", summary);
            scenario.put("features", features);
            scenario.put("references", references);
            scenario.put("totalScore", totalScore);
            scenario.put("scores", scores);
            scenario.put("pros", pros);
            scenario.put("cons", cons);

            scenarios.add(scenario);
            scenarioKeys.add(key);
        }

        String rawRecommendedKey = text(raw.get("recommendedScenarioKey"), scenarioKeys.get(0));
        final String recommendedKey = scenarioKeys.contains(rawRecommendedKey)
                ? rawRecommendedKey
                : scenarioKeys.get(0);

        Map<String, Object> recommendedScenario = scenarios.stream()
                .filter(item -> recommendedKey.equals(item.get("key")))
                .findFirst()
                .orElse(scenarios.get(0));

        String recommendedTitle = text(raw.get("recommendedScenarioTitle"), text(recommendedScenario.get("title"), "시나리오"));
        List<Map<String, Object>> criteria = normalizeCriteria(asMapList(raw.get("criteria")), scenarios);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("compareTitle", compareTitle);
        result.put("recommendedScenarioKey", recommendedScenario.get("key"));
        result.put("recommendedScenarioTitle", recommendedTitle);
        result.put("recommendationReason", text(raw.get("recommendationReason"), recommendedTitle + "이(가) 전체 균형 측면에서 가장 적합합니다."));
        result.put("scenarios", scenarios);
        result.put("criteria", criteria);
        result.put("finalSuggestion", text(raw.get("finalSuggestion"), recommendedTitle + "을(를) 중심으로 다음 단계 검증을 진행하는 것이 적절합니다."));
        result.put("hybridSuggestion", text(raw.get("hybridSuggestion"), "상위 시나리오의 강점만 결합해 하이브리드 대안을 추가 검토할 수 있습니다."));
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeCriteria(List<Map<String, Object>> rawCriteria, List<Map<String, Object>> scenarios) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (String criterion : CRITERIA) {
            Map<String, Object> rawRow = rawCriteria.stream()
                    .filter(item -> criterion.equals(text(item.get("name"), "")))
                    .findFirst()
                    .orElse(Map.of());

            List<Map<String, Object>> values = new ArrayList<>();
            String winnerKey = "";
            String winnerTitle = "";
            int maxScore = Integer.MIN_VALUE;

            for (Map<String, Object> scenario : scenarios) {
                Map<String, Integer> scoreMap = (Map<String, Integer>) scenario.get("scores");
                int score = clamp(scoreMap.getOrDefault(criterion, 0), 0, 100);

                values.add(Map.of(
                        "scenarioKey", text(scenario.get("key"), ""),
                        "scenarioTitle", text(scenario.get("title"), ""),
                        "score", score
                ));

                if (score > maxScore) {
                    maxScore = score;
                    winnerKey = text(scenario.get("key"), "");
                    winnerTitle = text(scenario.get("title"), "");
                }
            }

            String normalizedWinnerKey = text(rawRow.get("winnerScenarioKey"), winnerKey);
            String normalizedWinnerTitle = text(rawRow.get("winnerScenarioTitle"), winnerTitle);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", criterion);
            row.put("winnerScenarioKey", normalizedWinnerKey);
            row.put("winnerScenarioTitle", normalizedWinnerTitle);
            row.put("values", values);
            rows.add(row);
        }

        return rows;
    }

    private Map<String, Integer> normalizeScores(Map<String, Object> rawScores) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        for (String criterion : CRITERIA) {
            normalized.put(criterion, clamp(intNumber(rawScores.get(criterion), 60), 0, 100));
        }
        return normalized;
    }

    private int averageScore(Map<String, Integer> scores) {
        return (int) scores.values().stream().mapToInt(Integer::intValue).average().orElse(60);
    }

    private List<String> ensureMinItems(List<String> values, int minSize, String fallbackText) {
        List<String> normalized = new ArrayList<>(values);
        while (normalized.size() < minSize) {
            normalized.add(fallbackText);
        }
        return normalized;
    }

    private List<String> limitList(List<String> values, List<String> fallback, int maxSize) {
        List<String> source = values.isEmpty() ? fallback : values;
        return source.stream()
                .filter(item -> item != null && !item.isBlank())
                .limit(maxSize)
                .toList();
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
                .map(item -> item.toString().trim())
                .toList();
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::trimLongText)
                .filter(item -> item != null && !item.isBlank())
                .limit(10)
                .toList();
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "direct";
        }
        return switch (mode.trim()) {
            case "upload", "project", "direct" -> mode.trim();
            default -> "direct";
        };
    }

    private String normalizeText(String value, String fallback) {
        String normalized = trimLongText(value);
        return normalized == null || normalized.isBlank() ? fallback : normalized;
    }

    private String trimLongText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > 5000 ? normalized.substring(0, 5000) : normalized;
    }

    private String joinOrFallback(List<String> values, String fallback) {
        return values.isEmpty() ? fallback : String.join(", ", values);
    }

    private String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.toString().trim();
        return normalized.isEmpty() ? fallback : normalized;
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

    private String toJson(Map<String, Object> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize scenario result.", e);
        }
    }

    private Map<String, Object> fromJson(String resultJson) {
        try {
            return objectMapper.readValue(resultJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
