package com.example.simuuser.service;

import com.example.simuuser.dto.SignupRequest;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean isUserIdAvailable(String userId) {
        return !isBlank(userId) && !appUserRepository.existsByUserId(userId.trim());
    }

    @Transactional
    public void signup(SignupRequest request) {
        validate(request);

        AppUser appUser = new AppUser(
                request.getName().trim(),
                request.getUserId().trim(),
                passwordEncoder.encode(request.getPassword()),
                normalize(request.getEmail()),
                request.getPhone().trim(),
                request.getBirthDate(),
                request.getGender()
        );

        appUserRepository.save(appUser);
    }

    private void validate(SignupRequest request) {
        if (isBlank(request.getName())) {
            throw new IllegalArgumentException("이름을 입력하세요.");
        }
        if (isBlank(request.getUserId())) {
            throw new IllegalArgumentException("아이디를 입력하세요.");
        }
        if (appUserRepository.existsByUserId(request.getUserId().trim())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (isBlank(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호를 입력하세요.");
        }
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        if (isBlank(request.getEmail())) {
            throw new IllegalArgumentException("이메일을 입력하세요.");
        }
        if (isBlank(request.getPhone())) {
            throw new IllegalArgumentException("전화번호를 입력하세요.");
        }
        if (request.getBirthDate() == null) {
            throw new IllegalArgumentException("생년월일을 입력하세요.");
        }
        if (isBlank(request.getGender())) {
            throw new IllegalArgumentException("성별을 선택하세요.");
        }
        if (!request.isPrivacyAgree() || !request.isTermsAgree()) {
            throw new IllegalArgumentException("필수 약관에 동의하세요.");
        }
    }

    private String normalize(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
