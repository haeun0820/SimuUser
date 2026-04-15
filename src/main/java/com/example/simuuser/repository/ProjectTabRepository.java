package com.example.simuuser.repository;

import com.example.simuuser.entity.ProjectTab;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectTabRepository extends JpaRepository<ProjectTab, Long> {
    // 특정 프로젝트의 탭들을 순서대로 가져오는 기능 추가
    List<ProjectTab> findByProjectIdOrderByOrderIndexAsc(Long projectId);
}