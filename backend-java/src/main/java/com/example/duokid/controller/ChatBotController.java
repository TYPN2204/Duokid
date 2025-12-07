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
import com.example.duokid.service.LessonService;
import com.example.duokid.model.Lesson;
import com.example.duokid.model.Vocabulary;
import java.util.List;

@Controller
@RequestMapping("/chatbot")
public class ChatBotController {

    private final RestTemplate restTemplate = new RestTemplate();
    // Use Colab public endpoint for ChatBot
    private final String COLAB_SERVICE_URL = "https://elongative-pyrographic-kendrick.ngrok-free.dev";

    private final VocabularyService vocabularyService;
    private final LessonService lessonService;

    public ChatBotController(VocabularyService vocabularyService, LessonService lessonService) {
        this.vocabularyService = vocabularyService;
        this.lessonService = lessonService;
    }

    // Small in-memory fallback for very common words (used if DB search misses)
    private static final java.util.Map<String, String[]> FALLBACK_VOCAB = new java.util.HashMap<>() {{
        put("father", new String[]{"/ˈfɑː.ðər/", "Cha, bố", "My father works at the bank."});
        put("mother", new String[]{"/ˈmʌð.ər/", "Mẹ", "My mother cooks delicious food."});
        put("pen", new String[]{"/pen/", "Bút", "I write with a blue pen."});
        put("pencil", new String[]{"/ˈpen.səl/", "Bút chì", "She draws with a pencil."});
        put("teacher", new String[]{"/ˈtiː.tʃər/", "Giáo viên", "The teacher explains the lesson."});
        put("breakfast", new String[]{"/ˈbrek.fəst/", "Bữa sáng", "I eat breakfast every morning."});
        put("school", new String[]{"/skuːl/", "Trường học", "I go to school every day."});
        put("book", new String[]{"/bʊk/", "Cuốn sách", "This book is very interesting."});
        put("apple", new String[]{"/ˈæp.əl/", "Quả táo", "I eat an apple every day."});
        put("cat", new String[]{"/kæt/", "Con mèo", "My cat is very cute."});
        put("dog", new String[]{"/dɔɡ/", "Con chó", "The dog runs in the park."});
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
                System.out.println("VOCAB FALLBACK HIT: " + key);
                String[] d = FALLBACK_VOCAB.get(key);
                String jsonF = String.format(
                    "{\"word\":\"%s\",\"phonetic\":\"%s\",\"meaning\":\"%s\",\"example\":\"%s\"}",
                    key, d[0], d[1], d[2]
                );
                return ResponseEntity.ok(jsonF);
            }

                List<Vocabulary> results = vocabularyService.searchVocabularies(word.trim());
                if (results != null && !results.isEmpty()) {
                // Prefer exact match on english word or whole-word matches in fields
                String kw = word.trim();
                java.util.regex.Pattern whole = java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(kw) + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE);

                // First try exact englishWord match
                Vocabulary v = results.stream()
                    .filter(x -> x.getEnglishWord() != null && x.getEnglishWord().equalsIgnoreCase(kw))
                    .findFirst()
                    .orElse(null);

                // Then try whole-word match across englishWord, vietnameseMeaning, exampleSentence
                if (v == null) {
                    v = results.stream()
                        .filter(x -> (x.getEnglishWord() != null && whole.matcher(x.getEnglishWord()).find())
                            || (x.getVietnameseMeaning() != null && whole.matcher(x.getVietnameseMeaning()).find())
                            || (x.getExampleSentence() != null && whole.matcher(x.getExampleSentence()).find()))
                        .findFirst().orElse(null);
                }

                // Fallback to first result only if it contains a whole-word match
                if (v == null) {
                    Vocabulary candidate = results.get(0);
                    boolean candidateMatches = (candidate.getEnglishWord() != null && whole.matcher(candidate.getEnglishWord()).find())
                            || (candidate.getVietnameseMeaning() != null && whole.matcher(candidate.getVietnameseMeaning()).find())
                            || (candidate.getExampleSentence() != null && whole.matcher(candidate.getExampleSentence()).find());
                    if (candidateMatches) v = candidate;
                }
                // Build a simple JSON response matching Python format
                String json = String.format(
                    "{\"word\":\"%s\",\"phonetic\":\"%s\",\"meaning\":\"%s\",\"example\":\"%s\"}",
                    v.getEnglishWord(), v.getIpaAmerican() == null ? "" : v.getIpaAmerican(),
                    v.getVietnameseMeaning() == null ? "" : v.getVietnameseMeaning(),
                    v.getExampleSentence() == null ? "" : v.getExampleSentence()
                );
                return ResponseEntity.ok(json);
            }

            // Not found in DB/fallback — try searching lesson contentHtml
            System.out.println("VOCAB NOT FOUND IN DB: " + word);
            try {
                java.util.List<Lesson> lessons = lessonService.findAll();
                // More robust lesson HTML lookup: find <b>word</b> then extract surrounding <li> text
                for (Lesson lesson : lessons) {
                    String html = lesson.getContentHtml();
                    if (html == null) continue;
                    String lowered = html.toLowerCase();
                    String token = "<b>" + word.toLowerCase() + "</b>";
                    int idx = lowered.indexOf(token);
                    if (idx == -1) continue;

                    // find enclosing <li> ... </li>
                    int liStart = lowered.lastIndexOf("<li", idx);
                    int liEnd = lowered.indexOf("</li>", idx);
                    if (liStart == -1 || liEnd == -1) continue;
                    String li = html.substring(liStart, liEnd);

                    // try to split by common separators (–, -, —)
                    String meaning = null;
                    if (li.contains("–")) {
                        meaning = li.substring(li.indexOf("–") + 1).replaceAll("<.*?>", "").trim();
                    } else if (li.contains(" - ")) {
                        meaning = li.substring(li.indexOf(" - ") + 3).replaceAll("<.*?>", "").trim();
                    } else if (li.contains("—")) {
                        meaning = li.substring(li.indexOf("—") + 1).replaceAll("<.*?>", "").trim();
                    } else {
                        // fallback: remove tags and take text after closing </b>
                        int bClose = li.toLowerCase().indexOf("</b>");
                        if (bClose != -1 && bClose + 4 < li.length()) {
                            meaning = li.substring(bClose + 4).replaceAll("<.*?>", "").trim();
                        }
                    }

                    if (meaning != null && !meaning.isEmpty()) {
                        String jsonLesson = String.format(
                            "{\"word\":\"%s\",\"phonetic\":\"/word/\",\"meaning\":\"%s\",\"example\":\"From lesson: %s\"}",
                            word, meaning.replaceAll("\"","'"), lesson.getTitle().replaceAll("\"","'")
                        );
                        return ResponseEntity.ok(jsonLesson);
                    }
                }
            } catch (Exception ex) {
                // ignore and return not found
                System.err.println("Lesson search failed: " + ex.getMessage());
            }

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
