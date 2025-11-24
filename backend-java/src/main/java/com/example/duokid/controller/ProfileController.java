package com.example.duokid.controller;

import com.example.duokid.model.User;
import com.example.duokid.service.LessonProgressService;
import com.example.duokid.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final LessonProgressService lessonProgressService;
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/avatars/";
    private static final String UPLOAD_URL_PREFIX = "/uploads/avatars/";

    public ProfileController(UserService userService, LessonProgressService lessonProgressService) {
        this.userService = userService;
        this.lessonProgressService = lessonProgressService;
        
        // Create upload directory if it doesn't exist
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            System.err.println("Could not create upload directory: " + e.getMessage());
        }
    }

    @GetMapping
    public String showProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // Refresh user data
        user = userService.save(user);
        session.setAttribute("user", user);

        // Get user statistics
        int completedLessonsCount = lessonProgressService.getCompletedLessonsCount(user);
        
        model.addAttribute("user", user);
        model.addAttribute("completedLessonsCount", completedLessonsCount);
        return "profile";
    }

    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file ảnh!");
            return "redirect:/profile";
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            redirectAttributes.addFlashAttribute("error", "File phải là ảnh (JPG, PNG, GIF)!");
            return "redirect:/profile";
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            redirectAttributes.addFlashAttribute("error", "File ảnh không được vượt quá 5MB!");
            return "redirect:/profile";
        }

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "avatar_" + user.getId() + "_" + UUID.randomUUID().toString() + extension;
            
            // Save file
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Delete old avatar if it's a custom upload (not default avatar)
            String oldAvatar = user.getAvatar();
            if (oldAvatar != null && oldAvatar.startsWith("/uploads/avatars/")) {
                try {
                    Path oldFilePath = Paths.get("src/main/resources/static" + oldAvatar);
                    if (Files.exists(oldFilePath)) {
                        Files.delete(oldFilePath);
                    }
                } catch (IOException e) {
                    // Ignore if old file doesn't exist
                }
            }

            // Update user avatar
            user.setAvatar(UPLOAD_URL_PREFIX + filename);
            user = userService.save(user);
            session.setAttribute("user", user);

            redirectAttributes.addFlashAttribute("success", "Cập nhật avatar thành công!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi upload ảnh: " + e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/update-info")
    public String updateInfo(@RequestParam("displayName") String displayName,
                            @RequestParam(value = "gradeLevel", required = false) String gradeLevel,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        if (displayName == null || displayName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tên hiển thị không được để trống!");
            return "redirect:/profile";
        }

        user.setDisplayName(displayName.trim());
        if (gradeLevel != null && !gradeLevel.isEmpty()) {
            user.setGradeLevel(gradeLevel);
        }

        user = userService.save(user);
        session.setAttribute("user", user);

        redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        return "redirect:/profile";
    }
}

