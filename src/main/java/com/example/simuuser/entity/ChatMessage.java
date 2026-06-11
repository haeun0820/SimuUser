package com.example.simuuser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_room_created_at", columnList = "room_id, created_at"),
                @Index(name = "idx_chat_messages_room_id_id", columnList = "room_id, id")
        }
)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 20)
    private String messageType = "TEXT";

    @Column(length = 500)
    private String attachmentUrl;

    @Column(length = 255)
    private String attachmentName;

    @Column(length = 100)
    private String attachmentContentType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(ChatRoom room, AppUser sender, String content) {
        this.room = room;
        this.sender = sender;
        this.content = content;
        this.messageType = "TEXT";
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage(ChatRoom room, AppUser sender, String content, String messageType,
                       String attachmentUrl, String attachmentName, String attachmentContentType) {
        this.room = room;
        this.sender = sender;
        this.content = content;
        this.messageType = messageType;
        this.attachmentUrl = attachmentUrl;
        this.attachmentName = attachmentName;
        this.attachmentContentType = attachmentContentType;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ChatRoom getRoom() {
        return room;
    }

    public AppUser getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
