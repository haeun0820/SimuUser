package com.example.simuuser.service;

import com.example.simuuser.dto.AdminUserSummaryResponse;
import com.example.simuuser.repository.AppUserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminUserService {

    private static final DateTimeFormatter EXPORT_FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AppUserRepository appUserRepository;

    public AdminUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummaryResponse> findAllUsers() {
        return findUsers(null, null);
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummaryResponse> findUsers(String query, String loginMethod) {
        return appUserRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(user -> !"ADMIN".equalsIgnoreCase(user.getRole()))
                .map(AdminUserSummaryResponse::new)
                .filter(user -> matchesQuery(user, query))
                .filter(user -> matchesLoginMethod(user, loginMethod))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportUsers(String query, String loginMethod) {
        List<AdminUserSummaryResponse> users = findUsers(query, loginMethod);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Users");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("이름");
            header.createCell(1).setCellValue("사용자 ID");
            header.createCell(2).setCellValue("이메일");
            header.createCell(3).setCellValue("로그인 방식");
            header.createCell(4).setCellValue("가입일");

            for (int i = 0; i < users.size(); i++) {
                AdminUserSummaryResponse user = users.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(defaultValue(user.getName()));
                row.createCell(1).setCellValue(defaultValue(user.getUserId()));
                row.createCell(2).setCellValue(defaultValue(user.getEmail()));
                row.createCell(3).setCellValue(loginMethodLabel(user.getLoginMethod()));
                row.createCell(4).setCellValue(defaultValue(user.getCreatedAt()));
            }

            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("사용자 목록 파일 생성에 실패했습니다.", e);
        }
    }

    public String exportFilename() {
        return "admin-users-" + LocalDateTime.now().format(EXPORT_FILE_TIME) + ".xlsx";
    }

    private boolean matchesQuery(AdminUserSummaryResponse user, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String keyword = query.trim().toLowerCase();
        String searchable = String.join(" ",
                defaultValue(user.getName()),
                defaultValue(user.getUserId()),
                defaultValue(user.getEmail())
        ).toLowerCase();
        return searchable.contains(keyword);
    }

    private boolean matchesLoginMethod(AdminUserSummaryResponse user, String loginMethod) {
        if (loginMethod == null || loginMethod.isBlank() || "all".equalsIgnoreCase(loginMethod)) {
            return true;
        }
        return loginMethod.trim().equalsIgnoreCase(user.getLoginMethod());
    }

    private String loginMethodLabel(String loginMethod) {
        return switch (defaultValue(loginMethod).toLowerCase()) {
            case "google" -> "Google";
            case "naver" -> "Naver";
            case "kakao" -> "Kakao";
            case "apple" -> "Apple";
            default -> "일반 ID";
        };
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
