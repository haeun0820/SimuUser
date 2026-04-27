package com.example.simuuser.repository;

import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.ScenarioComparisonResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioComparisonResultRepository extends JpaRepository<ScenarioComparisonResult, Long> {

    List<ScenarioComparisonResult> findByProjectOrderByCreatedAtDesc(Project project);

    List<ScenarioComparisonResult> findByProjectIn(List<Project> projects);
}
