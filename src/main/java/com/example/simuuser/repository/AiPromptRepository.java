package com.example.simuuser.repository;

import com.example.simuuser.entity.AiPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiPromptRepository extends JpaRepository<AiPrompt, Long> {
    List<AiPrompt> findAllByOrderByCreatedAtDesc();
    List<AiPrompt> findByCategoryOrderByCreatedAtDesc(String category);
}
