package com.example.simuuser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TestController {
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/dashboard";
    }

            // {projectId} 부분이 URL의 숫자를 받아주는 변수 역할을 합니다.
    @GetMapping("/project/detail/{projectId}")
    public String projectDetail(@PathVariable("projectId") String projectId, Model model) {
        // 받아온 ID를 타임리프 페이지로 넘겨줍니다.
        model.addAttribute("projectId", projectId);
        return "project/exact_project"; // 실제 html 파일 경로
    }

    @GetMapping("/scenario")
    public String scenario() {
        return "scenario/scenario";
    }

    @PostMapping("/scenario/result")
    public String scenarioResultPost(@RequestParam(value = "compareTitle", required = false) String compareTitle, Model model) {
    // 필요한 경우 전달받은 데이터를 모델에 담아 결과창에 띄울 수 있습니다.
        model.addAttribute("title", compareTitle);
        
        // 결과 페이지 HTML 경로 리턴
        return "scenario/scenario_result";
}

    @GetMapping("/document")
    public String document() {
        return "document/document";
    }

    @GetMapping("/document/editor")
    public String openEditor(@RequestParam Long id) {
        // id를 기반으로 DB에서 문서를 조회하는 로직을 추가할 수도 있습니다.
        return "document/document_editor"; // templates/document/editor.html 을 호출
    }

    @GetMapping("/feedback")
    public String feedback() {
        return "feedback/feedback";
    }
}
