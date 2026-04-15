package com.example.simuuser.repository;

import com.example.simuuser.entity.CostAnalysisResult;
import com.example.simuuser.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CostAnalysisResultRepository extends JpaRepository<CostAnalysisResult, Long> {

    List<CostAnalysisResult> findByProjectOrderByCreatedAtDesc(Project project);
}
