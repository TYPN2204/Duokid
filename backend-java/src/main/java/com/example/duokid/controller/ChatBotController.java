package com.example.duokid.controller;

import com.example.duokid.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/chatbot")
public class ChatBotController {

    /**
     * Display ChatBot page
     */
    @GetMapping
    public String chatbotPage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("user", currentUser);
        model.addAttribute("isAdmin", Boolean.TRUE.equals(currentUser.getIsAdmin()));

        return "chatbot";
    }
}
