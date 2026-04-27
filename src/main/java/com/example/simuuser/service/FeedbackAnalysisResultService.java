package com.example.simuuser.service;

import com.example.simuuser.dto.FeedbackAnalysisResultResponse;
import com.example.simuuser.dto.FeedbackAnalysisResultSaveRequest;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.FeedbackAnalysisResult;
import com.example.simuuser.entity.Project;
import com.example.simuuser.repository.FeedbackAnalysisResultRepository;
import com.example.simuuser.repository.ProjectMemberRepository;
import com.example.simuuser.repository.ProjectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class FeedbackAnalysisResultService {

    private final FeedbackAnalysisResultRepository feedbackAnalysisResultRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public FeedbackAnalysisResultService(
            FeedbackAnalysisResultRepository feedbackAnalysisResultRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectService projectService,
            ObjectMapper objectMapper
    ) {
        this.feedbackAnalysisResultRepository = feedbackAnalysisResultRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FeedbackAnalysisResultResponse save(FeedbackAnalysisResultSaveRequest request, Authentication authentication) {
        if (request == null || request.getProjectId() == null) {
            throw new IllegalArgumentException("projectId is required.");
        }
        if (request.getResult() == null || request.getResult().isEmpty()) {
            throw new IllegalArgumentException("result is required.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = findAccessibleProject(request.getProjectId(), currentUser);

        FeedbackAnalysisResult saved = feedbackAnalysisResultRepository.save(new FeedbackAnalysisResult(
                project,
                currentUser,
                normalize(request.getSourceType(), 20, "project"),
                trimLongText(request.getSourceContent()),
                clamp(number(request.getResult().get("totalScore"), 0), 0, 100),
                clamp(number(request.getResult().get("logicScore"), 0), 0, 100),
                clamp(number(request.getResult().get("completionScore"), 0), 0, 100),
                clamp(number(request.getResult().get("feasibilityScore"), 0), 0, 100),
                toJson(request.getResult())
        ));

        return new FeedbackAnalysisResultResponse(saved, request.getResult());
    }

    private Project findAccessibleProject(Long projectId, AppUser currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found."));

        if (!projectMemberRepository.existsByProjectAndUserAndStatus(project, currentUser, "ACCEPTED")) {
            throw new IllegalArgumentException("You do not have access to this project.");
        }

        return project;
    }

    private String toJson(Map<String, Object> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize feedback result.", e);
        }
    }

    @SuppressWarnings("unused")
    private Map<String, Object> fromJson(String resultJson) {
        try {
            return objectMapper.readValue(resultJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String normalize(String value, int maxLength, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String trimLongText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > 10000 ? normalized.substring(0, 10000) : normalized;
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
}
