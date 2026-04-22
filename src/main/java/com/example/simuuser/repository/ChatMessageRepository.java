package com.example.simuuser.repository;

import com.example.simuuser.entity.ChatMessage;
import com.example.simuuser.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomOrderByCreatedAtAsc(ChatRoom room);

    Optional<ChatMessage> findTopByRoomOrderByCreatedAtDesc(ChatRoom room);
}
