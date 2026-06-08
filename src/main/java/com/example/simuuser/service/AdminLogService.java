package com.example.simuuser.service;

import com.example.simuuser.dto.AdminLogEntryResponse;
import com.example.simuuser.entity.AdminLog;
import com.example.simuuser.repository.AdminLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminLogService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AdminLogRepository adminLogRepository;

    public AdminLogService(AdminLogRepository adminLogRepository) {
        this.adminLogRepository = adminLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminLogEntryResponse> filterLogs(String query, String filter) {
        return adminLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(log -> matchesFilter(log, filter))
                .filter(log -> matchesQuery(log, query))
                .map(log -> new AdminLogEntryResponse(
                        log.getType(),
                        log.getTypeLabel(),
                        log.getMessage(),
                        log.getCreatedAt() == null ? null : log.getCreatedAt().toString()
                ))
                .toList();
    }

    @Transactional
    public void logUserChat(String message) {
        save(AdminLog.TYPE_USER_CHAT, "채팅", message);
    }

    @Transactional
    public void logApiError(String message) {
        save(AdminLog.TYPE_API_ERROR, "API 오류", message);
    }

    @Transactional
    public void logSystemError(String message) {
        save(AdminLog.TYPE_SYS_ERROR, "시스템 오류", message);
    }

    @Transactional(readOnly = true)
    public byte[] exportLogsCsv(String query, String filter) {
        List<AdminLogEntryResponse> logs = filterLogs(query, filter);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("구분,메시지,시간\n");

        for (AdminLogEntryResponse log : logs) {
            csv.append(csvEscape(log.getTypeLabel())).append(',')
                    .append(csvEscape(log.getMessage())).append(',')
                    .append(csvEscape(log.getCreatedAt())).append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public String exportFilename() {
        return "admin-logs-" + LocalDateTime.now().format(FILE_TIME) + ".csv";
    }

    private void save(String type, String typeLabel, String message) {
        String normalized = normalize(message);
        if (normalized == null) {
            return;
        }
        adminLogRepository.save(new AdminLog(type, typeLabel, normalized));
    }

    private boolean matchesFilter(AdminLog log, String filter) {
        return filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter) || log.getType().equalsIgnoreCase(filter);
    }

    private boolean matchesQuery(AdminLog log, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String keyword = query.trim().toLowerCase();
        return (defaultValue(log.getTypeLabel()) + " " + defaultValue(log.getMessage())).toLowerCase().contains(keyword);
    }

    private String csvEscape(String value) {
        String normalized = defaultValue(value).replace("\"", "\"\"");
        return "\"" + normalized + "\"";
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replaceAll("\\s+", " ").trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
