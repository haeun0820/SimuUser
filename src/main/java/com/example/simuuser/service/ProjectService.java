package com.example.simuuser.service;

import com.example.simuuser.dto.ProjectCreateRequest;
import com.example.simuuser.dto.ProjectInviteResponse;
import com.example.simuuser.dto.ProjectMemberResponse;
import com.example.simuuser.dto.ProjectResponse;
import com.example.simuuser.dto.UserSearchResponse;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ProjectMember;
import com.example.simuuser.repository.AppUserRepository;
import com.example.simuuser.repository.NotificationRepository;
import com.example.simuuser.repository.ProjectMemberRepository;
import com.example.simuuser.repository.ProjectRepository;
import com.example.simuuser.entity.Notification;
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
    private final NotificationRepository notificationRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            AppUserRepository appUserRepository,
            NotificationRepository notificationRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.appUserRepository = appUserRepository;
        this.notificationRepository = notificationRepository;
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

    @Transactional
    public ProjectInviteResponse inviteMember(Long projectId, String email, String role, Authentication authentication) {
        AppUser currentUser = getCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다."));

        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("프로젝트 소유자만 팀원을 초대할 수 있습니다.");
        }

        AppUser invitedUser = findMemberByEmail(currentUser, normalize(email));
        String memberRole = normalizeRole(role);

        ProjectMember invite = projectMemberRepository.findByProjectAndUser(project, invitedUser)
                .map(existing -> {
                    if ("ACCEPTED".equals(existing.getStatus())) {
                        throw new IllegalArgumentException("이미 프로젝트 팀원입니다.");
                    }
                    if ("PENDING".equals(existing.getStatus())) {
                        throw new IllegalArgumentException("이미 초대 요청을 보낸 사용자입니다.");
                    }
                    existing.resend(memberRole);
                    return existing;
                })
                .orElseGet(() -> projectMemberRepository.save(new ProjectMember(project, invitedUser, memberRole, "PENDING")));

        createProjectInviteNotification(invite);
        return new ProjectInviteResponse(invite);
    }

    @Transactional(readOnly = true)
    public List<ProjectInviteResponse> findPendingInvites(Authentication authentication) {
        AppUser currentUser = getCurrentUser(authentication);
        return projectMemberRepository.findByUserAndStatusOrderByCreatedAtDesc(currentUser, "PENDING")
                .stream()
                .map(ProjectInviteResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> findProjectMembers(Long projectId, Authentication authentication) {
        AppUser currentUser = getCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다."));

        if (!projectMemberRepository.existsByProjectAndUserAndStatus(project, currentUser, "ACCEPTED")) {
            throw new IllegalStateException("프로젝트 멤버만 팀원 목록을 볼 수 있습니다.");
        }

        return projectMemberRepository.findByProjectOrderByCreatedAtAsc(project)
                .stream()
                .map(member -> new ProjectMemberResponse(member, currentUser.getId()))
                .toList();
    }

    @Transactional
    public void removeProjectMember(Long projectId, Long memberId, Authentication authentication) {
        AppUser currentUser = getCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다."));

        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("프로젝트 소유자만 팀원을 삭제할 수 있습니다.");
        }

        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("팀원을 찾을 수 없습니다."));

        if (!member.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("해당 프로젝트의 팀원이 아닙니다.");
        }

        if ("OWNER".equals(member.getRole()) || member.getUser().getId().equals(project.getOwner().getId())) {
            throw new IllegalArgumentException("프로젝트 소유자는 삭제할 수 없습니다.");
        }

        projectMemberRepository.delete(member);
    }

    @Transactional
    public ProjectInviteResponse acceptInvite(Long inviteId, Authentication authentication) {
        AppUser currentUser = getCurrentUser(authentication);
        ProjectMember invite = projectMemberRepository.findByIdAndUser(inviteId, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("초대 요청을 찾을 수 없습니다."));

        if (!"PENDING".equals(invite.getStatus())) {
            throw new IllegalStateException("이미 처리된 초대 요청입니다.");
        }

        invite.accept();
        return new ProjectInviteResponse(invite);
    }

    @Transactional
    public ProjectInviteResponse declineInvite(Long inviteId, Authentication authentication) {
        AppUser currentUser = getCurrentUser(authentication);
        ProjectMember invite = projectMemberRepository.findByIdAndUser(inviteId, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("초대 요청을 찾을 수 없습니다."));

        if (!"PENDING".equals(invite.getStatus())) {
            throw new IllegalStateException("이미 처리된 초대 요청입니다.");
        }

        invite.decline();
        return new ProjectInviteResponse(invite);
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
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("초대할 사용자 이메일을 입력해주세요.");
        }

        AppUser member = appUserRepository.findFirstByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 팀원 이메일입니다: " + email));

        if (member.getId().equals(owner.getId())) {
            throw new IllegalArgumentException("본인은 팀원으로 추가할 수 없습니다.");
        }

        return member;
    }

    private String normalizeRole(String role) {
        String normalized = normalize(role);
        if (normalized == null) {
            return "편집자";
        }

        return switch (normalized) {
            case "관리자", "뷰어", "편집자" -> normalized;
            default -> "편집자";
        };
    }

    private void createProjectInviteNotification(ProjectMember invite) {
        notificationRepository
                .findByRecipientAndTypeAndReferenceIdAndDeletedAtIsNull(
                        invite.getUser(),
                        NotificationService.TYPE_PROJECT_INVITE,
                        invite.getId()
                )
                .orElseGet(() -> notificationRepository.save(new Notification(
                        invite.getUser(),
                        NotificationService.TYPE_PROJECT_INVITE,
                        invite.getId(),
                        "프로젝트 초대 요청",
                        invite.getProject().getOwner().getName() + "님이 " + invite.getProject().getTitle() + " 프로젝트에 초대했습니다."
                )));
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
