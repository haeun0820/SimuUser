package com.example.simuuser.service;

import com.example.simuuser.dto.MyPageAnalysisHistoryResponse;
import com.example.simuuser.dto.MyPageAnalysisSummaryResponse;
import com.example.simuuser.entity.AiSimulationResult;
import com.example.simuuser.entity.CostAnalysisResult;
import com.example.simuuser.entity.FeedbackAnalysisResult;
import com.example.simuuser.entity.MarketAnalysisResult;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ScenarioComparisonResult;
import com.example.simuuser.repository.AiSimulationResultRepository;
import com.example.simuuser.repository.CostAnalysisResultRepository;
import com.example.simuuser.repository.FeedbackAnalysisResultRepository;
import com.example.simuuser.repository.MarketAnalysisResultRepository;
import com.example.simuuser.repository.ScenarioComparisonResultRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MyPageAnalysisService {

    private final ProjectService projectService;
    private final AiSimulationResultRepository aiSimulationResultRepository;
    private final MarketAnalysisResultRepository marketAnalysisResultRepository;
    private final CostAnalysisResultRepository costAnalysisResultRepository;
    private final FeedbackAnalysisResultRepository feedbackAnalysisResultRepository;
    private final ScenarioComparisonResultRepository scenarioComparisonResultRepository;

    public MyPageAnalysisService(
            ProjectService projectService,
            AiSimulationResultRepository aiSimulationResultRepository,
            MarketAnalysisResultRepository marketAnalysisResultRepository,
            CostAnalysisResultRepository costAnalysisResultRepository,
            FeedbackAnalysisResultRepository feedbackAnalysisResultRepository,
            ScenarioComparisonResultRepository scenarioComparisonResultRepository
    ) {
        this.projectService = projectService;
        this.aiSimulationResultRepository = aiSimulationResultRepository;
        this.marketAnalysisResultRepository = marketAnalysisResultRepository;
        this.costAnalysisResultRepository = costAnalysisResultRepository;
        this.feedbackAnalysisResultRepository = feedbackAnalysisResultRepository;
        this.scenarioComparisonResultRepository = scenarioComparisonResultRepository;
    }

    @Transactional(readOnly = true)
    public MyPageAnalysisSummaryResponse getSummary(Authentication authentication) {
        List<MyPageAnalysisHistoryResponse> histories = getHistories(authentication);
        return new MyPageAnalysisSummaryResponse(histories.size(), histories);
    }

    @Transactional(readOnly = true)
    public List<MyPageAnalysisHistoryResponse> getHistories(Authentication authentication) {
        List<Project> projects = projectService.findVisibleProjects(authentication);
        if (projects.isEmpty()) {
            return List.of();
        }

        List<MyPageAnalysisHistoryResponse> histories = new ArrayList<>();
        histories.addAll(aiSimulationResultRepository.findByProjectIn(projects).stream().map(this::fromAiSimulation).toList());
        histories.addAll(marketAnalysisResultRepository.findByProjectIn(projects).stream().map(this::fromMarketAnalysis).toList());
        histories.addAll(costAnalysisResultRepository.findByProjectIn(projects).stream().map(this::fromCostAnalysis).toList());
        histories.addAll(feedbackAnalysisResultRepository.findByProjectIn(projects).stream().map(this::fromFeedbackAnalysis).toList());
        histories.addAll(scenarioComparisonResultRepository.findByProjectIn(projects).stream().map(this::fromScenarioComparison).toList());

        return histories.stream()
                .sorted(Comparator.comparing(MyPageAnalysisHistoryResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    private MyPageAnalysisHistoryResponse fromAiSimulation(AiSimulationResult result) {
        return history(result.getId(), "aiuser", "AI \uC0AC\uC6A9\uC790 \uC2DC\uBBAC\uB808\uC774\uC158", result.getProject(), result.getCreatedAt(), result.isStarred(), "/aiuser/result?resultId=" + result.getId());
    }

    private MyPageAnalysisHistoryResponse fromMarketAnalysis(MarketAnalysisResult result) {
        return history(result.getId(), "market", text(result.getTitle(), "\uC2DC\uC7A5 & \uACBD\uC7C1 \uBD84\uC11D"), result.getProject(), result.getCreatedAt(), result.isStarred(), "/market/result?analysisId=" + result.getId());
    }

    private MyPageAnalysisHistoryResponse fromCostAnalysis(CostAnalysisResult result) {
        return history(result.getId(), "cost", text(result.getTitle(), "\uBE44\uC6A9 & \uC218\uC775\uC131 \uBD84\uC11D"), result.getProject(), result.getCreatedAt(), result.isStarred(), "/cost/result?analysisId=" + result.getId());
    }

    private MyPageAnalysisHistoryResponse fromFeedbackAnalysis(FeedbackAnalysisResult result) {
        return history(result.getId(), "feedback", "\uAE30\uD68D \uD53C\uB4DC\uBC31 \uBD84\uC11D", result.getProject(), result.getCreatedAt(), result.isStarred(), "/feedback/result?resultId=" + result.getId());
    }

    private MyPageAnalysisHistoryResponse fromScenarioComparison(ScenarioComparisonResult result) {
        return history(result.getId(), "scenario", text(result.getCompareTitle(), "\uC2DC\uB098\uB9AC\uC624 \uBE44\uAD50"), result.getProject(), result.getCreatedAt(), result.isStarred(), "/scenario/result?resultId=" + result.getId());
    }

    private MyPageAnalysisHistoryResponse history(Long id, String type, String title, Project project, LocalDateTime createdAt, boolean starred, String detailUrl) {
        return new MyPageAnalysisHistoryResponse(id, type, title, project.getId(), project.getTitle(), createdAt, starred, detailUrl);
    }

    private String text(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
