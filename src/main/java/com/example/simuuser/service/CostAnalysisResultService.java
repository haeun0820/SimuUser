package com.example.simuuser.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.simuuser.dto.CostAnalysisResultResponse;
import com.example.simuuser.dto.CostAnalysisResultSaveRequest;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.CostAnalysisResult;
import com.example.simuuser.entity.Project;
import com.example.simuuser.repository.CostAnalysisResultRepository;
import com.example.simuuser.repository.ProjectMemberRepository;
import com.example.simuuser.repository.ProjectRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CostAnalysisResultService {

    private static final String GEMINI_FALLBACK_MODEL = "gemini-2.5-flash-lite";

    private final CostAnalysisResultRepository costAnalysisResultRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String geminiApiKey;
    private final String geminiModel;

    public CostAnalysisResultService(
            CostAnalysisResultRepository costAnalysisResultRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectService projectService,
            @Value("${gemini.api.key:}") String geminiApiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String geminiModel
    ) {
        this.costAnalysisResultRepository = costAnalysisResultRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
    }

    @Transactional
    public Map<String, Object> analyze(CostAnalysisResultSaveRequest request, Authentication authentication) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured.");
        }

        Project project = findAccessibleProject(requiredProjectId(request), projectService.getCurrentUser(authentication));
        Map<String, Object> baseline = buildFormulaResult(request, project);

        try {
            Map<String, Object> geminiResponse = callGemini(buildGeminiRequest(buildPrompt(request, project, baseline)));
            Map<String, Object> aiResult = parseJsonResult(extractText(geminiResponse));
            return normalizeAiResult(aiResult, baseline);
        } catch (RestClientException e) {
            throw new IllegalStateException(toGeminiErrorMessage(e), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("AI cost analysis failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public CostAnalysisResultResponse save(CostAnalysisResultSaveRequest request, Authentication authentication) {
        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = findAccessibleProject(requiredProjectId(request), currentUser);
        Map<String, Object> result = normalizeResult(request.getResult(), request, project);
        Map<String, Object> formData = buildFormData(request, project);

        CostAnalysisResult saved = costAnalysisResultRepository.save(new CostAnalysisResult(
                project,
                currentUser,
                "비용 & 수익성 분석",
                toJson(normalizeRevenueModels(request.getRevenueModels())),
                normalizedUsers(request.getExpectedUsers()),
                normalizedPrice(request.getPricePerUser()),
                toJson(formData),
                toJson(result)
        ));

        return new CostAnalysisResultResponse(saved, toJson(formData), toJson(result));
    }

    @Transactional(readOnly = true)
    public CostAnalysisResultResponse findOne(Long resultId, Authentication authentication) {
        if (resultId == null) {
            throw new IllegalArgumentException("resultId is required.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        CostAnalysisResult result = costAnalysisResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Cost analysis result not found."));
        findAccessibleProject(result.getProject().getId(), currentUser);

        return new CostAnalysisResultResponse(result, result.getFormJson(), result.getResultJson());
    }

    @Transactional(readOnly = true)
    public List<CostAnalysisResultResponse> findByProject(Long projectId, Authentication authentication) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is required.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = findAccessibleProject(projectId, currentUser);

        return costAnalysisResultRepository.findByProjectOrderByCreatedAtDesc(project).stream()
                .map(result -> new CostAnalysisResultResponse(result, result.getFormJson(), result.getResultJson()))
                .toList();
    }

    private Map<String, Object> buildFormulaResult(CostAnalysisResultSaveRequest request, Project project) {
        List<String> revenueModels = normalizeRevenueModels(request.getRevenueModels());
        int expectedUsers = normalizedUsers(request.getExpectedUsers());
        int pricePerUser = normalizedPrice(request.getPricePerUser());

        if (expectedUsers <= 0) {
            throw new IllegalArgumentException("expectedUsers is required.");
        }
        if (pricePerUser < 0) {
            throw new IllegalArgumentException("pricePerUser is required.");
        }
        if (revenueModels.isEmpty()) {
            throw new IllegalArgumentException("revenueModels is required.");
        }

        int modelCount = revenueModels.size();
        int maxMRR = (int) Math.round((expectedUsers * (double) pricePerUser) / 10000.0);

        int totalDevCost = 8000 + modelCount * 1000;
        int frontend = (int) Math.round(totalDevCost * 0.200 / 100.0) * 100;
        int backend = (int) Math.round(totalDevCost * 0.278 / 100.0) * 100;
        int aiml = (int) Math.round(totalDevCost * 0.389 / 100.0) * 100;
        int design = totalDevCost - frontend - backend - aiml;

        int serverCost = 200;
        int apiCost = Math.max(100, (int) Math.round(expectedUsers * 0.3 / 100.0) * 100);
        int maintenanceCost = 150;
        int marketingCost = 100;
        int totalMonthlyCost = serverCost + apiCost + maintenanceCost + marketingCost;

        int rev3 = Math.max(10, (int) Math.round(maxMRR * 0.09 / 10.0) * 10);
        int rev6 = Math.max(20, (int) Math.round(maxMRR * 0.27 / 10.0) * 10);
        int rev12 = Math.max(40, (int) Math.round(maxMRR * 0.55 / 10.0) * 10);

        double effectiveMRR = Math.max(maxMRR * 0.325, totalMonthlyCost * 0.5);
        int bepMonths = (int) Math.min(60, Math.max(6, Math.round(totalDevCost / effectiveMRR)));

        int bepUsers = pricePerUser > 0
                ? (int) Math.ceil((totalMonthlyCost * 10000.0) / (pricePerUser * 0.9))
                : 0;

        double profitRatio = maxMRR > 0 ? (maxMRR - totalMonthlyCost) / (double) totalMonthlyCost : -1.0;
        int bepPenalty = Math.max(0, bepMonths - 12) * 2;
        int score = Math.min(95, Math.max(20, (int) Math.round(65 + profitRatio * 20 - bepPenalty + modelCount * 3)));
        String grade = score >= 75 ? "우수" : score >= 50 ? "보통" : "주의";

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("projectId", project.getId());
        analysis.put("projectTitle", project.getTitle());
        analysis.put("grade", grade);
        analysis.put("score", score);
        analysis.put("bepMonths", bepMonths);
        analysis.put("bepUsers", bepUsers);
        analysis.put("devCosts", Map.of(
                "frontend", frontend,
                "backend", backend,
                "aiml", aiml,
                "design", design,
                "total", totalDevCost
        ));
        analysis.put("monthlyCosts", Map.of(
                "server", serverCost,
                "api", apiCost,
                "maintenance", maintenanceCost,
                "marketing", marketingCost,
                "total", totalMonthlyCost
        ));
        analysis.put("revenue", Map.of(
                "m3", rev3,
                "m6", rev6,
                "m12", rev12
        ));
        analysis.put("maxMRR", maxMRR);
        analysis.put("suggestions", buildFormulaSuggestions(revenueModels, bepMonths, maxMRR, totalMonthlyCost));
        return analysis;
    }

    private Map<String, Object> normalizeAiResult(Map<String, Object> aiResult, Map<String, Object> baseline) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    normalized.put("projectId",    baseline.get("projectId"));
    normalized.put("projectTitle", baseline.get("projectTitle"));
    normalized.put("grade",     normalizeGrade(text(aiResult.get("grade"), text(baseline.get("grade"), "보통"))));
    normalized.put("score",     clamp(number(aiResult.get("score"), number(baseline.get("score"), 50)), 0, 100));
    normalized.put("bepMonths", Math.max(1, number(aiResult.get("bepMonths"), number(baseline.get("bepMonths"), 12))));
    normalized.put("bepUsers",  Math.max(0, number(aiResult.get("bepUsers"),  number(baseline.get("bepUsers"), 0))));
    normalized.put("devCosts",      normalizeCostMap(aiResult.get("devCosts"),     mapValue(baseline.get("devCosts"))));
    normalized.put("monthlyCosts",  normalizeMonthlyCostMap(aiResult.get("monthlyCosts"), mapValue(baseline.get("monthlyCosts"))));
    normalized.put("revenue",       normalizeRevenueMap(aiResult.get("revenue"),   mapValue(baseline.get("revenue"))));
    normalized.put("maxMRR",    Math.max(0, number(aiResult.get("maxMRR"), number(baseline.get("maxMRR"), 0))));
 
    // ── 라벨 정규화 (핵심 추가 부분) ──
    normalized.put("devCostLabels",     normalizeLabels(aiResult.get("devCostLabels"),
            List.of("프론트엔드", "백엔드", "AI/ML", "디자인")));
    normalized.put("monthlyCostLabels", normalizeLabels(aiResult.get("monthlyCostLabels"),
            List.of("서버", "API", "유지보수", "마케팅")));
 
    List<String> suggestions = asStringList(aiResult.get("suggestions")).stream()
            .filter(value -> !value.isBlank())
            .limit(4)
            .toList();
    normalized.put("suggestions", suggestions.isEmpty() ? baseline.get("suggestions") : suggestions);
    return normalized;
}

