package com.example.simuuser.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // 이 부분으로 수정해야 합니다!
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class FeedbackController {

    @PostMapping("/feedbackresult")
public String getFeedbackResult(@RequestParam(value = "file", required = false) MultipartFile file, 
                                @RequestParam(value = "textContent", required = false) String textContent, 
                                Model model) {
    
    Map<String, Object> analysisData = new HashMap<>();
    analysisData.put("totalScore", 10);
    analysisData.put("logicScore", 90);
    analysisData.put("completionScore", 5);
    analysisData.put("feasibilityScore", 15);
    
    analysisData.put("strengths", Arrays.asList(
        "AI 기술 활용은 현대 트렌드에 부합함", 
        "교육 분야는 AI 적용 잠재력이 높음"
    ));
    
    analysisData.put("weaknesses", Arrays.asList(
        "기획서 내용이 너무 추상적이고 구체성이 전혀 없음",
        "서비스의 핵심 가치 제안(Value Proposition)이 불분명함",
        "타겟 고객 및 시장에 대한 정의가 없음",
        "AI 기반 유저 시뮬레이터 설명 부족",
        "검증 분석 및 차별화 전략 부재"
    ));

    // ★ 이 부분이 빠져있어서 에러가 난 것입니다! 추가해주세요.
    analysisData.put("missingElements", Arrays.asList(
        "서비스 목표 및 비전", "문제 정의", "솔루션 상세", 
        "타겟 고객 정의", "시장 분석", "비즈니스 모델",
        "기술 스택", "팀 구성", "마케팅 및 영업 전략"
    ));

    model.addAttribute("result", analysisData);

    return "feedback/feedback_result"; 
}
}