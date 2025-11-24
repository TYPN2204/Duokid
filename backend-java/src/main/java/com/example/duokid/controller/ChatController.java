package com.example.duokid.controller;

import com.example.duokid.model.Message;
import com.example.duokid.model.User;
import com.example.duokid.repo.MessageRepository;
import com.example.duokid.repo.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/chat/";
    private static final String UPLOAD_URL_PREFIX = "/uploads/chat/";

    public ChatController(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        
        // Create upload directory if it doesn't exist
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            System.err.println("Could not create chat upload directory: " + e.getMessage());
        }
    }

    @GetMapping
    public String chatPage(HttpSession session, Model model,
                          @RequestParam(required = false) Long withUserId,
                          @ModelAttribute("error") String error,
                          @ModelAttribute("success") String success) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        // Lấy danh sách người dùng để chat (trừ admin và chính mình)
        List<User> allUsers = userRepository.findAll().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsAdmin()) && !u.getId().equals(currentUser.getId()))
                .toList();
        model.addAttribute("allUsers", allUsers);

        // Lấy danh sách người đã chat với user hiện tại
        List<Message> userMessages = messageRepository.findMessagesByUser(currentUser);
        List<User> chatPartners = userMessages.stream()
                .map(m -> m.getSender().getId().equals(currentUser.getId()) ? m.getReceiver() : m.getSender())
                .distinct()
                .toList();
        model.addAttribute("chatPartners", chatPartners);

        // Đếm tin nhắn chưa đọc
        Long unreadCount = messageRepository.countUnreadMessages(currentUser);
        model.addAttribute("unreadCount", unreadCount);

        // Nếu có chọn người chat, lấy tin nhắn
        if (withUserId != null) {
            Optional<User> partnerOpt = userRepository.findById(withUserId);
            if (partnerOpt.isPresent()) {
                User partner = partnerOpt.get();
                List<Message> messages = messageRepository.findConversationBetweenUsers(currentUser, partner);
                
                // Đánh dấu tin nhắn là đã đọc
                messages.stream()
                        .filter(m -> m.getReceiver().getId().equals(currentUser.getId()) && !m.getIsRead())
                        .forEach(m -> {
                            m.setIsRead(true);
                            messageRepository.save(m);
                        });
                
                model.addAttribute("partner", partner);
                model.addAttribute("messages", messages);
            }
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("isAdmin", Boolean.TRUE.equals(currentUser.getIsAdmin()));
        
        // Add error/success messages from flash attributes
        if (error != null && !error.isEmpty()) {
            model.addAttribute("error", error);
        }
        if (success != null && !success.isEmpty()) {
            model.addAttribute("success", success);
        }
        
        return "chat";
    }

    @PostMapping("/send")
    public String sendMessage(@RequestParam Long receiverId,
                             @RequestParam(required = false) String content,
                             @RequestParam(required = false) MultipartFile image,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User sender = (User) session.getAttribute("user");
        if (sender == null) return "redirect:/login";

        // Phải có ít nhất content hoặc image
        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasImage = image != null && !image.isEmpty();
        
        if (!hasContent && !hasImage) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập tin nhắn hoặc chọn ảnh!");
            return "redirect:/chat?withUserId=" + receiverId;
        }

        if (receiverId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn người nhận!");
            return "redirect:/chat";
        }
        
        Optional<User> receiverOpt = userRepository.findById(receiverId);
        if (receiverOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Người nhận không tồn tại!");
            return "redirect:/chat";
        }

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiverOpt.get());
        message.setContent(hasContent ? (content != null ? content.trim() : null) : null);
        message.setSentAt(LocalDateTime.now());
        message.setIsRead(false);

        // Xử lý upload ảnh
        if (hasImage && image != null) {
            try {
                // Validate file type
                String contentType = image.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "File phải là ảnh (JPG, PNG, GIF)!");
                    return "redirect:/chat?withUserId=" + receiverId;
                }

                // Validate file size (max 5MB)
                long fileSize = image.getSize();
                if (fileSize > 5 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "File ảnh không được vượt quá 5MB!");
                    return "redirect:/chat?withUserId=" + receiverId;
                }

                // Generate unique filename
                String originalFilename = image.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String filename = "chat_" + sender.getId() + "_" + UUID.randomUUID().toString() + extension;
                
                // Save file
                Path uploadPath = Paths.get(UPLOAD_DIR);
                Path filePath = uploadPath.resolve(filename);
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                message.setImageUrl(UPLOAD_URL_PREFIX + filename);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi khi upload ảnh: " + e.getMessage());
                return "redirect:/chat?withUserId=" + receiverId;
            }
        }

        messageRepository.save(message);

        redirectAttributes.addFlashAttribute("success", "Tin nhắn đã được gửi!");
        return "redirect:/chat?withUserId=" + receiverId;
    }

    @GetMapping("/api/messages")
    @ResponseBody
    public List<Message> getMessages(@RequestParam(required = false) Long withUserId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || withUserId == null) return List.of();

        Optional<User> partnerOpt = userRepository.findById(withUserId);
        if (partnerOpt.isEmpty()) return List.of();

        return messageRepository.findConversationBetweenUsers(currentUser, partnerOpt.get());
    }

    @GetMapping("/api/unread")
    @ResponseBody
    public Long getUnreadCount(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return 0L;
        return messageRepository.countUnreadMessages(currentUser);
    }

    // Common emojis for picker
    public static List<String> getCommonEmojis() {
        return List.of(
            "😊", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
            "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛",
            "😜", "😝", "😎", "🤓", "🧐", "🤗", "🤔", "🤭",
            "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "👏",
            "🙌", "👐", "🤲", "🙏", "💪", "🦾", "🦿", "🦵",
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
            "⭐", "🌟", "✨", "💫", "💥", "🔥", "💯", "🎉",
            "🎊", "🎈", "🎁", "🏆", "🥇", "🥈", "🥉", "🏅"
        );
    }
}

