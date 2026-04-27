package com.example.simuuser.controller;

import com.example.simuuser.dto.ScenarioComparisonInput;
import com.example.simuuser.dto.ScenarioComparisonRequest;
import com.example.simuuser.dto.ScenarioComparisonResultResponse;
import com.example.simuuser.dto.ScenarioComparisonResultSaveRequest;
import com.example.simuuser.service.ScenarioComparisonService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ScenarioController {

    private final ScenarioComparisonService scenarioComparisonService;
    private final ObjectMapper objectMapper;

    public ScenarioController(
            ScenarioComparisonService scenarioComparisonService,
            ObjectMapper objectMapper
    ) {
        this.scenarioComparisonService = scenarioComparisonService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/scenario")
    public String scenarioPage() {
        return "scenario/scenario";
    }

    @GetMapping("/scenario/result")
    public String scenarioResultPage(
            @RequestParam(value = "resultId", required = false) Long resultId,
            Model model,
            Authentication authentication
    ) {
        if (resultId != null) {
            ScenarioComparisonResultResponse saved = scenarioComparisonService.findOne(resultId, authentication);
            model.addAttribute("scenarioResultData", saved.getResult());
            model.addAttribute("scenarioRequestData", buildRequestModel(saved.getProjectId(), saved.getCompareTitle(), List.of(), saved.getId()));
            return "scenario/scenario_result";
        }

        model.addAttribute("scenarioResultData", Map.of());
        model.addAttribute("scenarioRequestData", buildRequestModel(null, "", List.of(), null));
        return "scenario/scenario_result";
    }

    @PostMapping("/scenario/result")
    public String scenarioResultPost(
            @RequestParam("projectId") Long projectId,
            @RequestParam("compareTitle") String compareTitle,
            @RequestParam("scenarioPayload") String scenarioPayload,
            Model model,
            Authentication authentication
    ) {
        try {
            List<ScenarioComparisonInput> scenarios = objectMapper.readValue(scenarioPayload, new TypeReference<>() {});
            ScenarioComparisonRequest request = new ScenarioComparisonRequest();
            request.setProjectId(projectId);
            request.setCompareTitle(compareTitle);
            request.setScenarios(scenarios);

            Map<String, Object> result = scenarioComparisonService.generate(request, authentication);
            model.addAttribute("scenarioResultData", result);
            model.addAttribute("scenarioRequestData", buildRequestModel(projectId, compareTitle, scenarios, null));
            return "scenario/scenario_result";
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scenario comparison request is invalid.");
        }
    }

    @PostMapping("/scenario/results")
    @ResponseBody
    public Map<String, Object> saveScenarioResult(
            @RequestBody ScenarioComparisonResultSaveRequest request,
            Authentication authentication
    ) {
        try {
            return Map.of("saved", scenarioComparisonService.save(request, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/scenario/results/project/{projectId}")
    @ResponseBody
    public List<ScenarioComparisonResultResponse> findScenarioResultsByProject(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return scenarioComparisonService.findByProject(projectId, authentication);
    }

    private Map<String, Object> buildRequestModel(Long projectId, String compareTitle, List<ScenarioComparisonInput> scenarios, Long savedResultId) {
        Map<String, Object> request = new HashMap<>();
        request.put("projectId", projectId);
        request.put("compareTitle", compareTitle == null ? "" : compareTitle);
        request.put("scenarios", scenarios);
        request.put("savedResultId", savedResultId);
        return request;
    }
}
