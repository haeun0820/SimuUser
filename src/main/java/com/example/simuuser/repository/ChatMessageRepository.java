package com.example.simuuser.repository;

import com.example.simuuser.entity.ChatMessage;
import com.example.simuuser.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select m
            from ChatMessage m
            join fetch m.sender
            where m.room = :room
            order by m.createdAt asc
            """)
    List<ChatMessage> findByRoomOrderByCreatedAtAsc(ChatRoom room);

    @Query("""
            select m
            from ChatMessage m
            join fetch m.sender
            where m.room = :room and m.id > :messageId
            order by m.createdAt asc
            """)
    List<ChatMessage> findByRoomAndIdGreaterThanOrderByCreatedAtAsc(@Param("room") ChatRoom room, @Param("messageId") Long messageId);

    Optional<ChatMessage> findTopByRoomOrderByCreatedAtDesc(ChatRoom room);

    @Query("""
            select m
            from ChatMessage m
            where m.room in :rooms
            order by m.room.id asc, m.createdAt desc
            """)
    List<ChatMessage> findByRoomInOrderByRoomIdAscCreatedAtDesc(@Param("rooms") List<ChatRoom> rooms);
}
