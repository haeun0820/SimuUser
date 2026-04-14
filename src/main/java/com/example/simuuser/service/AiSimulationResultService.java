package com.example.simuuser.service;

import com.example.simuuser.dto.AiSimulationResultResponse;
import com.example.simuuser.dto.AiSimulationResultSaveRequest;
import com.example.simuuser.entity.AiSimulationResult;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Project;
import com.example.simuuser.repository.AiSimulationResultRepository;
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
public class AiSimulationResultService {

    private final AiSimulationResultRepository aiSimulationResultRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public AiSimulationResultService(
            AiSimulationResultRepository aiSimulationResultRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectService projectService,
            ObjectMapper objectMapper
    ) {
        this.aiSimulationResultRepository = aiSimulationResultRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiSimulationResultResponse save(AiSimulationResultSaveRequest request, Authentication authentication) {
        if (request == null || request.getProjectId() == null) {
            throw new IllegalArgumentException("projectId is required.");
        }
        if (request.getResult() == null || request.getResult().isEmpty()) {
            throw new IllegalArgumentException("result is required.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = findAccessibleProject(request.getProjectId(), currentUser);
        String resultJson = toJson(request.getResult());

        AiSimulationResult savedResult = aiSimulationResultRepository.save(new AiSimulationResult(
                project,
                currentUser,
                clamp(number(request.getPersonaCount(), 0), 0, 10),
                normalize(request.getGender(), 30),
                normalize(request.getAges(), 100),
                normalize(request.getJob(), 100),
                clamp(number(request.getResult().get("avgPurchaseIntent"), 0), 0, 100),
                normalize(asText(request.getResult().get("overallReaction")), 10_000),
                resultJson
        ));

        return new AiSimulationResultResponse(savedResult, request.getResult());
    }

    @Transactional(readOnly = true)
    public List<AiSimulationResultResponse> findByProject(Long projectId, Authentication authentication) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is required.");
        }

        AppUser currentUser = projectService.getCurrentUser(authentication);
        Project project = findAccessibleProject(projectId, currentUser);

        return aiSimulationResultRepository.findByProjectOrderByCreatedAtDesc(project).stream()
                .map(result -> new AiSimulationResultResponse(result, fromJson(result.getResultJson())))
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
            throw new IllegalArgumentException("Could not serialize simulation result.", e);
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

    private String asText(Object value) {
        return value == null ? null : value.toString();
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
