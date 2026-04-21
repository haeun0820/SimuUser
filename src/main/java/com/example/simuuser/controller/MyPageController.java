package com.example.simuuser.controller;

import com.example.simuuser.dto.ProfileResponse;
import com.example.simuuser.dto.ProfileUpdateRequest;
import com.example.simuuser.entity.AppUser;
import com.example.simuuser.service.AppUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Controller
public class MyPageController {

    private static final long MAX_PROFILE_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Path PROFILE_IMAGE_DIRECTORY = Paths.get("src/main/resources/uploads/profile-images")
            .toAbsolutePath()
            .normalize();

    private final PasswordEncoder passwordEncoder;
    private final AppUserService appUserService;

    public MyPageController(PasswordEncoder passwordEncoder, AppUserService appUserService) {
        this.passwordEncoder = passwordEncoder;
        this.appUserService = appUserService;
    }

    @GetMapping("/mypage")
    public String mypage() {
        return "mypage/mypage";
    }

    @ResponseBody
    @GetMapping("/api/me")
    public ProfileResponse me(Authentication authentication) {
        return new ProfileResponse(appUserService.getCurrentUser(authentication));
    }

    @ResponseBody
    @PutMapping("/api/me")
    @Transactional
    public ResponseEntity<?> updateMe(@RequestBody ProfileUpdateRequest request, Authentication authentication) {
        try {
            AppUser user = appUserService.getCurrentUser(authentication);
            validateProfile(request);

            user.updateProfile(
                    request.getName().trim(),
                    request.getPhone().trim(),
                    request.getBirthDate(),
                    request.getGender().trim()
            );

            updatePasswordIfRequested(user, request);

            return ResponseEntity.ok(new ProfileResponse(user));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @PostMapping("/api/me/profile-image")
    @Transactional
    public ResponseEntity<?> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        try {
            AppUser user = appUserService.getCurrentUser(authentication);
            validateProfileImageUpload(user, file);

            Files.createDirectories(PROFILE_IMAGE_DIRECTORY);
            deletePreviousUploadedProfileImage(user.getProfileImage());

            String extension = extractExtension(file.getOriginalFilename());
            String storedFileName = "user-" + user.getId() + "-" + UUID.randomUUID() + extension;
            Path targetPath = PROFILE_IMAGE_DIRECTORY.resolve(storedFileName).normalize();

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            user.setProfileImage("/uploads/profile-images/" + storedFileName);

            return ResponseEntity.ok(new ProfileResponse(user));
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private void updatePasswordIfRequested(AppUser user, ProfileUpdateRequest request) {
        boolean passwordChangeRequested =
                !isBlank(request.getCurrentPassword()) ||
                !isBlank(request.getNewPassword()) ||
                !isBlank(request.getNewPasswordConfirm());

        if (!passwordChangeRequested) {
            return;
        }

        if (!isLocalUser(user)) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }

        if (isBlank(request.getCurrentPassword())) {
            throw new IllegalArgumentException("현재 비밀번호를 입력해주세요.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (isBlank(request.getNewPassword())) {
            throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호 확인이 일치하지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    private void validateProfile(ProfileUpdateRequest request) {
        if (request == null || isBlank(request.getName())) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }
        if (isBlank(request.getPhone())) {
            throw new IllegalArgumentException("전화번호를 입력해주세요.");
        }
        if (request.getBirthDate() == null) {
            throw new IllegalArgumentException("생년월일을 입력해주세요.");
        }
        if (isBlank(request.getGender())) {
            throw new IllegalArgumentException("성별을 선택해주세요.");
        }
    }

    private void validateProfileImageUpload(AppUser user, MultipartFile file) {
        if (!isProfileImageEditable(user)) {
            throw new IllegalArgumentException("구글 로그인 계정은 프로필 이미지를 변경할 수 없습니다.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지 파일을 선택해주세요.");
        }
        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new IllegalArgumentException("프로필 이미지는 5MB 이하만 업로드할 수 있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (extension.isBlank()) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }
    }

    private void deletePreviousUploadedProfileImage(String profileImage) throws IOException {
        if (profileImage == null || !profileImage.startsWith("/uploads/profile-images/")) {
            return;
        }

        String fileName = profileImage.substring("/uploads/profile-images/".length());
        if (fileName.isBlank()) {
            return;
        }

        Path previousFile = PROFILE_IMAGE_DIRECTORY.resolve(fileName).normalize();
        if (previousFile.startsWith(PROFILE_IMAGE_DIRECTORY)) {
            Files.deleteIfExists(previousFile);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }

        String extension = originalFilename.substring(dotIndex).toLowerCase();
        return switch (extension) {
            case ".jpg", ".jpeg", ".png", ".gif", ".webp" -> extension;
            default -> "";
        };
    }

    private boolean isProfileImageEditable(AppUser user) {
        return !"GOOGLE".equalsIgnoreCase(user.getProvider());
    }

    private boolean isLocalUser(AppUser user) {
        return user.getProvider() == null || "LOCAL".equalsIgnoreCase(user.getProvider());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
