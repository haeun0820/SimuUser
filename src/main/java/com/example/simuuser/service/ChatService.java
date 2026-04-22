package com.example.simuuser.service;

import com.example.simuuser.dto.ChatMessageResponse;
import com.example.simuuser.dto.ChatRequestResponse;
import com.example.simuuser.dto.ChatRoomDetailResponse;
import com.example.simuuser.dto.ChatRoomSummaryResponse;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.ChatMessage;
import com.example.simuuser.entity.ChatParticipant;
import com.example.simuuser.entity.ChatRoom;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ProjectMember;
import com.example.simuuser.repository.AppUserRepository;
import com.example.simuuser.repository.ChatMessageRepository;
import com.example.simuuser.repository.ChatParticipantRepository;
import com.example.simuuser.repository.ChatRoomRepository;
import com.example.simuuser.repository.ProjectMemberRepository;
import com.example.simuuser.repository.ProjectRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;

    public ChatService(
            ChatRoomRepository chatRoomRepository,
            ChatParticipantRepository chatParticipantRepository,
            ChatMessageRepository chatMessageRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            AppUserRepository appUserRepository,
            AppUserService appUserService
    ) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.appUserRepository = appUserRepository;
        this.appUserService = appUserService;
    }

    @Transactional
    public List<ChatRoomSummaryResponse> findMyRooms(Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        syncExistingProjectRooms(currentUser);

        return chatParticipantRepository.findByUserOrderByRoomUpdatedAtDesc(currentUser).stream()
                .filter(participant -> "ACCEPTED".equals(participant.getStatus()))
                .map(ChatParticipant::getRoom)
                .filter(room -> "ACTIVE".equals(room.getStatus()))
                .distinct()
                .sorted(Comparator.comparing(ChatRoom::getUpdatedAt).reversed())
                .map(room -> toSummary(room, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatRequestResponse> findPendingRequests(Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);

        return chatParticipantRepository.findByUserOrderByRoomUpdatedAtDesc(currentUser).stream()
                .filter(participant -> "PENDING".equals(participant.getStatus()))
                .map(ChatParticipant::getRoom)
                .filter(room -> "PRIVATE".equals(room.getType()))
                .filter(room -> "PENDING".equals(room.getStatus()))
                .map(room -> toRequest(room, currentUser))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public ChatRoomSummaryResponse requestPrivateChat(String email, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        String normalizedEmail = normalize(email);

        if (normalizedEmail == null) {
            throw new IllegalArgumentException("대화 상대 이메일을 입력해주세요.");
        }

        AppUser targetUser = appUserRepository.findFirstByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다."));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("본인에게 채팅 요청을 보낼 수 없습니다.");
        }

        ChatRoom existingRoom = findPrivateRoomBetween(currentUser, targetUser);
        if (existingRoom != null) {
            ChatParticipant targetParticipant = findParticipant(existingRoom, targetUser);
            ChatParticipant currentParticipant = findParticipant(existingRoom, currentUser);

            if ("ACTIVE".equals(existingRoom.getStatus())) {
                return toSummary(existingRoom, currentUser);
            }

            if ("PENDING".equals(existingRoom.getStatus())) {
                if (existingRoom.getRequestedBy() != null && existingRoom.getRequestedBy().getId().equals(currentUser.getId())) {
                    throw new IllegalArgumentException("이미 채팅 요청을 보냈습니다.");
                }

                if (targetParticipant != null && currentParticipant != null) {
                    currentParticipant.accept();
                    targetParticipant.accept();
                    existingRoom.activate();
                    return toSummary(existingRoom, currentUser);
                }
            }

            existingRoom.markPending(currentUser);
            if (currentParticipant != null) {
                currentParticipant.accept();
            }
            if (targetParticipant != null) {
                targetParticipant.markPending();
            }

            return toSummary(existingRoom, currentUser);
        }

        ChatRoom room = chatRoomRepository.save(new ChatRoom("PRIVATE", "PENDING", null, currentUser));
        chatParticipantRepository.save(new ChatParticipant(room, currentUser, "ACCEPTED"));
        chatParticipantRepository.save(new ChatParticipant(room, targetUser, "PENDING"));
        room.touch();

        return toSummary(room, currentUser);
    }

    @Transactional
    public ChatRoomSummaryResponse createOrGetProjectRoom(Long projectId, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다."));

        ChatRoom room = createOrGetProjectRoomEntity(project, currentUser);
        return toSummary(room, currentUser);
    }

    @Transactional
    public ChatRoomDetailResponse findRoomDetail(Long roomId, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        ChatRoom room = resolveRoomReference(roomId, currentUser, true);
        return new ChatRoomDetailResponse(room.getId(), room.getType(), resolveRoomName(room, currentUser), "ACTIVE".equals(room.getStatus()));
    }

    @Transactional
    public List<ChatMessageResponse> findMessages(Long roomId, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        ChatRoom room = resolveRoomReference(roomId, currentUser, true);

        return chatMessageRepository.findByRoomOrderByCreatedAtAsc(room).stream()
                .map(message -> new ChatMessageResponse(message, currentUser.getId()))
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, String content, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        ChatRoom room = resolveRoomReference(roomId, currentUser, true);

        if (!"ACTIVE".equals(room.getStatus())) {
            throw new IllegalStateException("아직 수락되지 않은 채팅입니다.");
        }

        String normalizedContent = normalize(content);
        if (normalizedContent == null) {
            throw new IllegalArgumentException("메시지를 입력해주세요.");
        }

        ChatMessage message = chatMessageRepository.save(new ChatMessage(room, currentUser, normalizedContent));
        room.touch();
        return new ChatMessageResponse(message, currentUser.getId());
    }

    @Transactional
    public ChatRoomSummaryResponse acceptRequest(Long roomId, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        ChatRoom room = findAccessibleRoom(roomId, currentUser, false);

        if (!"PRIVATE".equals(room.getType()) || !"PENDING".equals(room.getStatus())) {
            throw new IllegalStateException("수락할 수 있는 채팅 요청이 아닙니다.");
        }

        ChatParticipant participant = chatParticipantRepository.findByRoomAndUser(room, currentUser)
                .orElseThrow(() -> new IllegalStateException("채팅 요청 참가자를 찾을 수 없습니다."));

        if (!"PENDING".equals(participant.getStatus())) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        participant.accept();
        room.activate();
        return toSummary(room, currentUser);
    }

    @Transactional
    public void declineRequest(Long roomId, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        ChatRoom room = findAccessibleRoom(roomId, currentUser, false);

        if (!"PRIVATE".equals(room.getType()) || !"PENDING".equals(room.getStatus())) {
            throw new IllegalStateException("거절할 수 있는 채팅 요청이 아닙니다.");
        }

        ChatParticipant participant = chatParticipantRepository.findByRoomAndUser(room, currentUser)
                .orElseThrow(() -> new IllegalStateException("채팅 요청 참가자를 찾을 수 없습니다."));

        participant.decline();
        room.decline();
    }

    private void syncExistingProjectRooms(AppUser currentUser) {
        List<Project> collabProjects = projectMemberRepository.findByUserAndStatusOrderByCreatedAtDesc(currentUser, "ACCEPTED").stream()
                .map(ProjectMember::getProject)
                .filter(project -> "collab".equalsIgnoreCase(project.getType()))
                .distinct()
                .toList();

        for (Project project : collabProjects) {
            chatRoomRepository.findByProject(project).ifPresent(room -> syncProjectParticipants(room, project));
        }
    }

    private void syncProjectParticipants(ChatRoom room, Project project) {
        List<ProjectMember> acceptedMembers = projectMemberRepository.findByProjectOrderByCreatedAtAsc(project).stream()
                .filter(member -> "ACCEPTED".equals(member.getStatus()))
                .toList();

        for (ProjectMember member : acceptedMembers) {
            chatParticipantRepository.findByRoomAndUser(room, member.getUser())
                    .orElseGet(() -> chatParticipantRepository.save(new ChatParticipant(room, member.getUser(), "ACCEPTED")));
        }
    }

    private ChatRoom createOrGetProjectRoomEntity(Project project, AppUser currentUser) {
        if (!projectMemberRepository.existsByProjectAndUserAndStatus(project, currentUser, "ACCEPTED")) {
            throw new IllegalStateException("프로젝트 멤버만 팀 채팅을 사용할 수 있습니다.");
        }

        if (!"collab".equalsIgnoreCase(project.getType())) {
            throw new IllegalArgumentException("협업 프로젝트에서만 팀 채팅을 사용할 수 있습니다.");
        }

        ChatRoom room = chatRoomRepository.findByProject(project)
                .orElseGet(() -> chatRoomRepository.save(new ChatRoom("TEAM", "ACTIVE", project, null)));

        syncProjectParticipants(room, project);
        room.activate();
        return room;
    }

    private ChatRoom findPrivateRoomBetween(AppUser currentUser, AppUser targetUser) {
        return chatParticipantRepository.findByUserOrderByRoomUpdatedAtDesc(currentUser).stream()
                .map(ChatParticipant::getRoom)
                .filter(room -> "PRIVATE".equals(room.getType()))
                .filter(room -> {
                    List<ChatParticipant> participants = chatParticipantRepository.findByRoomOrderByJoinedAtAsc(room);
                    return participants.size() == 2
                            && participants.stream().anyMatch(participant -> participant.getUser().getId().equals(targetUser.getId()));
                })
                .findFirst()
                .orElse(null);
    }

    private ChatParticipant findParticipant(ChatRoom room, AppUser user) {
        return chatParticipantRepository.findByRoomAndUser(room, user).orElse(null);
    }

    private ChatRoom resolveRoomReference(Long roomOrProjectId, AppUser currentUser, boolean acceptedOnly) {
        ChatRoom room = chatRoomRepository.findById(roomOrProjectId).orElse(null);
        if (room != null) {
            return validateRoomAccess(room, currentUser, acceptedOnly);
        }

        Project project = projectRepository.findById(roomOrProjectId).orElse(null);
        if (project != null && "collab".equalsIgnoreCase(project.getType())) {
            ChatRoom projectRoom = createOrGetProjectRoomEntity(project, currentUser);
            return validateRoomAccess(projectRoom, currentUser, acceptedOnly);
        }

        throw new IllegalArgumentException("채팅방을 찾을 수 없습니다.");
    }

    private ChatRoom findAccessibleRoom(Long roomId, AppUser currentUser, boolean acceptedOnly) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        return validateRoomAccess(room, currentUser, acceptedOnly);
    }

    private ChatRoom validateRoomAccess(ChatRoom room, AppUser currentUser, boolean acceptedOnly) {
        ChatParticipant participant = chatParticipantRepository.findByRoomAndUser(room, currentUser)
                .orElseThrow(() -> new IllegalStateException("채팅방 접근 권한이 없습니다."));

        if (acceptedOnly && !"ACCEPTED".equals(participant.getStatus())) {
            throw new IllegalStateException("아직 수락되지 않은 채팅방입니다.");
        }

        return room;
    }

    private ChatRoomSummaryResponse toSummary(ChatRoom room, AppUser currentUser) {
        ChatMessage lastMessage = chatMessageRepository.findTopByRoomOrderByCreatedAtDesc(room).orElse(null);
        return new ChatRoomSummaryResponse(
                room.getId(),
                room.getType(),
                resolveRoomName(room, currentUser),
                resolveRoomProfileImage(room, currentUser),
                lastMessage == null ? "" : lastMessage.getContent(),
                lastMessage == null ? room.getUpdatedAt() : lastMessage.getCreatedAt()
        );
    }

    private ChatRequestResponse toRequest(ChatRoom room, AppUser currentUser) {
        AppUser requester = room.getRequestedBy();
        if (requester == null || requester.getId().equals(currentUser.getId())) {
            return null;
        }

        ChatMessage lastMessage = chatMessageRepository.findTopByRoomOrderByCreatedAtDesc(room).orElse(null);
        return new ChatRequestResponse(
                room.getId(),
                displayName(requester),
                requester.getEmail(),
                requester.getProfileImage(),
                lastMessage == null ? "채팅을 요청했습니다." : lastMessage.getContent(),
                lastMessage == null ? room.getUpdatedAt() : lastMessage.getCreatedAt()
        );
    }

    private String resolveRoomName(ChatRoom room, AppUser currentUser) {
        if ("TEAM".equals(room.getType()) && room.getProject() != null) {
            return room.getProject().getTitle() + " 팀 채팅";
        }

        return chatParticipantRepository.findByRoomOrderByJoinedAtAsc(room).stream()
                .map(ChatParticipant::getUser)
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .findFirst()
                .map(this::displayName)
                .orElse("채팅");
    }

    private String resolveRoomProfileImage(ChatRoom room, AppUser currentUser) {
        if ("TEAM".equals(room.getType())) {
            return null;
        }

        return chatParticipantRepository.findByRoomOrderByJoinedAtAsc(room).stream()
                .map(ChatParticipant::getUser)
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .findFirst()
                .map(AppUser::getProfileImage)
                .orElse(null);
    }

    private String displayName(AppUser user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return user.getUserId();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
