package com.example.simuuser.service;

import com.example.simuuser.dto.NotificationResponse;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Notification;
import com.example.simuuser.entity.ProjectMember;
import com.example.simuuser.repository.NotificationRepository;
import com.example.simuuser.repository.ProjectMemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    public static final String TYPE_PROJECT_INVITE = "PROJECT_INVITE";

    private final NotificationRepository notificationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;

    public NotificationService(
            NotificationRepository notificationRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectService projectService
    ) {
        this.notificationRepository = notificationRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
    }

    @Transactional
    public void createProjectInviteNotification(ProjectMember invite) {
        notificationRepository
                .findByRecipientAndTypeAndReferenceIdAndDeletedAtIsNull(invite.getUser(), TYPE_PROJECT_INVITE, invite.getId())
                .orElseGet(() -> notificationRepository.save(new Notification(
                        invite.getUser(),
                        TYPE_PROJECT_INVITE,
                        invite.getId(),
                        "프로젝트 초대 요청",
                        invite.getProject().getOwner().getName() + "님이 " + invite.getProject().getTitle() + " 프로젝트에 초대했습니다."
                )));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findMine(Authentication authentication) {
        AppUser currentUser = projectService.getCurrentUser(authentication);
        return notificationRepository.findByRecipientAndDeletedAtIsNullOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(notification -> new NotificationResponse(notification, resolveInvite(notification)))
                .toList();
    }

    @Transactional
    public void delete(Long notificationId, Authentication authentication) {
        AppUser currentUser = projectService.getCurrentUser(authentication);
        Notification notification = notificationRepository.findByIdAndRecipient(notificationId, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        notification.delete();
    }

    @Transactional
    public void deleteAll(Authentication authentication) {
        AppUser currentUser = projectService.getCurrentUser(authentication);
        notificationRepository.findByRecipientAndDeletedAtIsNullOrderByCreatedAtDesc(currentUser)
                .forEach(Notification::delete);
    }

    @Transactional
    public void markProjectInviteHandled(Long inviteId, Authentication authentication) {
        AppUser currentUser = projectService.getCurrentUser(authentication);
        notificationRepository
                .findByRecipientAndTypeAndReferenceIdAndDeletedAtIsNull(currentUser, TYPE_PROJECT_INVITE, inviteId)
                .ifPresent(notification -> {
                    notification.markRead();
                    notification.delete();
                });
    }

    private ProjectMember resolveInvite(Notification notification) {
        if (!TYPE_PROJECT_INVITE.equals(notification.getType())) {
            return null;
        }

        return projectMemberRepository.findById(notification.getReferenceId()).orElse(null);
    }
}
