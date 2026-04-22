package com.example.simuuser.repository;

import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.ChatParticipant;
import com.example.simuuser.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByUserOrderByRoomUpdatedAtDesc(AppUser user);

    List<ChatParticipant> findByRoomOrderByJoinedAtAsc(ChatRoom room);

    Optional<ChatParticipant> findByRoomAndUser(ChatRoom room, AppUser user);

    boolean existsByRoomAndUserAndStatus(ChatRoom room, AppUser user, String status);
}