private List<String> normalizeLabels(Object value, List<String> fallback) {
    if (!(value instanceof List<?> list) || list.size() != 4) {
        return fallback;
    }
    List<String> labels = list.stream()
            .map(item -> item == null ? "" : item.toString().trim())
            .toList();
    // 빈 항목이 하나라도 있으면 fallback 사용
    boolean anyBlank = labels.stream().anyMatch(String::isBlank);
    return anyBlank ? fallback : labels;
}

    private Map<String, Object> normalizeCostMap(Object value, Map<String, Object> fallback) {
        Map<String, Object> source = mapValue(value);
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("frontend", Math.max(0, number(source.get("frontend"), number(fallback.get("frontend"), 0))));
        normalized.put("backend", Math.max(0, number(source.get("backend"), number(fallback.get("backend"), 0))));
        normalized.put("aiml", Math.max(0, number(source.get("aiml"), number(fallback.get("aiml"), 0))));
        normalized.put("design", Math.max(0, number(source.get("design"), number(fallback.get("design"), 0))));
        normalized.put("total", Math.max(0, number(source.get("total"), number(fallback.get("total"), 0))));
        return normalized;
    }

    private Map<String, Object> normalizeMonthlyCostMap(Object value, Map<String, Object> fallback) {
        Map<String, Object> source = mapValue(value);
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("server", Math.max(0, number(source.get("server"), number(fallback.get("server"), 0))));
        normalized.put("api", Math.max(0, number(source.get("api"), number(fallback.get("api"), 0))));
        normalized.put("maintenance", Math.max(0, number(source.get("maintenance"), number(fallback.get("maintenance"), 0))));
        normalized.put("marketing", Math.max(0, number(source.get("marketing"), number(fallback.get("marketing"), 0))));
        normalized.put("total", Math.max(0, number(source.get("total"), number(fallback.get("total"), 0))));
        return normalized;
    }

    private Map<String, Object> normalizeRevenueMap(Object value, Map<String, Object> fallback) {
        Map<String, Object> source = mapValue(value);
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("m3", Math.max(0, number(source.get("m3"), number(fallback.get("m3"), 0))));
        normalized.put("m6", Math.max(0, number(source.get("m6"), number(fallback.get("m6"), 0))));
        normalized.put("m12", Math.max(0, number(source.get("m12"), number(fallback.get("m12"), 0))));
        return normalized;
    }

    private Map<String, Object> normalizeResult(Map<String, Object> result, CostAnalysisResultSaveRequest request, Project project) {
        if (result != null && !result.isEmpty()) {
            return normalizeAiResult(result, buildFormulaResult(request, project));
        }

        return buildFormulaResult(request, project);
    }

    private Map<String, Object> buildFormData(CostAnalysisResultSaveRequest request, Project project) {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("projectId", project.getId());
        formData.put("projectTitle", project.getTitle());
        formData.put("revenueModels", normalizeRevenueModels(request.getRevenueModels()));
        formData.put("expectedUsers", normalizedUsers(request.getExpectedUsers()));
        formData.put("pricePerUser", normalizedPrice(request.getPricePerUser()));
        return formData;
    }

    private List<String> buildFormulaSuggestions(List<String> models, int bepMonths, int maxMRR, int monthlyCost) {
        List<String> suggestions = new java.util.ArrayList<>();

        if (!models.contains("광고 수익") && !models.contains("프리미엄")) {
            suggestions.add("광고 수익이나 프리미엄 요금제를 함께 검토하면 초기에 현금 흐름을 만들기 좋습니다.");
        }
        if (bepMonths > 18) {
            suggestions.add("초기 고정비를 줄이기 위해 출시 범위를 줄이고 핵심 기능부터 먼저 검증하는 편이 좋습니다.");
        }
        suggestions.add("AI API 비용이나 외부 연동 비용이 커질 수 있으니 호출량 기준의 요금제와 캐시 전략을 함께 설계하세요.");
        suggestions.add("가격 정책, 리포트 범위, 고객 지원 수준을 분리해 수익원별 손익을 따로 추적하는 것이 유리합니다.");

        if (models.contains("구독") && maxMRR > monthlyCost * 1.5) {
            suggestions.add("구독 모델이 월 고정비를 충분히 넘기므로 장기 유지율과 이탈 방지 지표를 우선 관리하세요.");
        }

        return suggestions.stream().limit(4).toList();
    }

    private Map<String, Object> callGemini(Map<String, Object> requestBody) {
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

    private Map<String, Object> buildGeminiRequest(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.35,
                        "responseMimeType", "application/json"
                )
        );
    }

    private String buildPrompt(CostAnalysisResultSaveRequest request, Project project, Map<String, Object> baseline) {
    return """
            You are a startup finance analyst.
 
            Analyze the product and monetization assumptions below. Return only valid JSON. Do not include markdown or code fences.
 
            [Product]
            - Name: %s
            - Description: %s
            - Target user: %s
            - Industry: %s
 
            [Revenue assumptions]
            - Revenue models: %s
            - Expected users: %d
            - Price per user in KRW: %d
 
            [Formula baseline, unit is 만원 for cost and revenue values]
            %s
 
            Return this exact JSON shape:
            {
              "grade": "우수 | 보통 | 주의",
              "score": 0,
              "bepMonths": 0,
              "bepUsers": 0,
              "devCosts": {
                "frontend": 0,
                "backend": 0,
                "aiml": 0,
                "design": 0,
                "total": 0
              },
              "devCostLabels": ["항목명1", "항목명2", "항목명3", "항목명4"],
              "monthlyCosts": {
                "server": 0,
                "api": 0,
                "maintenance": 0,
                "marketing": 0,
                "total": 0
              },
              "monthlyCostLabels": ["항목명1", "항목명2", "항목명3", "항목명4"],
              "revenue": {
                "m3": 0,
                "m6": 0,
                "m12": 0
              },
              "maxMRR": 0,
              "suggestions": ["string"]
            }
 
            Rules:
            - Write suggestions in Korean.
            - All cost and revenue number fields must be integers in 만원.
            - score must be an integer from 0 to 100.
            - grade must be one of "우수", "보통", "주의".
            - Include exactly 4 concrete suggestions written in Korean.
            - Adjust the formula baseline using the product context, revenue model risk, expected user scale, and AI/API operating cost risk.
 
            [Label rules — 가장 중요]
            devCostLabels and monthlyCostLabels must be exactly 4 Korean strings.
            They map in order to: frontend / backend / aiml / design  and  server / api / maintenance / marketing.
            Replace the labels with industry-appropriate Korean terms based on the product description.
 
            Industry label examples (use similar style, NOT copy-paste):
            - 커머스/쇼핑:
                devCostLabels: ["쇼핑몰 UI/UX", "주문·결제 시스템", "추천 AI 엔진", "브랜드 디자인"]
                monthlyCostLabels: ["클라우드 서버", "PG사 API", "CS·운영 관리", "퍼포먼스 광고"]
            - 금융/핀테크:
                devCostLabels: ["앱 인터페이스", "거래·보안 서버", "리스크 분석 AI", "UX 디자인"]
                monthlyCostLabels: ["금융 인프라", "오픈뱅킹 API", "컴플라이언스 유지", "사용자 획득"]
            - 의료/헬스케어:
                devCostLabels: ["환자 앱 UI", "EMR 연동 서버", "진단 AI 모델", "의료 UX 설계"]
                monthlyCostLabels: ["HIPAA 서버", "의료 데이터 API", "인증·규제 유지", "병원 마케팅"]
            - 교육/에듀테크:
                devCostLabels: ["학습 플랫폼 UI", "강의 스트리밍 서버", "AI 튜터 엔진", "커리큘럼 디자인"]
                monthlyCostLabels: ["CDN·스트리밍", "결제·LMS API", "콘텐츠 업데이트", "학부모 마케팅"]
            - 콘텐츠/엔터테인먼트:
                devCostLabels: ["콘텐츠 뷰어 UI", "미디어 스트리밍 서버", "콘텐츠 추천 AI", "크리에이티브 디자인"]
                monthlyCostLabels: ["스트리밍 서버", "광고·CDN API", "저작권·라이선스", "SNS 마케팅"]
            - 소셜/커뮤니티:
                devCostLabels: ["소셜 피드 UI", "실시간 채팅 서버", "콘텐츠 필터 AI", "브랜드 디자인"]
                monthlyCostLabels: ["실시간 서버", "알림·소셜 API", "커뮤니티 운영", "바이럴 마케팅"]
            - 모빌리티/여행:
                devCostLabels: ["예약·지도 UI", "예약·매칭 서버", "경로 최적화 AI", "서비스 디자인"]
                monthlyCostLabels: ["지도·GPS 서버", "항공·숙박 API", "차량·파트너 관리", "여행 광고"]
            - 생산성/비즈니스:
                devCostLabels: ["대시보드 UI", "데이터 처리 서버", "자동화 AI 엔진", "UX/UI 설계"]
                monthlyCostLabels: ["SaaS 인프라", "연동 API", "고객 지원 유지", "B2B 영업·마케팅"]
 
            If the industry does not clearly match any example, create appropriate labels based on the product description.
            """.formatted(
            text(project.getTitle(), "Untitled product"),
            text(project.getDescription(), "No description"),
            text(project.getTargetUser(), "Not specified"),
            text(project.getIndustry(), "Not specified"),
            normalizeRevenueModels(request.getRevenueModels()),
            normalizedUsers(request.getExpectedUsers()),
            normalizedPrice(request.getPricePerUser()),
            toJson(baseline)
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

    private Project findAccessibleProject(Long projectId, AppUser currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found."));

        if (!projectMemberRepository.existsByProjectAndUserAndStatus(project, currentUser, "ACCEPTED")) {
            throw new IllegalArgumentException("You do not have access to this project.");
        }

        return project;
    }

    private Long requiredProjectId(CostAnalysisResultSaveRequest request) {
        if (request == null || request.getProjectId() == null) {
            throw new IllegalArgumentException("projectId is required.");
        }

        return request.getProjectId();
    }

    private List<String> normalizeRevenueModels(List<String> revenueModels) {
        if (revenueModels == null) {
            return List.of();
        }

        return revenueModels.stream()
                .map(this::normalizeText)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private int normalizedUsers(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private int normalizedPrice(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String normalizeGrade(String value) {
        if ("우수".equals(value) || "보통".equals(value) || "주의".equals(value)) {
            return value;
        }
        if ("Excellent".equalsIgnoreCase(value)) {
            return "우수";
        }
        if ("Caution".equalsIgnoreCase(value) || "Risky".equalsIgnoreCase(value)) {
            return "주의";
        }
        return "보통";
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        return Map.of();
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

    private String toJson(Object value) {
    try {
        return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) { 
        throw new IllegalArgumentException("Could not serialize cost analysis data.", e);
    }
    }
}
