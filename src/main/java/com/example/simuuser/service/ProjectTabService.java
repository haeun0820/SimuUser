package com.example.simuuser.service;

import com.example.simuuser.dto.TabRequest;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ProjectTab;
import com.example.simuuser.repository.ProjectRepository;
import com.example.simuuser.repository.ProjectTabRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProjectTabService {

    private final ProjectTabRepository projectTabRepository;
    private final ProjectRepository projectRepository;

    public ProjectTabService(ProjectTabRepository projectTabRepository, ProjectRepository projectRepository) {
        this.projectTabRepository = projectTabRepository;
        this.projectRepository = projectRepository;
    }

    public ProjectTab addTab(Long projectId, TabRequest dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        // 새 탭 생성 및 저장
        ProjectTab tab = new ProjectTab(project, dto.getName(), dto.getOrderIndex());
        return projectTabRepository.save(tab);
    }

    // 이름 수정
    public void updateTabName(Long tabId, String newName) {
        ProjectTab tab = projectTabRepository.findById(tabId)
                .orElseThrow(() -> new IllegalArgumentException("탭이 없습니다."));
        tab.updateName(newName); // Dirty Checking으로 자동 저장됨
    }

    // 삭제
    public void deleteTab(Long tabId) {
        projectTabRepository.deleteById(tabId);
    }

    @Transactional(readOnly = true)
    public List<ProjectTab> getTabs(Long projectId) {
        return projectTabRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
    }
}