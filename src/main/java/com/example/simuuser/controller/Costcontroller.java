package com.example.simuuser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
 
/**
 * 비용 & 수익성 분석 컨트롤러
 * URL:
 *   GET /cost        → cost.html (분석 입력 페이지)
 *   GET /cost/cost   → cost.html
 *   GET /cost/result → cost_result.html (분석 결과 페이지)
 */
@Controller
@RequestMapping("/cost")
public class Costcontroller {
 
    /**
     * 비용 & 수익성 분석 입력 페이지
     * - 메뉴/헤더에서 직접 진입: /cost 또는 /cost/cost
     * - 프로젝트 상세에서 진입:  /cost/cost?projectId={id}&from=detail
     */
    @GetMapping({"", "/", "/cost"})
    public String costPage() {
        return "cost/cost";
    }
 
    /**
     * 수익성 분석 결과 페이지
     * - sessionStorage에서 폼 데이터를 읽어 JS로 렌더링
     */
    @GetMapping("/result")
    public String costResultPage() {
        return "cost/cost_result";
    }
}