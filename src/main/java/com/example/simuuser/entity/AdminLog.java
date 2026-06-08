package com.example.simuuser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_logs")
public class AdminLog {

    public static final String TYPE_API_ERROR = "api-error";
    public static final String TYPE_SYS_ERROR = "sys-error";
    public static final String TYPE_USER_CHAT = "user-chat";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 50)
    private String typeLabel;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AdminLog() {
    }

    public AdminLog(String type, String typeLabel, String message) {
        this.type = type;
        this.typeLabel = typeLabel;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
