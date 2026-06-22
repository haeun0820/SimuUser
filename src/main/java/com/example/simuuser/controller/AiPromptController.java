package com.example.simuuser.controller;

import com.example.simuuser.dto.AiPromptRequest;
import com.example.simuuser.service.AiPromptService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class AiPromptController {
    private final AiPromptService aiPromptService;

    public AiPromptController(AiPromptService aiPromptService) {
        this.aiPromptService = aiPromptService;
    }

    @ResponseBody
    @GetMapping("/api/prompts")
    public ResponseEntity<?> prompts(@RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(aiPromptService.find(category));
    }

    @ResponseBody
    @PostMapping("/api/admin/prompts")
    public ResponseEntity<?> createPrompt(@RequestBody AiPromptRequest request) {
        try {
            return ResponseEntity.ok(aiPromptService.create(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @PutMapping("/api/admin/prompts/{promptId}")
    public ResponseEntity<?> updatePrompt(@PathVariable("promptId") Long promptId, @RequestBody AiPromptRequest request) {
        try {
            return ResponseEntity.ok(aiPromptService.update(promptId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @DeleteMapping("/api/admin/prompts/{promptId}")
    public ResponseEntity<?> deletePrompt(@PathVariable("promptId") Long promptId) {
        try {
            aiPromptService.delete(promptId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
