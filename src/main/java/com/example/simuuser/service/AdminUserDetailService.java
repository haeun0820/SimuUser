package com.example.simuuser.service;

import com.example.simuuser.dto.AdminUserDetailResponse;
import com.example.simuuser.entity.AiSimulationResult;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.CostAnalysisResult;
import com.example.simuuser.entity.Document;
import com.example.simuuser.entity.FeedbackAnalysisResult;
import com.example.simuuser.entity.Inquiry;
import com.example.simuuser.entity.MarketAnalysisResult;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ScenarioComparisonResult;
import com.example.simuuser.repository.AiSimulationResultRepository;
import com.example.simuuser.repository.AppUserRepository;
import com.example.simuuser.repository.CostAnalysisResultRepository;
import com.example.simuuser.repository.DocumentRepository;
import com.example.simuuser.repository.FeedbackAnalysisResultRepository;
import com.example.simuuser.repository.InquiryRepository;
import com.example.simuuser.repository.MarketAnalysisResultRepository;
import com.example.simuuser.repository.ProjectRepository;
import com.example.simuuser.repository.ScenarioComparisonResultRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AdminUserDetailService {

    private final AppUserRepository appUserRepository;
    private final ProjectRepository projectRepository;
    private final InquiryRepository inquiryRepository;
    private final AiSimulationResultRepository aiSimulationResultRepository;
    private final MarketAnalysisResultRepository marketAnalysisResultRepository;
    private final CostAnalysisResultRepository costAnalysisResultRepository;
    private final FeedbackAnalysisResultRepository feedbackAnalysisResultRepository;
    private final DocumentRepository documentRepository;
    private final ScenarioComparisonResultRepository scenarioComparisonResultRepository;
    private final ObjectMapper objectMapper;

    public AdminUserDetailService(
            AppUserRepository appUserRepository,
            ProjectRepository projectRepository,
            InquiryRepository inquiryRepository,
            AiSimulationResultRepository aiSimulationResultRepository,
            MarketAnalysisResultRepository marketAnalysisResultRepository,
            CostAnalysisResultRepository costAnalysisResultRepository,
            FeedbackAnalysisResultRepository feedbackAnalysisResultRepository,
            DocumentRepository documentRepository,
            ScenarioComparisonResultRepository scenarioComparisonResultRepository,
            ObjectMapper objectMapper
    ) {
        this.appUserRepository = appUserRepository;
        this.projectRepository = projectRepository;
        this.inquiryRepository = inquiryRepository;
        this.aiSimulationResultRepository = aiSimulationResultRepository;
        this.marketAnalysisResultRepository = marketAnalysisResultRepository;
        this.costAnalysisResultRepository = costAnalysisResultRepository;
        this.feedbackAnalysisResultRepository = feedbackAnalysisResultRepository;
        this.documentRepository = documentRepository;
        this.scenarioComparisonResultRepository = scenarioComparisonResultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        List<Project> projects = projectRepository.findByOwnerOrderByCreatedAtDesc(user);
        List<Inquiry> inquiries = inquiryRepository.findByUserOrderByCreatedAtDesc(user);

        List<AiSimulationResult> aiResults = aiSimulationResultRepository.findAll().stream()
                .filter(result -> result.getCreatedBy().getId().equals(userId))
                .toList();
        List<MarketAnalysisResult> marketResults = marketAnalysisResultRepository.findAll().stream()
                .filter(result -> result.getCreatedBy().getId().equals(userId))
                .toList();
        List<CostAnalysisResult> costResults = costAnalysisResultRepository.findAll().stream()
                .filter(result -> result.getCreatedBy().getId().equals(userId))
                .toList();
        List<FeedbackAnalysisResult> feedbackResults = feedbackAnalysisResultRepository.findAll().stream()
                .filter(result -> result.getCreatedBy().getId().equals(userId))
                .toList();
        List<ScenarioComparisonResult> scenarioResults = scenarioComparisonResultRepository.findAll().stream()
                .filter(result -> result.getCreatedBy().getId().equals(userId))
                .toList();

        List<Document> documents = new ArrayList<>();
        for (Project project : projects) {
            documents.addAll(documentRepository.findByProjectIdOrderByUpdatedAtDesc(project.getId()));
        }

        int totalAnalysisCount = aiResults.size()
                + marketResults.size()
                + costResults.size()
                + feedbackResults.size()
                + documents.size()
                + scenarioResults.size();

        List<AdminUserDetailResponse.RecentAnalysisItem> recentAnalyses = buildRecentAnalyses(
                aiResults, marketResults, costResults, feedbackResults, documents, scenarioResults
        );

        LocalDateTime lastActivityAt = latestDate(
                user.getCreatedAt(),
                projects.stream().map(Project::getCreatedAt).max(LocalDateTime::compareTo).orElse(null),
                inquiries.stream().map(Inquiry::getCreatedAt).max(LocalDateTime::compareTo).orElse(null),
                recentAnalyses.stream()
                        .map(AdminUserDetailResponse.RecentAnalysisItem::getCreatedAt)
                        .filter(value -> value != null && !value.isBlank())
                        .map(LocalDateTime::parse)
                        .max(LocalDateTime::compareTo)
                        .orElse(null)
        );

        return new AdminUserDetailResponse(
                user.getId(),
                user.getName(),
                user.getUserId(),
                user.getEmail(),
                stringify(user.getCreatedAt()),
                normalizeLoginMethod(user.getProvider()),
                loginMethodLabel(user.getProvider()),
                user.getRole(),
                roleLabel(user.getRole()),
                user.isProfileCompleted() ? "정상" : "추가 정보 필요",
                avatarText(user.getName()),
                projects.size(),
                totalAnalysisCount,
                inquiries.size(),
                stringify(lastActivityAt),
                projects.stream()
                        .map(project -> new AdminUserDetailResponse.ProjectItem(
                                project.getId(),
                                project.getTitle(),
                                project.getDescription(),
                                project.getType(),
                                projectTypeLabel(project.getType()),
                                stringify(project.getCreatedAt())
                        ))
                        .toList(),
                recentAnalyses,
                buildChart(aiResults, marketResults, costResults, feedbackResults, documents, scenarioResults)
        );
    }

    private AdminUserDetailResponse.ChartData buildChart(
            List<AiSimulationResult> aiResults,
            List<MarketAnalysisResult> marketResults,
            List<CostAnalysisResult> costResults,
            List<FeedbackAnalysisResult> feedbackResults,
            List<Document> documents,
            List<ScenarioComparisonResult> scenarioResults
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월");
        List<YearMonth> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            months.add(YearMonth.now().minusMonths(i));
        }

        List<String> labels = months.stream().map(month -> month.format(formatter)).toList();
        return new AdminUserDetailResponse.ChartData(
                labels,
                countByMonth(months, aiResults.stream().map(AiSimulationResult::getCreatedAt).toList()),
                countByMonth(months, marketResults.stream().map(MarketAnalysisResult::getCreatedAt).toList()),
                countByMonth(months, costResults.stream().map(CostAnalysisResult::getCreatedAt).toList()),
                countByMonth(months, feedbackResults.stream().map(FeedbackAnalysisResult::getCreatedAt).toList()),
                countByMonth(months, documents.stream().map(Document::getCreatedAt).toList()),
                countByMonth(months, scenarioResults.stream().map(ScenarioComparisonResult::getCreatedAt).toList())
        );
    }

    private List<Integer> countByMonth(List<YearMonth> months, List<LocalDateTime> values) {
        List<Integer> counts = new ArrayList<>();
        for (YearMonth month : months) {
            int count = 0;
            for (LocalDateTime value : values) {
                if (value != null && YearMonth.from(value).equals(month)) {
                    count++;
                }
            }
            counts.add(count);
        }
        return counts;
    }

    private List<AdminUserDetailResponse.RecentAnalysisItem> buildRecentAnalyses(
            List<AiSimulationResult> aiResults,
            List<MarketAnalysisResult> marketResults,
            List<CostAnalysisResult> costResults,
            List<FeedbackAnalysisResult> feedbackResults,
            List<Document> documents,
            List<ScenarioComparisonResult> scenarioResults
    ) {
        List<AnalysisRow> rows = new ArrayList<>();

        aiResults.forEach(result -> rows.add(new AnalysisRow(
                "AI 시뮬레이션",
                result.getProject().getTitle(),
                summarizeAiSimulation(result),
                result.getCreatedAt()
        )));
        marketResults.forEach(result -> rows.add(new AnalysisRow(
                "시장 분석",
                result.getProject().getTitle(),
                summarizeMarketAnalysis(result),
                result.getCreatedAt()
        )));
        costResults.forEach(result -> rows.add(new AnalysisRow(
                "수익성 분석",
                result.getProject().getTitle(),
                summarizeCostAnalysis(result),
                result.getCreatedAt()
        )));
        feedbackResults.forEach(result -> rows.add(new AnalysisRow(
                "기획 피드백",
                result.getProject().getTitle(),
                summarizeFeedbackAnalysis(result),
                result.getCreatedAt()
        )));
        documents.forEach(result -> rows.add(new AnalysisRow(
                "자동 문서화",
                result.getProject() == null ? "-" : result.getProject().getTitle(),
                summarizeDocument(result),
                result.getCreatedAt()
        )));
        scenarioResults.forEach(result -> rows.add(new AnalysisRow(
                "시나리오 비교",
                result.getProject().getTitle(),
                summarizeScenarioComparison(result),
                result.getCreatedAt()
        )));

        return rows.stream()
                .sorted(Comparator.comparing(AnalysisRow::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(row -> new AdminUserDetailResponse.RecentAnalysisItem(
                        row.toolName(),
                        row.projectTitle(),
                        row.summary(),
                        stringify(row.createdAt())
                ))
                .toList();
    }

    private String summarizeAiSimulation(AiSimulationResult result) {
        Map<String, Object> json = parseJson(result.getResultJson());
        return summary(
                firstNonBlank(
                        text(json.get("overallReaction")),
                        firstListItem(json, "keyInsights"),
                        firstListItem(json, "improvements"),
                        result.getOverallReaction()
                )
        );
    }

    private String summarizeMarketAnalysis(MarketAnalysisResult result) {
        Map<String, Object> json = parseJson(result.getResultJson());
        return summary(
                firstNonBlank(
                        text(json.get("opportunity")),
                        firstListItem(json, "differentiation"),
                        firstListItem(json, "risks"),
                        result.getTitle()
                )
        );
    }

    private String summarizeCostAnalysis(CostAnalysisResult result) {
        Map<String, Object> json = parseJson(result.getResultJson());
        String suggestion = firstListItem(json, "suggestions");
        if (suggestion != null) {
            return summary(suggestion);
        }

        String score = text(json.get("score"));
        String bepMonths = text(json.get("bepMonths"));
        if (score != null || bepMonths != null) {
            return summary(
                    ("손익 점수 " + defaultText(score, "-") + "점 / BEP " + defaultText(bepMonths, "-") + "개월")
            );
        }

        return summary(result.getTitle());
    }

    private String summarizeFeedbackAnalysis(FeedbackAnalysisResult result) {
        Map<String, Object> json = parseJson(result.getResultJson());
        return summary(
                firstNonBlank(
                        firstListItem(json, "improvements"),
                        firstListItem(json, "strengths"),
                        firstListItem(json, "risks"),
                        result.getSourceContent()
                )
        );
    }

    private String summarizeDocument(Document result) {
        return summary(
                firstNonBlank(
                        result.getContent(),
                        result.getDescription(),
                        result.getTitle()
                )
        );
    }

    private String summarizeScenarioComparison(ScenarioComparisonResult result) {
        Map<String, Object> json = parseJson(result.getResultJson());
        return summary(
                firstNonBlank(
                        text(json.get("finalSuggestion")),
                        text(json.get("recommendationReason")),
                        text(json.get("recommendedScenarioTitle")),
                        result.getRecommendedScenarioTitle(),
                        result.getCompareTitle()
                )
        );
    }

    private Map<String, Object> parseJson(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(resultJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String firstListItem(Map<String, Object> json, String key) {
        Object value = json.get(key);
        if (value instanceof List<?> list) {
            for (Object item : list) {
                String text = text(item);
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }

        String normalized = String.valueOf(value).replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String summary(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String normalizeLoginMethod(String provider) {
        if (provider == null || provider.isBlank()) {
            return "site";
        }
        String normalized = provider.trim().toLowerCase();
        return "local".equals(normalized) ? "site" : normalized;
    }

    private String loginMethodLabel(String provider) {
        return switch (normalizeLoginMethod(provider)) {
            case "google" -> "Google";
            case "naver" -> "Naver";
            case "kakao" -> "Kakao";
            case "apple" -> "Apple";
            default -> "일반 ID";
        };
    }

    private String roleLabel(String role) {
        return "ADMIN".equalsIgnoreCase(role) ? "관리자" : "사용자";
    }

    private String projectTypeLabel(String type) {
        if (type == null || type.isBlank()) {
            return "-";
        }
        return switch (type.trim().toUpperCase()) {
            case "TEAM", "COLLAB", "COLLABORATION" -> "협업";
            case "PERSONAL" -> "개인";
            default -> type;
        };
    }

    private String avatarText(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        return name.substring(0, 1);
    }

    @SafeVarargs
    private final LocalDateTime latestDate(LocalDateTime... values) {
        LocalDateTime latest = null;
        for (LocalDateTime value : values) {
            if (value != null && (latest == null || value.isAfter(latest))) {
                latest = value;
            }
        }
        return latest;
    }

    private record AnalysisRow(String toolName, String projectTitle, String summary, LocalDateTime createdAt) {
    }
}
