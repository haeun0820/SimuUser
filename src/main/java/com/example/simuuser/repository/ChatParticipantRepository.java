package com.example.simuuser.repository;

import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.ChatParticipant;
import com.example.simuuser.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    @Query("""
            select cp
            from ChatParticipant cp
            join fetch cp.room r
            left join fetch r.project
            left join fetch r.requestedBy
            where cp.user = :user
            order by r.updatedAt desc
            """)
    List<ChatParticipant> findByUserOrderByRoomUpdatedAtDesc(AppUser user);

    @Query("""
            select cp
            from ChatParticipant cp
            join fetch cp.user
            where cp.room = :room
            order by cp.joinedAt asc
            """)
    List<ChatParticipant> findByRoomOrderByJoinedAtAsc(ChatRoom room);

    @Query("""
            select cp
            from ChatParticipant cp
            join fetch cp.user
            join fetch cp.room
            where cp.room in :rooms
            order by cp.room.id asc, cp.joinedAt asc
            """)
    List<ChatParticipant> findByRoomInOrderByRoomIdAscJoinedAtAsc(@Param("rooms") List<ChatRoom> rooms);

    Optional<ChatParticipant> findByRoomAndUser(ChatRoom room, AppUser user);

    boolean existsByRoomAndUserAndStatus(ChatRoom room, AppUser user, String status);
}
