package com.example.simuuser.controller;

import com.example.simuuser.dto.ProjectCreateRequest;
import com.example.simuuser.dto.ProjectResponse;
import com.example.simuuser.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
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
}
