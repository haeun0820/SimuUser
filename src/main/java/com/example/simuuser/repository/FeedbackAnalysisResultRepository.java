package com.example.simuuser.repository;

import com.example.simuuser.entity.FeedbackAnalysisResult;
import com.example.simuuser.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackAnalysisResultRepository extends JpaRepository<FeedbackAnalysisResult, Long> {

    List<FeedbackAnalysisResult> findByProjectOrderByCreatedAtDesc(Project project);
}
