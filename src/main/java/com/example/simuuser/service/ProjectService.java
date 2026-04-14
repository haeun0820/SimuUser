package com.example.simuuser.service;

import com.example.simuuser.dto.ProjectCreateRequest;
import com.example.simuuser.dto.ProjectResponse;
import com.example.simuuser.dto.UserSearchResponse;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ProjectMember;
import com.example.simuuser.repository.AppUserRepository;
import com.example.simuuser.repository.ProjectMemberRepository;
import com.example.simuuser.repository.ProjectRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AppUserRepository appUserRepository;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository, AppUserRepository appUserRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request, Authentication authentication) {
        AppUser owner = getCurrentUser(authentication);
        validate(request);

        Project project = new Project(
                owner,
                request.getTitle().trim(),
                normalize(request.getDescription()),
                normalize(request.getTargetUser()),
                normalize(request.getIndustry()),
                normalizeType(request.getType())
        );

        Project savedProject = projectRepository.save(project);
        List<ProjectMember> savedMembers = new ArrayList<>();
        savedMembers.add(projectMemberRepository.save(new ProjectMember(savedProject, owner, "OWNER", "ACCEPTED")));
        savedMembers.addAll(saveMembers(savedProject, owner, request.getMembers()));

        return new ProjectResponse(savedProject, savedMembers, owner.getId());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findMine(Authentication authentication) {
        AppUser owner = getCurrentUser(authentication);
        Map<Long, Project> visibleProjects = new LinkedHashMap<>();

        projectMemberRepository.findByUserAndStatusOrderByCreatedAtDesc(owner, "ACCEPTED")
                .forEach(member -> visibleProjects.putIfAbsent(member.getProject().getId(), member.getProject()));

        return new ArrayList<>(visibleProjects.values()).stream()
                .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
                .map(project -> new ProjectResponse(project, projectMemberRepository.findByProjectOrderByCreatedAtAsc(project), owner.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchInviteCandidates(String email, Authentication authentication) {
        AppUser currentUser = getCurrentUser(authentication);
        String keyword = normalize(email);

        if (keyword == null || keyword.length() < 2) {
            return List.of();
        }

        return appUserRepository.findTop10ByEmailContainingIgnoreCase(keyword)
                .stream()
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .map(UserSearchResponse::new)
                .toList();
    }

    public AppUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            String provider = oauth2Authentication.getAuthorizedClientRegistrationId().toUpperCase();
            String providerId = extractOAuthProviderId(provider, oauth2Authentication.getPrincipal().getAttributes());

            return appUserRepository.findByProviderAndProviderId(provider, providerId)
                    .orElseThrow(() -> new IllegalStateException("소셜 로그인 사용자를 찾을 수 없습니다."));
        }

        return appUserRepository.findByUserId(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));
    }

    @SuppressWarnings("unchecked")
    private String extractOAuthProviderId(String provider, Map<String, Object> attributes) {
        Object value;

        if ("NAVER".equals(provider)) {
            Object response = attributes.get("response");
            if (!(response instanceof Map<?, ?> responseMap)) {
                throw new IllegalStateException("네이버 로그인 응답을 확인할 수 없습니다.");
            }
            value = ((Map<String, Object>) responseMap).get("id");
        } else if ("KAKAO".equals(provider)) {
            value = attributes.get("id");
        } else {
            value = attributes.get("sub");
        }

        if (value == null || value.toString().isBlank()) {
            throw new IllegalStateException("소셜 로그인 식별자를 확인할 수 없습니다.");
        }

        return value.toString().trim();
    }

    private void validate(ProjectCreateRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("서비스 아이디어를 입력해주세요.");
        }

        if (request.getTitle().trim().length() > 200) {
            throw new IllegalArgumentException("서비스 아이디어는 200자 이하로 입력해주세요.");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String normalizeType(String value) {
        if ("collab".equals(value)) {
            return "collab";
        }

        return "personal";
    }

    private List<ProjectMember> saveMembers(Project project, AppUser owner, List<String> members) {
        return normalizeMembers(members).stream()
                .map(email -> findMemberByEmail(owner, email))
                .map(member -> projectMemberRepository.save(new ProjectMember(project, member, "MEMBER", "PENDING")))
                .toList();
    }

    private AppUser findMemberByEmail(AppUser owner, String email) {
        AppUser member = appUserRepository.findFirstByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 팀원 이메일입니다: " + email));

        if (member.getId().equals(owner.getId())) {
            throw new IllegalArgumentException("본인은 팀원으로 추가할 수 없습니다.");
        }

        return member;
    }

    private List<String> normalizeMembers(List<String> members) {
        if (members == null) {
            return List.of();
        }

        return members.stream()
                .map(this::normalize)
                .filter(email -> email != null && email.length() <= 100)
                .distinct()
                .toList();
    }
}
