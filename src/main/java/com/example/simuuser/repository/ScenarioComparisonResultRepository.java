package com.example.simuuser.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ScenarioComparisonResult;

public interface ScenarioComparisonResultRepository extends JpaRepository<ScenarioComparisonResult, Long> {

    List<ScenarioComparisonResult> findByProjectOrderByCreatedAtDesc(Project project);

    List<ScenarioComparisonResult> findByProjectIn(List<Project> projects);

    List<ScenarioComparisonResult> findByProject(Project project);
}
