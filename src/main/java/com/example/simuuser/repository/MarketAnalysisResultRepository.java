package com.example.simuuser.repository;

import com.example.simuuser.entity.MarketAnalysisResult;
import com.example.simuuser.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketAnalysisResultRepository extends JpaRepository<MarketAnalysisResult, Long> {

    List<MarketAnalysisResult> findByProjectOrderByCreatedAtDesc(Project project);

    List<MarketAnalysisResult> findByProjectIn(List<Project> projects);
}
