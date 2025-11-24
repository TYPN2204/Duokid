package com.example.duokid.repo;

import com.example.duokid.model.Message;
import com.example.duokid.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // Lấy tất cả tin nhắn giữa 2 người dùng
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender = :user1 AND m.receiver = :user2) OR " +
           "(m.sender = :user2 AND m.receiver = :user1) " +
           "ORDER BY m.sentAt ASC")
    List<Message> findConversationBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
    
    // Lấy tất cả tin nhắn liên quan đến user (để lấy danh sách partners)
    @Query("SELECT m FROM Message m WHERE m.sender = :user OR m.receiver = :user ORDER BY m.sentAt DESC")
    List<Message> findMessagesByUser(@Param("user") User user);
    
    // Đếm số tin nhắn chưa đọc
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :user AND m.isRead = false")
    Long countUnreadMessages(@Param("user") User user);
    
    // Lấy tin nhắn chưa đọc
    List<Message> findByReceiverAndIsReadFalseOrderBySentAtDesc(User receiver);
}

