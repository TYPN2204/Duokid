package com.example.duokid.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User sender; // Người gửi

    @ManyToOne(optional = false)
    private User receiver; // Người nhận

    @Column(length = 2000, nullable = true)
    private String content; // Nội dung tin nhắn (có thể null nếu chỉ gửi ảnh)

    @Column(length = 500, nullable = true)
    private String imageUrl; // URL ảnh đính kèm (nếu có)

    private LocalDateTime sentAt; // Thời gian gửi

    private Boolean isRead = false; // Đã đọc chưa

    // Chat room type: "PRIVATE" (1-1) or "PUBLIC" (group chat)
    private String chatType = "PRIVATE";

    // Reference to lesson/test if sharing (optional)
    private Long lessonId;
    private Long testId;

    public Message() {
        this.sentAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public Boolean getIsRead() { return isRead != null ? isRead : false; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public Long getTestId() { return testId; }
    public void setTestId(Long testId) { this.testId = testId; }
}

