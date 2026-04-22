package com.example.simuuser.repository;

import com.example.simuuser.entity.ChatRoom;
import com.example.simuuser.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByProject(Project project);

    List<ChatRoom> findByProjectIn(List<Project> projects);
}
