package com.example.simuuser.controller;

import com.example.simuuser.dto.CostAnalysisResultResponse;
import com.example.simuuser.dto.CostAnalysisResultSaveRequest;
import com.example.simuuser.service.CostAnalysisResultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/cost")
public class Costcontroller {

    private final CostAnalysisResultService costAnalysisResultService;

    public Costcontroller(CostAnalysisResultService costAnalysisResultService) {
        this.costAnalysisResultService = costAnalysisResultService;
    }

    @GetMapping({"", "/", "/cost"})
    public String costPage() {
        return "cost/cost";
    }

    @GetMapping("/result")
    public String costResultPage() {
        return "cost/cost_result";
    }

    @PostMapping("/analyze")
    @ResponseBody
    public ResponseEntity<?> analyze(@RequestBody CostAnalysisResultSaveRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(costAnalysisResultService.analyze(request, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cost analysis failed: " + e.getMessage()));
        }
    }

    @PostMapping("/results")
    @ResponseBody
    public ResponseEntity<?> saveResult(@RequestBody CostAnalysisResultSaveRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(costAnalysisResultService.save(request, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cost analysis save failed: " + e.getMessage()));
        }
    }

    @GetMapping("/results/{resultId}")
    @ResponseBody
    public ResponseEntity<?> result(@PathVariable Long resultId, Authentication authentication) {
        try {
            return ResponseEntity.ok(costAnalysisResultService.findOne(resultId, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cost analysis load failed: " + e.getMessage()));
        }
    }

    @GetMapping("/results/project/{projectId}")
    @ResponseBody
    public ResponseEntity<?> projectResults(@PathVariable Long projectId, Authentication authentication) {
        try {
            return ResponseEntity.ok(costAnalysisResultService.findByProject(projectId, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cost analysis list load failed: " + e.getMessage()));
        }
    }
}
