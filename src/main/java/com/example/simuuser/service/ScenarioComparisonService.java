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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScenarioComparisonService {

    private static final List<String> CRITERIA = List.of("사업성", "사용자 가치", "구현 가능성", "명확성", "시장 경쟁력");

    private final ScenarioComparisonResultRepository scenarioComparisonResultRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public ScenarioComparisonService(
            ScenarioComparisonResultRepository scenarioComparisonResultRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectService projectService,
            ObjectMapper objectMapper
    ) {
        this.scenarioComparisonResultRepository = scenarioComparisonResultRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
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

        findAccessibleProject(request.getProjectId(), projectService.getCurrentUser(authentication));

        List<Map<String, Object>> scenarios = new ArrayList<>();
        for (int i = 0; i < request.getScenarios().size(); i++) {
            scenarios.add(buildScenarioSummary(request.getScenarios().get(i), i));
        }

        Map<String, Object> recommended = scenarios.stream()
                .max(Comparator.comparingInt(item -> number(item.get("totalScore"))))
                .orElseThrow();

        List<Map<String, Object>> criteria = buildCriteria(scenarios);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("compareTitle", normalizeText(request.getCompareTitle(), "시나리오 비교"));
        result.put("recommendedScenarioKey", recommended.get("key"));
        result.put("recommendedScenarioTitle", recommended.get("title"));
        result.put("recommendationReason", buildRecommendationReason(recommended));
        result.put("scenarios", scenarios);
        result.put("criteria", criteria);
        result.put("finalSuggestion", buildFinalSuggestion(recommended));
        result.put("hybridSuggestion", buildHybridSuggestion(scenarios));
        return result;
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

    private Project findAccessibleProject(Long projectId, AppUser currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found."));

        if (!projectMemberRepository.existsByProjectAndUserAndStatus(project, currentUser, "ACCEPTED")) {
            throw new IllegalArgumentException("You do not have access to this project.");
        }

        return project;
    }

    private Map<String, Object> buildScenarioSummary(ScenarioComparisonInput input, int index) {
        String title = normalizeText(input.getTitle(), "시나리오 " + (index + 1));
        String mode = normalizeMode(input.getMode());
        String summary = trimLongText(input.getSummary());
        List<String> features = normalizeList(input.getFeatures());
        List<String> references = normalizeList(input.getReferences());

        int business = clamp(35 + features.size() * 9 + references.size() * 6 + lengthScore(title, 15), 0, 100);
        int userValue = clamp(30 + features.size() * 8 + lengthScore(summary, 80), 0, 100);
        int feasibility = clamp(35 + references.size() * 10 + modeBonus(mode) + Math.min(features.size() * 4, 16), 0, 100);
        int clarity = clamp(25 + lengthScore(title, 12) + lengthScore(summary, 120) + features.size() * 5, 0, 100);
        int market = clamp(30 + features.size() * 7 + references.size() * 5 + lengthScore(summary, 100), 0, 100);
        int total = (business + userValue + feasibility + clarity + market) / 5;

        List<String> pros = new ArrayList<>();
        List<String> cons = new ArrayList<>();
        if (!title.isBlank()) pros.add("제안의 방향과 초점이 비교적 명확합니다.");
        if (!features.isEmpty()) pros.add("핵심 기능이 정리되어 있어 비교 판단이 쉽습니다.");
        if (!references.isEmpty()) pros.add("참고 자료 또는 근거가 있어 실행 검토에 도움이 됩니다.");
        if (summary != null && summary.length() >= 80) pros.add("설명이 충분해 기획 의도를 파악하기 쉽습니다.");

        if (summary == null || summary.length() < 40) cons.add("시나리오 설명이 짧아 실제 차별점 파악이 어렵습니다.");
        if (features.size() < 2) cons.add("기능 정의가 부족해 비교 기준이 약합니다.");
        if (references.isEmpty()) cons.add("근거 자료가 없어 실현 가능성 검토가 제한됩니다.");
        if ("upload".equals(mode)) cons.add("업로드 파일 내용 자체는 비교에 직접 반영되지 않아 보완 설명이 필요합니다.");

        while (pros.size() < 2) {
            pros.add("핵심 방향은 잡혀 있으나 추가 상세화 여지가 있습니다.");
        }
        while (cons.size() < 2) {
            cons.add("사업성, 사용자 가치, 실행 계획에 대한 보강이 더 필요합니다.");
        }

        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        scoreMap.put(CRITERIA.get(0), business);
        scoreMap.put(CRITERIA.get(1), userValue);
        scoreMap.put(CRITERIA.get(2), feasibility);
        scoreMap.put(CRITERIA.get(3), clarity);
        scoreMap.put(CRITERIA.get(4), market);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", "S" + (index + 1));
        result.put("title", title);
        result.put("mode", mode);
        result.put("summary", summary == null ? "" : summary);
        result.put("features", features);
        result.put("references", references);
        result.put("totalScore", total);
        result.put("scores", scoreMap);
        result.put("pros", pros);
        result.put("cons", cons);
        return result;
    }

    private List<Map<String, Object>> buildCriteria(List<Map<String, Object>> scenarios) {
        List<Map<String, Object>> criteria = new ArrayList<>();
        for (String criterion : CRITERIA) {
            List<Map<String, Object>> values = new ArrayList<>();
            Map<String, Object> winner = null;
            int max = Integer.MIN_VALUE;

            for (Map<String, Object> scenario : scenarios) {
                @SuppressWarnings("unchecked")
                Map<String, Integer> scores = (Map<String, Integer>) scenario.get("scores");
                int value = scores.getOrDefault(criterion, 0);
                Map<String, Object> item = new HashMap<>();
                item.put("scenarioKey", scenario.get("key"));
                item.put("scenarioTitle", scenario.get("title"));
                item.put("score", value);
                values.add(item);

                if (value > max) {
                    max = value;
                    winner = scenario;
                }
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", criterion);
            row.put("winnerScenarioKey", winner == null ? "" : winner.get("key"));
            row.put("winnerScenarioTitle", winner == null ? "" : winner.get("title"));
            row.put("values", values);
            criteria.add(row);
        }
        return criteria;
    }

    private String buildRecommendationReason(Map<String, Object> recommended) {
        return "%s이(가) 전체 점수와 핵심 비교 지표에서 가장 균형이 좋아 우선 검토 대상으로 적합합니다."
                .formatted(String.valueOf(recommended.get("title")));
    }

    private String buildFinalSuggestion(Map<String, Object> recommended) {
        return "%s을(를) 기준안으로 삼고, 부족한 근거 자료와 세부 실행 계획만 보강해서 다음 단계로 넘기는 편이 효율적입니다."
                .formatted(String.valueOf(recommended.get("title")));
    }

    private String buildHybridSuggestion(List<Map<String, Object>> scenarios) {
        if (scenarios.size() < 2) {
            return "현재 비교안 기준으로 세부안 정리를 진행하는 것이 적절합니다.";
        }

        Map<String, Object> first = scenarios.get(0);
        Map<String, Object> second = scenarios.stream()
                .sorted((a, b) -> Integer.compare(number(b.get("totalScore")), number(a.get("totalScore"))))
                .skip(1)
                .findFirst()
                .orElse(first);

        return "%s의 강한 방향성과 %s의 보완 포인트를 결합하면 더 현실적인 하이브리드 안을 만들 수 있습니다."
                .formatted(String.valueOf(first.get("title")), String.valueOf(second.get("title")));
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

    private int lengthScore(String value, int maxUsefulLength) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Math.min(value.length(), maxUsefulLength) * 30 / maxUsefulLength;
    }

    private int modeBonus(String mode) {
        return switch (mode) {
            case "project" -> 12;
            case "direct" -> 8;
            case "upload" -> 4;
            default -> 0;
        };
    }

    private int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
