package com.example.simuuser.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.simuuser.dto.DocumentRequest;
import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.entity.AiSimulationResult;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.CostAnalysisResult;
import com.example.simuuser.entity.Document;
import com.example.simuuser.entity.FeedbackAnalysisResult;
import com.example.simuuser.entity.MarketAnalysisResult;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ProjectTab;
import com.example.simuuser.entity.ScenarioComparisonResult;
import com.example.simuuser.repository.AiSimulationResultRepository;
import com.example.simuuser.repository.CostAnalysisResultRepository;
import com.example.simuuser.repository.DocumentRepository;
import com.example.simuuser.repository.FeedbackAnalysisResultRepository;
import com.example.simuuser.repository.MarketAnalysisResultRepository;
import com.example.simuuser.repository.ProjectMemberRepository;
import com.example.simuuser.repository.ProjectRepository;
import com.example.simuuser.repository.ProjectTabRepository;
import com.example.simuuser.repository.ScenarioComparisonResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiDocumentService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTabRepository projectTabRepository;
    private final DocumentRepository documentRepository;
    private final ProjectService projectService;
    private final LlmApiService llmApiService;
    private final AiSimulationResultRepository aiRepo;
    private final CostAnalysisResultRepository costRepo;
    private final FeedbackAnalysisResultRepository feedbackRepo;
    private final MarketAnalysisResultRepository marketRepo;
    private final ScenarioComparisonResultRepository scenarioRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public DocumentResponse generateDocument(DocumentRequest request, Authentication authentication) {
        if (request == null || request.getProjectId() == null) {
            throw new IllegalArgumentException("projectId is required.");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("title is required.");
        }

        Project project = findAccessibleProject(request.getProjectId(), authentication);
        ProjectTab projectTab = resolveProjectTab(request.getTabId(), project);
        String todayDate = LocalDateTime.now().format(DATE_FORMATTER);

        String prompt = isAnalysisDocumentType(request.getDocumentType())
                ? buildAnalysisPrompt(project, request, todayDate)
                : buildGeneralPrompt(project, request, todayDate);

        String generatedContent = llmApiService.generateText(prompt);

        Document newDoc = Document.builder()
                .project(project)
                .projectTab(projectTab)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getDocumentType())
                .content(generatedContent)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        documentRepository.save(newDoc);
        return new DocumentResponse(newDoc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByProjectId(Long projectId, Authentication authentication) {
        findAccessibleProject(projectId, authentication);

        return documentRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
                .map(DocumentResponse::new)
                .toList();
    }

    @Transactional
    public boolean toggleStarred(Long documentId, Authentication authentication) {
        AppUser currentUser = projectService.getCurrentUser(authentication);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found."));

        Project project = document.getProject();
        if (project == null && document.getProjectTab() != null) {
            project = document.getProjectTab().getProject();
        }
        if (project == null || !projectMemberRepository.existsByProjectAndUserAndStatus(project, currentUser, "ACCEPTED")) {
            throw new IllegalArgumentException("You do not have access to this document.");
        }

        return document.toggleStarred();
    }

    private String buildGeneralPrompt(Project project, DocumentRequest request, String todayDate) {
        return String.format(
                """
                Write a professional business document in Korean.
                Document type: %s
                Project name: %s
                Date: %s
                Project description: %s
                Title: %s
                Additional requirements: %s

                Use markdown with a clear introduction, body, and conclusion.
                Include the date at the top.
                """,
                text(request.getDocumentType(), "planning document"),
                text(project.getTitle(), "project"),
                todayDate,
                text(project.getDescription(), "No description"),
                text(request.getTitle(), "document"),
                text(request.getDescription(), "No additional requirements")
        );
    }

    private String buildAnalysisPrompt(Project project, DocumentRequest request, String todayDate) {
        StringBuilder dataContext = new StringBuilder();
        appendSection(dataContext, "AI user simulation", aiRepo.findByProject(project).stream()
                .map(item -> summarizeJson(item.getResultJson(), Map.of(
                        "avgPurchaseIntent", item.getAvgPurchaseIntent(),
                        "overallReaction", item.getOverallReaction()
                )))
                .toList());
        appendSection(dataContext, "Cost analysis", costRepo.findByProject(project).stream()
                .map(item -> summarizeJson(item.getResultJson(), Map.of(
                        "title", item.getTitle(),
                        "expectedUsers", item.getExpectedUsers(),
                        "pricePerUser", item.getPricePerUser()
                )))
                .toList());
        appendSection(dataContext, "Feedback analysis", feedbackRepo.findByProject(project).stream()
                .map(item -> summarizeJson(item.getResultJson(), Map.of(
                        "sourceType", item.getSourceType(),
                        "totalScore", item.getTotalScore()
                )))
                .toList());
        appendSection(dataContext, "Market analysis", marketRepo.findByProject(project).stream()
                .map(item -> summarizeJson(item.getResultJson(), Map.of(
                        "competitionLevel", item.getCompetitionLevel(),
                        "saturation", item.getSaturation(),
                        "competitorCount", item.getCompetitorCount()
                )))
                .toList());
        appendSection(dataContext, "Scenario comparison", scenarioRepo.findByProject(project).stream()
                .map(item -> summarizeJson(item.getResultJson(), Map.of(
                        "compareTitle", item.getCompareTitle(),
                        "recommendedScenarioTitle", item.getRecommendedScenarioTitle()
                )))
                .toList());

        return String.format(
                """
                Write a Korean analysis report by synthesizing the project context and the analysis data below.
                Project name: %s
                Date: %s
                Title: %s
                Additional requirements: %s

                === Analysis Data ===
                %s
                =====================

                Produce a markdown report with key insights, supporting evidence, and concrete action items.
                Include the date at the top.
                """,
                text(project.getTitle(), "project"),
                todayDate,
                text(request.getTitle(), "analysis report"),
                text(request.getDescription(), "No additional requirements"),
                dataContext
        );
    }

    private Project findAccessibleProject(Long projectId, Authentication authentication) {
        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found."));

        if (!projectMemberRepository.existsByProjectAndUserAndStatus(project, currentUser, "ACCEPTED")) {
            throw new IllegalArgumentException("You do not have access to this project.");
        }
        return project;
    }

    private ProjectTab resolveProjectTab(Long tabId, Project project) {
        if (tabId == null) {
            return null;
        }

        ProjectTab tab = projectTabRepository.findById(tabId)
                .orElseThrow(() -> new IllegalArgumentException("Tab not found."));
        if (!tab.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Tab does not belong to the selected project.");
        }
        return tab;
    }

    private void appendSection(StringBuilder builder, String title, List<String> lines) {
        builder.append('[').append(title).append(']').append('\n');
        if (lines.isEmpty()) {
            builder.append("- No results\n\n");
            return;
        }

        lines.forEach(line -> builder.append("- ").append(line).append('\n'));
        builder.append('\n');
    }

    private String summarizeJson(String json, Map<String, Object> fallback) {
        try {
            String fallbackJson = objectMapper.writeValueAsString(fallback);
            if (json == null || json.isBlank()) {
                return fallbackJson;
            }

            String compact = objectMapper.writeValueAsString(objectMapper.readTree(json));
            return compact.length() > 1200 ? compact.substring(0, 1200) + "..." : compact;
        } catch (JsonProcessingException e) {
            return fallback.toString();
        }
    }

    private String text(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private boolean isAnalysisDocumentType(String documentType) {
        if (documentType == null) {
            return false;
        }

        String normalized = documentType.trim().toLowerCase();
        return normalized.contains("analysis")
                || normalized.contains("result")
                || normalized.contains("분석");
    }
}
