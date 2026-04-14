package com.example.simuuser.repository;

import com.example.simuuser.entity.AiSimulationResult;
import com.example.simuuser.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiSimulationResultRepository extends JpaRepository<AiSimulationResult, Long> {

    List<AiSimulationResult> findByProjectOrderByCreatedAtDesc(Project project);
}
