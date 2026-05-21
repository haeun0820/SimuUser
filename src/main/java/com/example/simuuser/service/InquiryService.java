package com.example.simuuser.service;

import com.example.simuuser.dto.InquiryAnswerRequest;
import com.example.simuuser.dto.InquiryCreateRequest;
import com.example.simuuser.dto.InquiryResponse;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Inquiry;
import com.example.simuuser.repository.InquiryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final AppUserService appUserService;
    private final NotificationService notificationService;

    public InquiryService(
            InquiryRepository inquiryRepository,
            AppUserService appUserService,
            NotificationService notificationService
    ) {
        this.inquiryRepository = inquiryRepository;
        this.appUserService = appUserService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> findMine(Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        return inquiryRepository.findByUserOrderByCreatedAtDesc(currentUser).stream()
                .map(InquiryResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public InquiryResponse findMine(Long inquiryId, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        Inquiry inquiry = inquiryRepository.findByIdAndUser(inquiryId, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("문의 내역을 찾을 수 없습니다."));
        return new InquiryResponse(inquiry);
    }

    @Transactional
    public InquiryResponse create(InquiryCreateRequest request, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        validateCreate(request);

        Inquiry inquiry = new Inquiry(
                currentUser,
                request.getCategory().trim(),
                request.getTitle().trim(),
                request.getContent().trim()
        );
        return new InquiryResponse(inquiryRepository.save(inquiry));
    }

    @Transactional
    public InquiryResponse update(Long inquiryId, InquiryCreateRequest request, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        validateCreate(request);

        Inquiry inquiry = inquiryRepository.findByIdAndUser(inquiryId, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("문의 내역을 찾을 수 없습니다."));
        if (inquiry.isAnswered()) {
            throw new IllegalArgumentException("답변이 완료된 문의는 수정할 수 없습니다.");
        }

        inquiry.update(
                request.getCategory().trim(),
                request.getTitle().trim(),
                request.getContent().trim()
        );
        return new InquiryResponse(inquiry);
    }

    @Transactional
    public void delete(Long inquiryId, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        Inquiry inquiry = inquiryRepository.findByIdAndUser(inquiryId, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("문의 내역을 찾을 수 없습니다."));
        if (inquiry.isAnswered()) {
            throw new IllegalArgumentException("답변이 완료된 문의는 삭제할 수 없습니다.");
        }
        inquiryRepository.delete(inquiry);
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> findAllForAdmin() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(InquiryResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public InquiryResponse findForAdmin(Long inquiryId) {
        return new InquiryResponse(findInquiry(inquiryId));
    }

    @Transactional
    public InquiryResponse answer(Long inquiryId, InquiryAnswerRequest request, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        if (request == null || isBlank(request.getAnswer())) {
            throw new IllegalArgumentException("답변 내용을 입력해주세요.");
        }

        Inquiry inquiry = findInquiry(inquiryId);
        inquiry.answer(request.getAnswer().trim(), currentUser);
        notificationService.createInquiryAnswerNotification(inquiry);
        return new InquiryResponse(inquiry);
    }

    private Inquiry findInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의 내역을 찾을 수 없습니다."));
    }

    private void validateCreate(InquiryCreateRequest request) {
        if (request == null || isBlank(request.getCategory())) {
            throw new IllegalArgumentException("문의 유형을 선택해주세요.");
        }
        if (isBlank(request.getTitle())) {
            throw new IllegalArgumentException("문의 제목을 입력해주세요.");
        }
        if (isBlank(request.getContent())) {
            throw new IllegalArgumentException("문의 내용을 입력해주세요.");
        }
        if (request.getTitle().trim().length() > 120) {
            throw new IllegalArgumentException("문의 제목은 120자 이하로 입력해주세요.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
