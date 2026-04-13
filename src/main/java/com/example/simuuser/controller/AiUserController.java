package com.example.simuuser.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/aiuser")
public class AiUserController {

    // application.properties 에 gemini.api.key=AIzaSy... 형태로 추가
    private String geminiApiKey = "AIzaSyBXR90EGyObExOSWVPMQKXw1vcX9kMslkE";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    // ── 페이지 라우팅 ──────────────────────────────────────────────

    @GetMapping("/ai_user")
    public String aiUserPage() {
        return "aiuser/ai_user";
    }

    @GetMapping("/result")
    public String aiUserResultPage() {
        return "aiuser/ai_user_result";
    }

    // ── Gemini API 프록시 ───────────────────────────────────────

    @PostMapping("/simulate")
    @ResponseBody
    public ResponseEntity<?> simulate(@RequestBody Map<String, Object> body) {
        try {
            // 실제 AI가 고민하는 것처럼 1.5초 딜레이 (1500ms)
            Thread.sleep(1500);

            int personaCount = body.get("personaCount") instanceof Number
                    ? ((Number) body.get("personaCount")).intValue() : 3;

            // 가짜(Mock) 응답 데이터 만들기
            Map<String, Object> mockResult = new java.util.HashMap<>();
            mockResult.put("avgPurchaseIntent", 82);
            mockResult.put("overallReaction", "대체로 긍정적이며, 특히 핵심 타겟인 2030 세대에서 즉각적인 반응을 보입니다. 다만 초기 진입 장벽(가격, 복잡도)에 대한 우려가 일부 있습니다.");

            // 페르소나 리스트 만들기
            java.util.List<Map<String, Object>> personas = new java.util.ArrayList<>();
            
            Map<String, Object> p1 = new java.util.HashMap<>();
            p1.put("name", "김민지");
            p1.put("age", 28);
            p1.put("job", "서비스 기획자");
            p1.put("consumerType", "신중형");
            p1.put("purchaseScore", 85);
            p1.put("positiveReactions", java.util.Arrays.asList("UI가 직관적이고 깔끔하다", "내가 딱 필요했던 기능"));
            p1.put("negativeReactions", java.util.Arrays.asList("초기 세팅 과정이 조금 길다"));
            p1.put("churnPoints", java.util.Arrays.asList("푸시 알림이 너무 자주 올 때"));
            personas.add(p1);

            Map<String, Object> p2 = new java.util.HashMap<>();
            p2.put("name", "이준호");
            p2.put("age", 35);
            p2.put("job", "마케터");
            p2.put("consumerType", "소비형");
            p2.put("purchaseScore", 60);
            p2.put("positiveReactions", java.util.Arrays.asList("데이터 시각화가 훌륭하다"));
            p2.put("negativeReactions", java.util.Arrays.asList("가격 대비 차별성이 부족하다"));
            p2.put("churnPoints", java.util.Arrays.asList("무료 체험 기간이 끝나는 시점"));
            personas.add(p2);

            Map<String, Object> p3 = new java.util.HashMap<>();
            p3.put("name", "박소연");
            p3.put("age", 23);
            p3.put("job", "대학생");
            p3.put("consumerType", "충동형");
            p3.put("purchaseScore", 95);
            p3.put("positiveReactions", java.util.Arrays.asList("트렌디하고 친구들에게 공유하기 좋다", "재밌다"));
            p3.put("negativeReactions", java.util.Arrays.asList("데이터를 많이 쓸 것 같다"));
            p3.put("churnPoints", java.util.Arrays.asList("앱 로딩이 3초 이상 걸릴 때"));
            personas.add(p3);

            // 요청한 페르소나 수만큼 잘라서 넣기
            mockResult.put("personas", personas.subList(0, Math.min(personaCount, personas.size())));

            mockResult.put("keyInsights", java.util.Arrays.asList(
                    "20대 유저는 소셜 공유와 시각적 디자인에 가장 크게 반응합니다.",
                    "30대 직장인 그룹은 '시간 단축'을 핵심 가치로 평가합니다."
            ));
            mockResult.put("improvements", java.util.Arrays.asList(
                    "초기 온보딩 과정을 2단계 이하로 축소하여 이탈을 방지하세요.",
                    "첫 결제 시 무료 체험 기간을 제공해 심리적 장벽을 낮추세요."
            ));

            return ResponseEntity.ok(mockResult);

        } catch (Exception e) {
            Map<String, String> errorMap = new java.util.HashMap<>();
            errorMap.put("error", "서버 내부 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
        }
    }

    // ── 프롬프트 빌더 ─────────────────────────────────────────────

    private String buildPrompt(Map<String, Object> p) {
        int personaCount = p.get("personaCount") instanceof Number
                ? ((Number) p.get("personaCount")).intValue() : 3;

        return "당신은 스타트업 서비스 아이디어를 검증하는 AI 시뮬레이션 전문가입니다.\n\n"
            + "다음 서비스 아이디어에 대해 가상 유저 " + personaCount + "명의 반응을 시뮬레이션해주세요.\n\n"
            + "[서비스 정보]\n"
            + "- 서비스 아이디어: " + p.getOrDefault("serviceIdea", "미입력") + "\n"
            + "- 상세 설명: "       + p.getOrDefault("description",  "없음")  + "\n"
            + "- 타겟 유저: "       + p.getOrDefault("targetUser",   "없음")  + "\n"
            + "- 산업 분야: "       + p.getOrDefault("industry",     "미지정") + "\n\n"
            + "[시뮬레이션 설정]\n"
            + "- 페르소나 수: " + personaCount + "명\n"
            + "- 성별: "       + p.getOrDefault("gender", "전체") + "\n"
            + "- 나이대: "     + p.getOrDefault("ages",   "전체") + "\n"
            + "- 직업: "       + p.getOrDefault("job",    "미입력") + "\n\n"
            + "아래 JSON 형식으로만 응답해주세요.\n\n" // Gemini는 generationConfig로 강제하므로 마크다운 빼달라는 말은 생략해도 됩니다.
            + "{\n"
            + "  \"avgPurchaseIntent\": <0-100 정수, 평균 구매 의사 %>,\n"
            + "  \"overallReaction\": \"<전반적인 반응 2-3문장>\",\n"
            + "  \"personas\": [\n"
            + "    {\n"
            + "      \"name\": \"<한국인 이름>\",\n"
            + "      \"age\": <나이 정수>,\n"
            + "      \"job\": \"<직업>\",\n"
            + "      \"consumerType\": \"<소비형|절약형|충동형|신중형 중 하나>\",\n"
            + "      \"purchaseScore\": <0-100 정수>,\n"
            + "      \"positiveReactions\": [\"<반응1>\", \"<반응2>\", \"<반응3>\"],\n"
            + "      \"negativeReactions\": [\"<반응1>\", \"<반응2>\", \"<반응3>\"],\n"
            + "      \"churnPoints\": [\"<이탈포인트1>\", \"<이탈포인트2>\"]\n"
            + "    }\n"
            + "  ],\n"
            + "  \"keyInsights\": [\"<인사이트1>\", \"<인사이트2>\", \"<인사이트3>\", \"<인사이트4>\"],\n"
            + "  \"improvements\": [\"<개선제안1>\", \"<개선제안2>\", \"<개선제안3>\", \"<개선제안4>\"]\n"
            + "}\n\n"
            + "페르소나는 정확히 " + personaCount + "명이어야 하며, 각각 다른 개성과 관점을 가져야 합니다.\n"
            + "현실적이고 구체적인 내용으로 작성해주세요.";
    }
}