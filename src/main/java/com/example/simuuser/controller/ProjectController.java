package com.example.simuuser.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.example.simuuser.service.NotificationService;
import com.example.simuuser.service.ProjectService;
import com.example.simuuser.service.ProjectTabService;

@Controller
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectTabService projectTabService;
    private final NotificationService notificationService;

    public ProjectController(
            ProjectService projectService,
            ProjectTabService projectTabService,
            NotificationService notificationService
    ) {
        this.projectService = projectService;
        this.projectTabService = projectTabService;
        this.notificationService = notificationService;
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

    @GetMapping("/project/edit/{projectId}")
    public String editProject(@PathVariable("projectId") Long projectId, Model model, Authentication authentication) {
        // 1. 기존 데이터 조회
        ProjectResponse project = projectService.getProjectDetail(projectId, authentication);
        // 2. HTML에 데이터 전달
        model.addAttribute("project", project);
        return "project/project_edit";
    }
    @ResponseBody
    @PatchMapping("/api/projects/{projectId}")
    public ResponseEntity<?> updateProject(
            @PathVariable("projectId") Long projectId,
            @RequestBody ProjectCreateRequest request,
            Authentication authentication) {
        try {
            projectService.update(projectId, request, authentication);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/api/users/search")
    public List<UserSearchResponse> searchUsers(@RequestParam("email") String email, Authentication authentication) { // ("email") 추가
        return projectService.searchInviteCandidates(email, authentication);
    }

    @ResponseBody
    @PostMapping("/api/projects/{projectId}/members/invite")
    public ResponseEntity<?> inviteMember(@PathVariable Long projectId, @RequestBody Map<String, String> body, Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.inviteMember(projectId, body.get("email"), body.get("role"), authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/api/projects/{projectId}/members")
    public ResponseEntity<?> projectMembers(@PathVariable Long projectId, Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.findProjectMembers(projectId, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @DeleteMapping("/api/projects/{projectId}/members/{memberId}")
    public ResponseEntity<?> removeMember(@PathVariable Long projectId, @PathVariable Long memberId, Authentication authentication) {
        try {
            projectService.removeProjectMember(projectId, memberId, authentication);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/api/project-invitations")
    public ResponseEntity<?> pendingInvitations(Authentication authentication) {
        return ResponseEntity.ok(projectService.findPendingInvites(authentication));
    }

    @ResponseBody
    @PostMapping("/api/project-invitations/{inviteId}/accept")
    public ResponseEntity<?> acceptInvitation(@PathVariable Long inviteId, Authentication authentication) {
        try {
            var response = projectService.acceptInvite(inviteId, authentication);
            notificationService.markProjectInviteHandled(inviteId, authentication);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @PostMapping("/api/project-invitations/{inviteId}/decline")
    public ResponseEntity<?> declineInvitation(@PathVariable Long inviteId, Authentication authentication) {
        try {
            var response = projectService.declineInvite(inviteId, authentication);
            notificationService.markProjectInviteHandled(inviteId, authentication);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

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

    @ResponseBody
    @GetMapping("/api/projects/{projectId}/tabs")
    public List<ProjectTab> getProjectTabs(@PathVariable Long projectId) {
        return projectTabService.getTabs(projectId);
    }

    @ResponseBody
    @PatchMapping("/api/projects/tabs/{tabId}")
    public ResponseEntity<?> updateTab(@PathVariable Long tabId, @RequestBody Map<String, String> body) {
        projectTabService.updateTabName(tabId, body.get("name"));
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @DeleteMapping("/api/projects/tabs/{tabId}")
    public ResponseEntity<?> deleteTab(@PathVariable Long tabId) {
        projectTabService.deleteTab(tabId);
        return ResponseEntity.ok().build();
    }
}
