package com.example.simuuser.service;

import com.example.simuuser.dto.MarketAnalysisResultResponse;
import com.example.simuuser.dto.MarketAnalysisResultSaveRequest;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.MarketAnalysisResult;
import com.example.simuuser.entity.Project;
import com.example.simuuser.repository.MarketAnalysisResultRepository;
import com.example.simuuser.repository.ProjectMemberRepository;
import com.example.simuuser.repository.ProjectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MarketAnalysisResultService {

    private final MarketAnalysisResultRepository marketAnalysisResultRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public MarketAnalysisResultService(
            MarketAnalysisResultRepository marketAnalysisResultRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectService projectService
    ) {
        this.marketAnalysisResultRepository = marketAnalysisResultRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public MarketAnalysisResultResponse save(MarketAnalysisResultSaveRequest request, Authentication authentication) {
        if (request == null || request.getProjectId() == null) {
            throw new IllegalArgumentException("projectId is required.");
        }
        if (request.getResult() == null || request.getResult().isEmpty()) {
            throw new IllegalArgumentException("result is required.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = findAccessibleProject(request.getProjectId(), currentUser);
        Map<String, Object> result = request.getResult();

        MarketAnalysisResult savedResult = marketAnalysisResultRepository.save(new MarketAnalysisResult(
                project,
                currentUser,
                "시장 & 경쟁 분석",
                normalize(text(result.get("competitionLevel"), "중간"), 30),
                clamp(number(result.get("saturation"), 0), 0, 100),
                Math.max(0, number(result.get("competitorCount"), 0)),
                toJson(result)
        ));

        return new MarketAnalysisResultResponse(savedResult, result);
    }

    @Transactional(readOnly = true)
    public MarketAnalysisResultResponse findOne(Long resultId, Authentication authentication) {
        if (resultId == null) {
            throw new IllegalArgumentException("resultId is required.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        MarketAnalysisResult result = marketAnalysisResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Market analysis result not found."));
        findAccessibleProject(result.getProject().getId(), currentUser);

        return new MarketAnalysisResultResponse(result, fromJson(result.getResultJson()));
    }

    @Transactional(readOnly = true)
    public List<MarketAnalysisResultResponse> findByProject(Long projectId, Authentication authentication) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is required.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = findAccessibleProject(projectId, currentUser);

        return marketAnalysisResultRepository.findByProjectOrderByCreatedAtDesc(project).stream()
                .map(result -> new MarketAnalysisResultResponse(result, fromJson(result.getResultJson())))
                .toList();
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
            throw new IllegalArgumentException("Could not serialize market analysis result.", e);
        }
    }

    private Map<String, Object> fromJson(String resultJson) {
        try {
            return objectMapper.readValue(resultJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String normalize(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }

        return text.length() > maxLength ? text.substring(0, maxLength) : text;
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
}
