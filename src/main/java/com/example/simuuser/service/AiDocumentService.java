package com.example.simuuser.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.example.simuuser.dto.DocumentRequest;
import com.example.simuuser.dto.DocumentResponse;
import com.example.simuuser.entity.Document;
import com.example.simuuser.entity.Project;
import com.example.simuuser.entity.AiSimulationResult;
import com.example.simuuser.entity.CostAnalysisResult;
import com.example.simuuser.entity.FeedbackAnalysisResult;
import com.example.simuuser.entity.MarketAnalysisResult;

import com.example.simuuser.repository.DocumentRepository;
import com.example.simuuser.repository.ProjectRepository;
import com.example.simuuser.repository.AiSimulationResultRepository;
import com.example.simuuser.repository.CostAnalysisResultRepository;
import com.example.simuuser.repository.FeedbackAnalysisResultRepository;
import com.example.simuuser.repository.MarketAnalysisResultRepository;

@Service
@RequiredArgsConstructor
public class AiDocumentService {

    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final LlmApiService llmApiService; // 방금 만든 Gemini API 통신 서비스

    // 4개의 분석 결과 레포지토리 모두 주입
    private final AiSimulationResultRepository aiRepo;
    private final CostAnalysisResultRepository costRepo;
    private final FeedbackAnalysisResultRepository feedbackRepo;
    private final MarketAnalysisResultRepository marketRepo;

    @Transactional
    public DocumentResponse generateDocument(DocumentRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다."));

        String aiPrompt = "";

        if ("분석결과".equals(request.getDocumentType())) {
            // [수정된 부분] project.getId() 대신 project 엔티티 자체를 넘겨줍니다!
            List<AiSimulationResult> aiResults = aiRepo.findByProject(project);
            List<CostAnalysisResult> costResults = costRepo.findByProject(project);
            List<FeedbackAnalysisResult> feedbackResults = feedbackRepo.findByProject(project);
            List<MarketAnalysisResult> marketResults = marketRepo.findByProject(project);

            aiPrompt = buildAnalysisPrompt(project, aiResults, costResults, feedbackResults, marketResults, request);
        } else {
            aiPrompt = buildGeneralPrompt(project, request);
        }

        // AI API 호출
        String generatedContent = llmApiService.generateText(aiPrompt);

        // 생성된 문서 DB 저장
        Document newDoc = Document.builder()
                .project(project)
                .title(request.getTitle())
                .type(request.getDocumentType())
                .content(generatedContent)
                .build();
        
        documentRepository.save(newDoc);

        return new DocumentResponse(newDoc);
    }

    private String buildGeneralPrompt(Project project, DocumentRequest request) {
        return String.format(
            "너는 전문적인 비즈니스 문서 작성자야. 다음 정보를 바탕으로 %s를 작성해줘.\n" +
            "- 프로젝트명: %s\n" +
            "- 프로젝트 설명: %s\n" +
            "- 작성할 문서 제목: %s\n" +
            "- 문서 추가 설명(요구사항): %s\n" +
            "전문적인 어조로, 서론-본론-결론 구조의 마크다운 형식으로 작성해줘.",
            request.getDocumentType(), project.getTitle(), project.getDescription(), 
            request.getTitle(), request.getDescription()
        );
    }

    private String buildAnalysisPrompt(Project project, 
                                       List<AiSimulationResult> ai, 
                                       List<CostAnalysisResult> cost, 
                                       List<FeedbackAnalysisResult> feedback, 
                                       List<MarketAnalysisResult> market, 
                                       DocumentRequest request) {
        
        // 4가지 분석 결과를 하나의 문자열로 보기 좋게 합칩니다.
        StringBuilder dataContext = new StringBuilder();
        
        dataContext.append("[AI 시뮬레이션 결과]\n");
        ai.forEach(item -> dataContext.append("- ").append(item.toString()).append("\n"));
        
        dataContext.append("\n[비용 분석 결과]\n");
        cost.forEach(item -> dataContext.append("- ").append(item.toString()).append("\n"));
        
        dataContext.append("\n[피드백 분석 결과]\n");
        feedback.forEach(item -> dataContext.append("- ").append(item.toString()).append("\n"));
        
        dataContext.append("\n[시장 분석 결과]\n");
        market.forEach(item -> dataContext.append("- ").append(item.toString()).append("\n"));

        return String.format(
            "너는 데이터 분석가야. 다음 프로젝트 정보와 4가지 분석 도구의 결과 데이터를 종합하여 통합 '분석결과 보고서'를 작성해줘.\n" +
            "- 프로젝트명: %s\n" +
            "- 작성할 문서 제목: %s\n" +
            "- 사용자 요구사항: %s\n" +
            "=== 수집된 분석 데이터 ===\n%s\n===================\n" +
            "이 데이터들이 의미하는 바를 연결해서 인사이트를 도출하고, 마크다운 형식의 보고서로 정리해줘.",
            project.getTitle(), request.getTitle(), request.getDescription(), dataContext.toString()
        );
    }
}