package com.example.duokid.controller;

import com.example.duokid.model.User;
import com.example.duokid.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        try {
            return "login";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "login";
        }
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {
        User user = userService.login(email, password);
        if (user == null) {
            model.addAttribute("error", "Sai email hoặc mật khẩu");
            return "login";
        }
        user = userService.checkDailyHeartRefill(user);
        session.setAttribute("user", user);
        
        // Tất cả user đều vào dashboard, admin có thể chuyển sang trang admin từ sidebar
        return "redirect:/dashboard";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam("gradeLevel") String gradeLevel,
                             Model model) {
        try {
            userService.register(email, password, name, gradeLevel);
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Email đã tồn tại");
            model.addAttribute("selectedGrade", gradeLevel);
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
