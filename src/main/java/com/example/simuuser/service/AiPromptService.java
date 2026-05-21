package com.example.simuuser.service;

import com.example.simuuser.dto.AiPromptRequest;
import com.example.simuuser.dto.AiPromptResponse;
import com.example.simuuser.entity.AiPrompt;
import com.example.simuuser.repository.AiPromptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AiPromptService {
    private final AiPromptRepository aiPromptRepository;

    public AiPromptService(AiPromptRepository aiPromptRepository) {
        this.aiPromptRepository = aiPromptRepository;
    }

    @Transactional(readOnly = true)
    public List<AiPromptResponse> find(String category) {
        List<AiPrompt> prompts = isBlank(category)
                ? aiPromptRepository.findAllByOrderByCreatedAtDesc()
                : aiPromptRepository.findByCategoryOrderByCreatedAtDesc(category);
        return prompts.stream().map(AiPromptResponse::new).toList();
    }

    @Transactional
    public AiPromptResponse create(AiPromptRequest request) {
        validate(request);
        AiPrompt prompt = new AiPrompt(
                request.getName().trim(),
                request.getCategory().trim(),
                textOrDefault(request.getModel(), "gemini-2.5-flash"),
                request.getSystemPrompt().trim(),
                request.getUserPromptTemplate().trim()
        );
        return new AiPromptResponse(aiPromptRepository.save(prompt));
    }

    @Transactional
    public AiPromptResponse update(Long promptId, AiPromptRequest request) {
        validate(request);
        AiPrompt prompt = findPrompt(promptId);
        prompt.update(
                request.getName().trim(),
                request.getCategory().trim(),
                textOrDefault(request.getModel(), "gemini-2.5-flash"),
                request.getSystemPrompt().trim(),
                request.getUserPromptTemplate().trim()
        );
        return new AiPromptResponse(prompt);
    }

    @Transactional
    public void delete(Long promptId) {
        aiPromptRepository.delete(findPrompt(promptId));
    }

    @Transactional(readOnly = true)
    public String renderPrompt(Long promptId, Map<String, Object> values) {
        if (promptId == null) {
            return null;
        }
        AiPrompt prompt = findPrompt(promptId);
        return prompt.getSystemPrompt() + "\n\n" + applyTemplate(prompt.getUserPromptTemplate(), values);
    }

    private AiPrompt findPrompt(Long promptId) {
        if (promptId == null) {
            throw new IllegalArgumentException("프롬프트를 찾을 수 없습니다.");
        }
        return aiPromptRepository.findById(promptId)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다."));
    }

    private String applyTemplate(String template, Map<String, Object> values) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            rendered = rendered.replace("{{" + entry.getKey() + "}}", value);
        }
        return rendered;
    }

    private void validate(AiPromptRequest request) {
        if (request == null || isBlank(request.getName())) throw new IllegalArgumentException("프롬프트 이름을 입력해주세요.");
        if (isBlank(request.getCategory())) throw new IllegalArgumentException("카테고리를 선택해주세요.");
        if (isBlank(request.getSystemPrompt())) throw new IllegalArgumentException("시스템 프롬프트를 입력해주세요.");
        if (isBlank(request.getUserPromptTemplate())) throw new IllegalArgumentException("사용자 프롬프트 템플릿을 입력해주세요.");
    }

    private String textOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
