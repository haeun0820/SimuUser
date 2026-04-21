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

    // 프로젝트 상세 페이지
    @GetMapping("/project/detail/{projectId}")
    public String projectDetail(@PathVariable("projectId") String projectId, Model model) {
        model.addAttribute("projectId", projectId);
        return "project/exact_project";
    }

    @GetMapping("/scenario")
    public String scenario() {
        return "scenario/scenario";
    }

    @PostMapping("/scenario/result")
    public String scenarioResultPost(@RequestParam(value = "compareTitle", required = false) String compareTitle, Model model) {
        model.addAttribute("title", compareTitle);
        return "scenario/scenario_result";
    }

    // 채팅 목록 창 (카톡 리스트 같은 화면)
    @GetMapping("/chat/window")
    public String openChatWindow() {
        return "chat/chat_window"; 
    }

    // 실제 채팅방 내부 (메시지 주고받는 화면)
    @GetMapping("/chat/room/{roomId}")
    public String openChatRoom(@PathVariable("roomId") String roomId, @RequestParam("name") String name, Model model) {
        model.addAttribute("roomId", roomId);
        model.addAttribute("roomName", name);
        return "chat/chat_room"; 
    }
}