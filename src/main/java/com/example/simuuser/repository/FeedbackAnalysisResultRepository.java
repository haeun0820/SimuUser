package com.example.simuuser.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.simuuser.entity.FeedbackAnalysisResult;
import com.example.simuuser.entity.Project;

public interface FeedbackAnalysisResultRepository extends JpaRepository<FeedbackAnalysisResult, Long> {

    List<FeedbackAnalysisResult> findByProjectOrderByCreatedAtDesc(Project project);

    List<FeedbackAnalysisResult> findByProject(Project project);
}
