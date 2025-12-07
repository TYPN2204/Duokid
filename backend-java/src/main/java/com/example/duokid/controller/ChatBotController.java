package com.example.duokid.controller;

import com.example.duokid.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.JsonNode;

@Controller
@RequestMapping("/chatbot")
public class ChatBotController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String PYTHON_SERVICE_URL = "http://localhost:5000";

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

    /**
     * Proxy chat request to Python service
     */
    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<?> chat(@RequestBody JsonNode request) {
        try {
            String response = restTemplate.postForObject(
                PYTHON_SERVICE_URL + "/api/chat",
                request,
                String.class
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("{\"error\":\"Python service unavailable\"}");
        }
    }

    /**
     * Proxy vocabulary lookup to Python service
     */
    @PostMapping("/api/vocabulary")
    @ResponseBody
    public ResponseEntity<?> vocabulary(@RequestBody JsonNode request) {
        try {
            String response = restTemplate.postForObject(
                PYTHON_SERVICE_URL + "/api/vocabulary",
                request,
                String.class
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("{\"error\":\"Python service unavailable\"}");
        }
    }
}
