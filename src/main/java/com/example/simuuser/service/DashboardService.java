package com.example.simuuser.service;

import com.example.simuuser.dto.DashboardAnalyticsResponse;
import com.example.simuuser.entity.AiSimulationResult;
import com.example.simuuser.entity.CostAnalysisResult;
import com.example.simuuser.entity.MarketAnalysisResult;
import com.example.simuuser.entity.Project;
import com.example.simuuser.repository.AiSimulationResultRepository;
import com.example.simuuser.repository.CostAnalysisResultRepository;
import com.example.simuuser.repository.MarketAnalysisResultRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final ProjectService projectService;
    private final AiSimulationResultRepository aiSimulationResultRepository;
    private final CostAnalysisResultRepository costAnalysisResultRepository;
    private final MarketAnalysisResultRepository marketAnalysisResultRepository;

    public DashboardService(
            ProjectService projectService,
            AiSimulationResultRepository aiSimulationResultRepository,
            CostAnalysisResultRepository costAnalysisResultRepository,
            MarketAnalysisResultRepository marketAnalysisResultRepository
    ) {
        this.projectService = projectService;
        this.aiSimulationResultRepository = aiSimulationResultRepository;
        this.costAnalysisResultRepository = costAnalysisResultRepository;
        this.marketAnalysisResultRepository = marketAnalysisResultRepository;
    }

    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse getAnalytics(Authentication authentication) {
        List<Project> projects = projectService.findVisibleProjects(authentication);
        if (projects.isEmpty()) {
            return emptyResponse();
        }

        List<AiSimulationResult> aiResults = aiSimulationResultRepository.findByProjectIn(projects);
        List<CostAnalysisResult> costResults = costAnalysisResultRepository.findByProjectIn(projects);
        List<MarketAnalysisResult> marketResults = marketAnalysisResultRepository.findByProjectIn(projects);

        List<LocalDateTime> allCreatedAt = new ArrayList<>();
        aiResults.forEach(result -> allCreatedAt.add(result.getCreatedAt()));
        costResults.forEach(result -> allCreatedAt.add(result.getCreatedAt()));
        marketResults.forEach(result -> allCreatedAt.add(result.getCreatedAt()));

        return new DashboardAnalyticsResponse(
                allCreatedAt.size(),
                buildToolUsage(aiResults.size(), costResults.size(), marketResults.size()),
                buildYearSeries(allCreatedAt),
                buildMonthSeries(allCreatedAt),
                buildWeekSeries(allCreatedAt)
        );
    }

    private DashboardAnalyticsResponse emptyResponse() {
        return new DashboardAnalyticsResponse(
                0,
                buildToolUsage(0, 0, 0),
                new DashboardAnalyticsResponse.ChartSeries(List.of(), List.of()),
                new DashboardAnalyticsResponse.ChartSeries(List.of(), List.of()),
                new DashboardAnalyticsResponse.ChartSeries(List.of(), List.of())
        );
    }

    private List<DashboardAnalyticsResponse.ToolUsageItem> buildToolUsage(int aiCount, int costCount, int marketCount) {
        return List.of(
                new DashboardAnalyticsResponse.ToolUsageItem("aiSimulation", "AI 시뮬레이션", aiCount, "#2563eb"),
                new DashboardAnalyticsResponse.ToolUsageItem("autoDocument", "자동 문서화", 0, "#f59e0b"),
                new DashboardAnalyticsResponse.ToolUsageItem("feedbackAi", "기획 피드백 AI", 0, "#f97316"),
                new DashboardAnalyticsResponse.ToolUsageItem("costAnalysis", "비용&수익성 분석", costCount, "#ec4899"),
                new DashboardAnalyticsResponse.ToolUsageItem("marketAnalysis", "시장&경쟁 분석", marketCount, "#8b5cf6"),
                new DashboardAnalyticsResponse.ToolUsageItem("scenarioCompare", "시나리오 비교", 0, "#ef4444")
        );
    }

    private DashboardAnalyticsResponse.ChartSeries buildYearSeries(List<LocalDateTime> createdAts) {
        int currentYear = LocalDate.now().getYear();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int year = currentYear - 4; year <= currentYear; year++) {
            counts.put(String.valueOf(year), 0);
        }

        for (LocalDateTime createdAt : createdAts) {
            String key = String.valueOf(createdAt.getYear());
            if (counts.containsKey(key)) {
                counts.put(key, counts.get(key) + 1);
            }
        }

        return new DashboardAnalyticsResponse.ChartSeries(List.copyOf(counts.keySet()), List.copyOf(counts.values()));
    }

    private DashboardAnalyticsResponse.ChartSeries buildMonthSeries(List<LocalDateTime> createdAts) {
        int currentYear = LocalDate.now().getYear();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            counts.put(month + "월", 0);
        }

        for (LocalDateTime createdAt : createdAts) {
            if (createdAt.getYear() == currentYear) {
                String key = createdAt.getMonthValue() + "월";
                counts.put(key, counts.get(key) + 1);
            }
        }

        return new DashboardAnalyticsResponse.ChartSeries(List.copyOf(counts.keySet()), List.copyOf(counts.values()));
    }

    private DashboardAnalyticsResponse.ChartSeries buildWeekSeries(List<LocalDateTime> createdAts) {
        LocalDate thisWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = thisWeekStart.minusWeeks(i);
            counts.put((4 - i) + "주", 0);
        }

        for (LocalDateTime createdAt : createdAts) {
            LocalDate date = createdAt.toLocalDate();
            for (int i = 3; i >= 0; i--) {
                LocalDate weekStart = thisWeekStart.minusWeeks(i);
                LocalDate weekEnd = weekStart.plusDays(6);
                if (!date.isBefore(weekStart) && !date.isAfter(weekEnd)) {
                    String key = (4 - i) + "주";
                    counts.put(key, counts.get(key) + 1);
                    break;
                }
            }
        }

        return new DashboardAnalyticsResponse.ChartSeries(List.copyOf(counts.keySet()), List.copyOf(counts.values()));
    }
}
