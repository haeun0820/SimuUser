package com.example.simuuser.controller;

import com.example.simuuser.dto.InquiryAnswerRequest;
import com.example.simuuser.dto.InquiryCreateRequest;
import com.example.simuuser.service.InquiryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @ResponseBody
    @GetMapping("/api/inquiries")
    public ResponseEntity<?> myInquiries(Authentication authentication) {
        return ResponseEntity.ok(inquiryService.findMine(authentication));
    }

    @ResponseBody
    @GetMapping("/api/inquiries/{inquiryId}")
    public ResponseEntity<?> myInquiry(@PathVariable Long inquiryId, Authentication authentication) {
        return ResponseEntity.ok(inquiryService.findMine(inquiryId, authentication));
    }

    @ResponseBody
    @PostMapping("/api/inquiries")
    public ResponseEntity<?> createInquiry(@RequestBody InquiryCreateRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(inquiryService.create(request, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @PutMapping("/api/inquiries/{inquiryId}")
    public ResponseEntity<?> updateInquiry(
            @PathVariable Long inquiryId,
            @RequestBody InquiryCreateRequest request,
            Authentication authentication
    ) {
        try {
            return ResponseEntity.ok(inquiryService.update(inquiryId, request, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @DeleteMapping("/api/inquiries/{inquiryId}")
    public ResponseEntity<?> deleteInquiry(@PathVariable Long inquiryId, Authentication authentication) {
        try {
            inquiryService.delete(inquiryId, authentication);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/api/admin/inquiries")
    public ResponseEntity<?> adminInquiries() {
        return ResponseEntity.ok(inquiryService.findAllForAdmin());
    }

    @ResponseBody
    @GetMapping("/api/admin/inquiries/{inquiryId}")
    public ResponseEntity<?> adminInquiry(@PathVariable Long inquiryId) {
        try {
            return ResponseEntity.ok(inquiryService.findForAdmin(inquiryId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @PutMapping("/api/admin/inquiries/{inquiryId}/answer")
    public ResponseEntity<?> answerInquiry(
            @PathVariable Long inquiryId,
            @RequestBody InquiryAnswerRequest request,
            Authentication authentication
    ) {
        try {
            return ResponseEntity.ok(inquiryService.answer(inquiryId, request, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
