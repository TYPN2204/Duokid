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
import com.example.duokid.service.VocabularyService;
import com.example.duokid.model.Vocabulary;
import java.util.List;

@Controller
@RequestMapping("/chatbot")
public class ChatBotController {

    private final RestTemplate restTemplate = new RestTemplate();
    // Use Colab public endpoint for ChatBot
    private final String COLAB_SERVICE_URL = "https://elongative-pyrographic-kendrick.ngrok-free.dev";

    private final VocabularyService vocabularyService;

    public ChatBotController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    // Small in-memory fallback for very common words (used if DB search misses)
    private static final java.util.Map<String, String[]> FALLBACK_VOCAB = new java.util.HashMap<>() {{
        put("father", new String[]{"/ˈfɑː.ðər/", "Cha, bố", "My father works at the bank."});
        put("mother", new String[]{"/ˈmʌð.ər/", "Mẹ", "My mother cooks delicious food."});
        put("pen", new String[]{"/pen/", "Bút", "I write with a blue pen."});
        put("pencil", new String[]{"/ˈpen.səl/", "Bút chì", "She draws with a pencil."});
        put("teacher", new String[]{"/ˈtiː.tʃər/", "Giáo viên", "The teacher explains the lesson."});
    }};

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
            // Try Colab first
            String response = null;
            try {
                response = restTemplate.postForObject(
                    COLAB_SERVICE_URL + "/api/chat",
                    request,
                    String.class
                );
            } catch (Exception ex) {
                response = null;
            }

            // If Colab returned nothing or a trivial reply like "English", fallback to local Python service
            if (response == null || response.trim().isEmpty() || response.contains("\"reply\":\"English\"") || response.trim().equalsIgnoreCase("English")) {
                try {
                    String local = restTemplate.postForObject(
                        "http://localhost:5000/api/chat",
                        request,
                        String.class
                    );
                    if (local != null && !local.trim().isEmpty()) {
                        return ResponseEntity.ok(local);
                    }
                } catch (Exception ex) {
                    // ignore and fall through
                }
            }

            if (response != null) {
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("{\"error\":\"Chat service unavailable\"}");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("{\"error\":\"Chat service error\"}");
        }
    }

    /**
     * Proxy vocabulary lookup to Python service
     */
    @PostMapping("/api/vocabulary")
    @ResponseBody
    public ResponseEntity<?> vocabulary(@RequestBody JsonNode request) {
        try {
            // Use internal VocabularyService to look up words (more complete DB)
            String word = request.has("word") ? request.get("word").asText("") : "";
            if (word == null || word.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("{\"error\":\"Word is required\"}");
            }
            // Check in-memory fallback first
            String key = word.trim().toLowerCase();
            if (FALLBACK_VOCAB.containsKey(key)) {
                String[] d = FALLBACK_VOCAB.get(key);
                String jsonF = String.format(
                    "{\"word\":\"%s\",\"phonetic\":\"%s\",\"meaning\":\"%s\",\"example\":\"%s\"}",
                    key, d[0], d[1], d[2]
                );
                return ResponseEntity.ok(jsonF);
            }

            List<Vocabulary> results = vocabularyService.searchVocabularies(word.trim());
            if (results != null && !results.isEmpty()) {
                Vocabulary v = results.get(0);
                // Build a simple JSON response matching Python format
                String json = String.format(
                    "{\"word\":\"%s\",\"phonetic\":\"%s\",\"meaning\":\"%s\",\"example\":\"%s\"}",
                    v.getEnglishWord(), v.getIpaAmerican() == null ? "" : v.getIpaAmerican(),
                    v.getVietnameseMeaning() == null ? "" : v.getVietnameseMeaning(),
                    v.getExampleSentence() == null ? "" : v.getExampleSentence()
                );
                return ResponseEntity.ok(json);
            }

            // Not found
            String notFound = String.format(
                "{\"word\":\"%s\",\"phonetic\":\"/word/\",\"meaning\":\"Sorry, I don't have this word in my vocabulary database yet. Try another word!\",\"example\":\"Keep learning new words!\"}",
                word
            );
            return ResponseEntity.ok(notFound);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("{\"error\":\"Vocabulary lookup failed\"}");
        }
    }
}
