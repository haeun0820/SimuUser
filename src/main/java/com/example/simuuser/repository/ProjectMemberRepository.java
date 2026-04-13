package com.example.simuuser.repository;

import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProjectOrderByCreatedAtAsc(Project project);

    List<ProjectMember> findByUserOrderByCreatedAtDesc(AppUser user);
}
