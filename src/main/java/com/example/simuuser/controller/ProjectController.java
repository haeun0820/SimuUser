package com.example.simuuser.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.simuuser.dto.ProjectCreateRequest;
import com.example.simuuser.dto.ProjectResponse;
import com.example.simuuser.dto.TabRequest;
import com.example.simuuser.dto.UserSearchResponse;
import com.example.simuuser.entity.ProjectTab;
import com.example.simuuser.service.ProjectService;
import com.example.simuuser.service.ProjectTabService;

@Controller
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectTabService projectTabService; // [추가]

        // [수정] 생성자 주입
        public ProjectController(ProjectService projectService, ProjectTabService projectTabService) {
            this.projectService = projectService;
            this.projectTabService = projectTabService;
        }

    @GetMapping("/project/new")
    public String newProject() {
        return "project/new_project";
    }

    @GetMapping("/project/all")
    public String allProject() {
        return "project/all_project";
    }

    @ResponseBody
    @GetMapping("/api/projects")
    public List<ProjectResponse> myProjects(Authentication authentication) {
        return projectService.findMine(authentication);
    }

    @ResponseBody
    @PostMapping("/api/projects")
    public ResponseEntity<?> createProject(@RequestBody ProjectCreateRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.create(request, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/api/users/search")
    public List<UserSearchResponse> searchUsers(@RequestParam String email, Authentication authentication) {
        return projectService.searchInviteCandidates(email, authentication);
    }

    // ─── [여기서부터 탭 관련 API 추가] ───

    // 1. 탭 저장 API (POST)
    @ResponseBody
    @PostMapping("/api/projects/{projectId}/tabs")
    public ResponseEntity<?> addTab(@PathVariable Long projectId, @RequestBody TabRequest request) {
        try {
            ProjectTab savedTab = projectTabService.addTab(projectId, request);
            return ResponseEntity.ok(savedTab);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 2. 탭 조회 API (GET)
    @ResponseBody
    @GetMapping("/api/projects/{projectId}/tabs")
    public List<ProjectTab> getProjectTabs(@PathVariable Long projectId) {
        return projectTabService.getTabs(projectId);
    }
}
