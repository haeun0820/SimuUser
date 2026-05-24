package com.example.simuuser.service;

import com.example.simuuser.dto.AdminDashboardAnalyticsResponse;
import com.example.simuuser.entity.AiSimulationResult;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.CostAnalysisResult;
import com.example.simuuser.entity.Document;
import com.example.simuuser.entity.FeedbackAnalysisResult;
import com.example.simuuser.entity.MarketAnalysisResult;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ScenarioComparisonResult;
import com.example.simuuser.repository.AiSimulationResultRepository;
import com.example.simuuser.repository.AppUserRepository;
import com.example.simuuser.repository.CostAnalysisResultRepository;
import com.example.simuuser.repository.DocumentRepository;
import com.example.simuuser.repository.FeedbackAnalysisResultRepository;
import com.example.simuuser.repository.MarketAnalysisResultRepository;
import com.example.simuuser.repository.ProjectRepository;
import com.example.simuuser.repository.ScenarioComparisonResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private final AppUserRepository appUserRepository;
    private final ProjectRepository projectRepository;
    private final AiSimulationResultRepository aiSimulationResultRepository;
    private final MarketAnalysisResultRepository marketAnalysisResultRepository;
    private final CostAnalysisResultRepository costAnalysisResultRepository;
    private final FeedbackAnalysisResultRepository feedbackAnalysisResultRepository;
    private final DocumentRepository documentRepository;
    private final ScenarioComparisonResultRepository scenarioComparisonResultRepository;

    public AdminDashboardService(
            AppUserRepository appUserRepository,
            ProjectRepository projectRepository,
            AiSimulationResultRepository aiSimulationResultRepository,
            MarketAnalysisResultRepository marketAnalysisResultRepository,
            CostAnalysisResultRepository costAnalysisResultRepository,
            FeedbackAnalysisResultRepository feedbackAnalysisResultRepository,
            DocumentRepository documentRepository,
            ScenarioComparisonResultRepository scenarioComparisonResultRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.projectRepository = projectRepository;
        this.aiSimulationResultRepository = aiSimulationResultRepository;
        this.marketAnalysisResultRepository = marketAnalysisResultRepository;
        this.costAnalysisResultRepository = costAnalysisResultRepository;
        this.feedbackAnalysisResultRepository = feedbackAnalysisResultRepository;
        this.documentRepository = documentRepository;
        this.scenarioComparisonResultRepository = scenarioComparisonResultRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardAnalyticsResponse getAnalytics() {
        List<AppUser> users = appUserRepository.findAll();
        List<Project> projects = projectRepository.findAll();
        List<AiSimulationResult> simResults = aiSimulationResultRepository.findAll();
        List<MarketAnalysisResult> marketResults = marketAnalysisResultRepository.findAll();
        List<CostAnalysisResult> costResults = costAnalysisResultRepository.findAll();
        List<FeedbackAnalysisResult> feedbackResults = feedbackAnalysisResultRepository.findAll();
        List<Document> documents = documentRepository.findAll();
        List<ScenarioComparisonResult> scenarioResults = scenarioComparisonResultRepository.findAll();

        int totalAnalyses = simResults.size()
                + marketResults.size()
                + costResults.size()
                + feedbackResults.size()
                + documents.size()
                + scenarioResults.size();

        Map<String, List<LocalDateTime>> toolSeries = Map.of(
                "sim", extractCreatedAt(simResults),
                "market", extractCreatedAt(marketResults),
                "cost", extractCreatedAt(costResults),
                "feedback", extractCreatedAt(feedbackResults),
                "doc", extractCreatedAt(documents),
                "scenario", extractCreatedAt(scenarioResults)
        );

        return new AdminDashboardAnalyticsResponse(
                new AdminDashboardAnalyticsResponse.Summary(users.size(), projects.size(), totalAnalyses),
                buildYearPeriod(extractCreatedAt(users), extractCreatedAt(projects), toolSeries),
                buildMonthPeriod(extractCreatedAt(users), extractCreatedAt(projects), toolSeries),
                buildDayPeriod(extractCreatedAt(users), extractCreatedAt(projects), toolSeries)
        );
    }

    private AdminDashboardAnalyticsResponse.ChartPeriod buildYearPeriod(
            List<LocalDateTime> userCreatedAts,
            List<LocalDateTime> projectCreatedAts,
            Map<String, List<LocalDateTime>> toolSeries
    ) {
        int currentYear = LocalDate.now().getYear();
        List<Bucket> buckets = new ArrayList<>();
        for (int year = currentYear - 3; year <= currentYear; year++) {
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);
            buckets.add(new Bucket(String.valueOf(year), start, end));
        }
        return buildChartPeriod(buckets, userCreatedAts, projectCreatedAts, toolSeries);
    }

    private AdminDashboardAnalyticsResponse.ChartPeriod buildMonthPeriod(
            List<LocalDateTime> userCreatedAts,
            List<LocalDateTime> projectCreatedAts,
            Map<String, List<LocalDateTime>> toolSeries
    ) {
        YearMonth currentMonth = YearMonth.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월");
        List<Bucket> buckets = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            buckets.add(new Bucket(
                    month.format(formatter),
                    month.atDay(1),
                    month.atEndOfMonth()
            ));
        }
        return buildChartPeriod(buckets, userCreatedAts, projectCreatedAts, toolSeries);
    }

    private AdminDashboardAnalyticsResponse.ChartPeriod buildDayPeriod(
            List<LocalDateTime> userCreatedAts,
            List<LocalDateTime> projectCreatedAts,
            Map<String, List<LocalDateTime>> toolSeries
    ) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
        List<Bucket> buckets = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            buckets.add(new Bucket(date.format(formatter), date, date));
        }
        return buildChartPeriod(buckets, userCreatedAts, projectCreatedAts, toolSeries);
    }

    private AdminDashboardAnalyticsResponse.ChartPeriod buildChartPeriod(
            List<Bucket> buckets,
            List<LocalDateTime> userCreatedAts,
            List<LocalDateTime> projectCreatedAts,
            Map<String, List<LocalDateTime>> toolSeries
    ) {
        List<String> labels = new ArrayList<>();
        for (Bucket bucket : buckets) {
            labels.add(bucket.label());
        }

        List<Integer> users = countByBuckets(userCreatedAts, buckets);
        List<Integer> projects = countByBuckets(projectCreatedAts, buckets);
        List<Integer> sim = countByBuckets(toolSeries.getOrDefault("sim", List.of()), buckets);
        List<Integer> market = countByBuckets(toolSeries.getOrDefault("market", List.of()), buckets);
        List<Integer> cost = countByBuckets(toolSeries.getOrDefault("cost", List.of()), buckets);
        List<Integer> feedback = countByBuckets(toolSeries.getOrDefault("feedback", List.of()), buckets);
        List<Integer> doc = countByBuckets(toolSeries.getOrDefault("doc", List.of()), buckets);
        List<Integer> scenario = countByBuckets(toolSeries.getOrDefault("scenario", List.of()), buckets);

        List<Integer> analyses = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            analyses.add(sim.get(i) + market.get(i) + cost.get(i) + feedback.get(i) + doc.get(i) + scenario.get(i));
        }

        return new AdminDashboardAnalyticsResponse.ChartPeriod(
                labels,
                users,
                projects,
                analyses,
                sim,
                market,
                cost,
                feedback,
                doc,
                scenario
        );
    }

    private List<Integer> countByBuckets(List<LocalDateTime> createdAts, List<Bucket> buckets) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Bucket bucket : buckets) {
            counts.put(bucket.label(), 0);
        }

        for (LocalDateTime createdAt : createdAts) {
            if (createdAt == null) {
                continue;
            }

            LocalDate date = createdAt.toLocalDate();
            for (Bucket bucket : buckets) {
                if (!date.isBefore(bucket.start()) && !date.isAfter(bucket.end())) {
                    counts.put(bucket.label(), counts.get(bucket.label()) + 1);
                    break;
                }
            }
        }

        return new ArrayList<>(counts.values());
    }

    private List<LocalDateTime> extractCreatedAt(List<?> items) {
        List<LocalDateTime> values = new ArrayList<>();

        for (Object item : items) {
            LocalDateTime createdAt = null;

            if (item instanceof AppUser user) {
                createdAt = user.getCreatedAt();
            } else if (item instanceof Project project) {
                createdAt = project.getCreatedAt();
            } else if (item instanceof AiSimulationResult result) {
                createdAt = result.getCreatedAt();
            } else if (item instanceof MarketAnalysisResult result) {
                createdAt = result.getCreatedAt();
            } else if (item instanceof CostAnalysisResult result) {
                createdAt = result.getCreatedAt();
            } else if (item instanceof FeedbackAnalysisResult result) {
                createdAt = result.getCreatedAt();
            } else if (item instanceof Document document) {
                createdAt = document.getCreatedAt();
            } else if (item instanceof ScenarioComparisonResult result) {
                createdAt = result.getCreatedAt();
            }

            if (createdAt != null) {
                values.add(createdAt);
            }
        }

        return values;
    }

    private record Bucket(String label, LocalDate start, LocalDate end) {
    }
}
