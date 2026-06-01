package com.example.simuuser.service;

import com.example.simuuser.dto.AiPromptRequest;
import com.example.simuuser.dto.AiPromptResponse;
import com.example.simuuser.entity.AiPrompt;
import com.example.simuuser.repository.AiPromptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiPromptService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "simulation",
            "market",
            "profit",
            "feedback",
            "scenario",
            "document"
    );
    private static final Map<String, List<String>> REQUIRED_PLACEHOLDERS = Map.of(
            "simulation", List.of("serviceIdea", "description", "targetUser", "industry", "personaCount", "gender", "ages", "job"),
            "market", List.of("projectTitle", "projectDescription", "targetUser", "industry"),
            "profit", List.of("projectTitle", "projectDescription", "targetUser", "industry", "expectedUsers", "pricePerUser", "revenueModels"),
            "feedback", List.of("projectTitle", "projectDescription", "targetUser", "industry", "planText"),
            "scenario", List.of("projectTitle", "projectDescription", "compareTitle", "scenarioSummaries"),
            "document", List.of("projectTitle", "projectDescription", "targetUser", "industry", "documentType", "title", "description", "date")
    );

    private final AiPromptRepository aiPromptRepository;

    public AiPromptService(AiPromptRepository aiPromptRepository) {
        this.aiPromptRepository = aiPromptRepository;
    }

    @Transactional(readOnly = true)
    public List<AiPromptResponse> find(String category) {
        String normalizedCategory = normalizeCategory(category, true);
        List<AiPrompt> prompts = normalizedCategory == null
                ? aiPromptRepository.findAllByOrderByCreatedAtDesc()
                : aiPromptRepository.findByCategoryOrderByCreatedAtDesc(normalizedCategory);
        return prompts.stream().map(AiPromptResponse::new).toList();
    }

    @Transactional
    public AiPromptResponse create(AiPromptRequest request) {
        String category = validateAndNormalize(request);

        AiPrompt prompt = new AiPrompt(
                request.getName().trim(),
                category,
                textOrDefault(request.getModel(), "gemini-2.5-flash"),
                request.getSystemPrompt().trim(),
                request.getUserPromptTemplate().trim()
        );

        return new AiPromptResponse(aiPromptRepository.save(prompt));
    }

    @Transactional
    public AiPromptResponse update(Long promptId, AiPromptRequest request) {
        String category = validateAndNormalize(request);

        AiPrompt prompt = findPrompt(promptId);
        prompt.update(
                request.getName().trim(),
                category,
                textOrDefault(request.getModel(), "gemini-2.5-flash"),
                request.getSystemPrompt().trim(),
                request.getUserPromptTemplate().trim()
        );

        return new AiPromptResponse(prompt);
    }

    @Transactional
    public void delete(Long promptId) {
        aiPromptRepository.delete(findPrompt(promptId));
    }

    @Transactional(readOnly = true)
    public String renderPrompt(Long promptId, Map<String, Object> values) {
        if (promptId == null) {
            return null;
        }

        AiPrompt prompt = findPrompt(promptId);
        return buildRuntimePrompt(prompt, values);
    }

    @Transactional(readOnly = true)
    public String resolveModel(Long promptId) {
        if (promptId == null) {
            return null;
        }

        return textOrDefault(findPrompt(promptId).getModel(), null);
    }

    private AiPrompt findPrompt(Long promptId) {
        if (promptId == null) {
            throw new IllegalArgumentException("프롬프트를 찾을 수 없습니다.");
        }

        return aiPromptRepository.findById(promptId)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다."));
    }

    private String buildRuntimePrompt(AiPrompt prompt, Map<String, Object> values) {
        StringBuilder runtimePrompt = new StringBuilder();
        runtimePrompt.append(prompt.getSystemPrompt().trim())
                .append("\n\n")
                .append(applyTemplate(prompt.getUserPromptTemplate(), values).trim());

        String contract = runtimeContract(prompt.getCategory());
        if (!contract.isBlank()) {
            runtimePrompt.append("\n\n").append(contract);
        }

        return runtimePrompt.toString();
    }

    private String applyTemplate(String template, Map<String, Object> values) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            rendered = rendered.replace("{{" + entry.getKey() + "}}", value);
        }
        return rendered;
    }

    private String validateAndNormalize(AiPromptRequest request) {
        if (request == null || isBlank(request.getName())) {
            throw new IllegalArgumentException("프롬프트 이름을 입력해주세요.");
        }
        if (isBlank(request.getCategory())) {
            throw new IllegalArgumentException("카테고리를 선택해주세요.");
        }
        if (isBlank(request.getSystemPrompt())) {
            throw new IllegalArgumentException("시스템 프롬프트를 입력해주세요.");
        }
        if (isBlank(request.getUserPromptTemplate())) {
            throw new IllegalArgumentException("사용자 프롬프트 템플릿을 입력해주세요.");
        }

        String category = normalizeCategory(request.getCategory(), false);
        validateRequiredPlaceholders(category, request.getUserPromptTemplate());
        return category;
    }

    private void validateRequiredPlaceholders(String category, String template) {
        List<String> required = REQUIRED_PLACEHOLDERS.getOrDefault(category, List.of());
        List<String> missing = required.stream()
                .filter(placeholder -> !template.contains("{{" + placeholder + "}}"))
                .toList();

        if (!missing.isEmpty()) {
            String placeholders = missing.stream()
                    .map(name -> "{{" + name + "}}")
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("필수 변수 누락: " + placeholders);
        }
    }

    private String runtimeContract(String category) {
        return switch (category) {
            case "simulation" -> """
                    반드시 JSON 객체 하나만 반환하세요. 설명 문장, 마크다운, 코드블록은 넣지 마세요.
                    최상위는 배열이 아니라 객체 1개여야 합니다.
                    아래 키를 모두 포함하세요:
                    - avgPurchaseIntent
                    - overallReaction
                    - personas
                    - keyInsights
                    - improvements

                    personas는 반드시 배열이어야 하며, 각 원소는 아래 키를 모두 포함해야 합니다:
                    - name
                    - age
                    - gender
                    - job
                    - consumerType
                    - purchaseScore
                    - positiveReactions
                    - negativeReactions
                    - churnPoints

                    규칙:
                    - personas 길이는 반드시 입력된 personaCount와 같아야 합니다.
                    - avgPurchaseIntent와 purchaseScore는 0~100 정수여야 합니다.
                    - 모든 텍스트는 한국어로 작성하세요.
                    """;
            case "market" -> """
                    반드시 JSON 객체 하나만 반환하세요. 설명 문장, 마크다운, 코드블록은 넣지 마세요.
                    아래 키를 모두 포함하세요:
                    - competitionLevel
                    - saturation
                    - competitorCount
                    - marketSize
                    - keywords
                    - competitors
                    - differentiation
                    - risks
                    - opportunity

                    marketSize는 tam, sam, som 객체를 포함해야 합니다.
                    competitors는 배열이어야 하며 각 원소는 name, tags, strength, weakness를 포함해야 합니다.

                    규칙:
                    - competitionLevel은 "낮음", "중간", "높음" 중 하나여야 합니다.
                    - saturation은 0~100 정수여야 합니다.
                    - competitorCount는 competitors 배열 길이와 같아야 합니다.
                    - keywords는 3~5개, competitors는 3~5개로 작성하세요.
                    - differentiation과 risks는 각각 3~5개로 작성하세요.
                    - 모든 텍스트는 한국어로 작성하세요.
                    """;
            case "profit" -> """
                    반드시 JSON 객체 하나만 반환하세요. 설명 문장, 마크다운, 코드블록은 넣지 마세요.
                    아래 키를 모두 포함하세요:
                    - grade
                    - score
                    - bepMonths
                    - bepUsers
                    - devCosts
                    - devCostLabels
                    - monthlyCosts
                    - monthlyCostLabels
                    - revenue
                    - maxMRR
                    - suggestions

                    devCosts는 frontend, backend, aiml, design, total을 포함해야 합니다.
                    monthlyCosts는 server, api, maintenance, marketing, total을 포함해야 합니다.
                    revenue는 m3, m6, m12를 포함해야 합니다.

                    규칙:
                    - grade는 "우수", "보통", "주의" 중 하나여야 합니다.
                    - score는 0~100 정수여야 합니다.
                    - 비용과 매출 숫자 필드는 모두 정수여야 합니다.
                    - devCostLabels와 monthlyCostLabels는 각각 정확히 4개의 한국어 문자열이어야 합니다.
                    - suggestions는 정확히 4개 작성하세요.
                    - 모든 텍스트는 한국어로 작성하세요.
                    """;
            case "feedback" -> """
                    반드시 JSON 객체 하나만 반환하세요. 설명 문장, 마크다운, 코드블록은 넣지 마세요.
                    아래 키를 모두 포함하세요:
                    - totalScore
                    - logicScore
                    - completionScore
                    - feasibilityScore
                    - strengths
                    - weaknesses
                    - missingElements
                    - improvements
                    - risks
                    - scenarios

                    scenarios는 best, normal, worst를 포함해야 합니다.

                    규칙:
                    - 모든 점수는 0~100 정수여야 합니다.
                    - strengths는 2~5개, weaknesses는 3~6개로 작성하세요.
                    - missingElements는 4~10개로 작성하세요.
                    - improvements는 4~6개, risks는 3~5개로 작성하세요.
                    - 모든 텍스트는 한국어로 작성하세요.
                    """;
            case "scenario" -> """
                    반드시 JSON 객체 하나만 반환하세요. 설명 문장, 마크다운, 코드블록은 절대 포함하지 마세요.
                    아래 키를 모두 포함하세요.
                    - compareTitle
                    - recommendedScenarioKey
                    - recommendedScenarioTitle
                    - recommendationReason
                    - scenarios
                    - criteria
                    - finalSuggestion
                    - hybridSuggestion

                    scenarios는 배열이어야 하며, 각 항목은 아래 키를 모두 포함해야 합니다.
                    - key
                    - title
                    - mode
                    - summary
                    - features
                    - references
                    - totalScore
                    - scores
                    - pros
                    - cons

                    scores는 객체여야 하며 아래 5개 기준을 모두 포함해야 합니다.
                    - 사업성
                    - 사용자 가치
                    - 구현 가능성
                    - 명확성
                    - 시장 경쟁력

                    criteria는 배열이어야 하며, 각 항목은 아래 키를 모두 포함해야 합니다.
                    - name
                    - winnerScenarioKey
                    - winnerScenarioTitle
                    - values

                    values는 배열이어야 하며, 각 항목은 아래 키를 포함해야 합니다.
                    - scenarioKey
                    - scenarioTitle
                    - score

                    규칙:
                    - recommendedScenarioKey는 scenarios 중 하나의 key와 반드시 일치해야 합니다.
                    - recommendedScenarioTitle은 recommendedScenarioKey가 가리키는 title과 같아야 합니다.
                    - scenarios 길이는 입력된 시나리오 개수와 같아야 합니다.
                    - totalScore와 모든 score 값은 0~100 정수여야 합니다.
                    - pros와 cons는 각각 2~4개로 작성하세요.
                    - criteria는 반드시 5개 기준을 모두 포함하세요.
                    - 각 criteria.values 길이는 scenarios 길이와 같아야 합니다.
                    - 모든 텍스트는 한국어로 작성하세요.
                    """;
            case "document" -> """
                    모든 출력은 한국어 문서 본문으로 작성하세요.
                    JSON, 마크다운 코드블록, 설명용 메타 문장은 출력하지 마세요.
                    바로 문서에 붙여 넣을 수 있는 완성형 문장과 문단으로 작성하세요.
                    """;
            default -> "";
        };
    }

    private String textOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeCategory(String category, boolean allowAll) {
        if (isBlank(category)) {
            return null;
        }

        String normalized = category.trim().toLowerCase();
        if (allowAll && "all".equals(normalized)) {
            return null;
        }

        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 카테고리입니다.");
        }

        return normalized;
    }
}
