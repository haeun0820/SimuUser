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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    public AdminUserDetailService(
            AppUserRepository appUserRepository,
            ProjectRepository projectRepository,
            InquiryRepository inquiryRepository,
            AiSimulationResultRepository aiSimulationResultRepository,
            MarketAnalysisResultRepository marketAnalysisResultRepository,
            CostAnalysisResultRepository costAnalysisResultRepository,
            FeedbackAnalysisResultRepository feedbackAnalysisResultRepository,
            DocumentRepository documentRepository,
            ScenarioComparisonResultRepository scenarioComparisonResultRepository
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
                        .filter(value -> value != null)
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

        aiResults.forEach(result -> rows.add(new AnalysisRow("AI 시뮬레이션", result.getProject().getTitle(), summary(result.getOverallReaction()), result.getCreatedAt())));
        marketResults.forEach(result -> rows.add(new AnalysisRow("시장 분석", result.getProject().getTitle(), summary(result.getTitle()), result.getCreatedAt())));
        costResults.forEach(result -> rows.add(new AnalysisRow("수익성 분석", result.getProject().getTitle(), summary(result.getTitle()), result.getCreatedAt())));
        feedbackResults.forEach(result -> rows.add(new AnalysisRow("기획 피드백", result.getProject().getTitle(), summary(result.getSourceType()), result.getCreatedAt())));
        documents.forEach(result -> rows.add(new AnalysisRow("자동 문서화", result.getProject() == null ? "-" : result.getProject().getTitle(), summary(result.getTitle()), result.getCreatedAt())));
        scenarioResults.forEach(result -> rows.add(new AnalysisRow("시나리오 비교", result.getProject().getTitle(), summary(result.getCompareTitle()), result.getCreatedAt())));

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

    private String summary(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 60 ? normalized.substring(0, 60) + "..." : normalized;
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
            case "TEAM" -> "협업";
            case "COLLAB" -> "협업";
            case "COLLABORATION" -> "협업";
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
