package com.example.duokid.controller;

import com.example.duokid.model.User;
import com.example.duokid.model.VocabGameScore;
import com.example.duokid.service.UserService;
import com.example.duokid.service.VocabGameService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MiniGameController {

    private static final int XP_PER_CORRECT = 5;

    private final VocabGameService vocabGameService;
    private final UserService userService;

    public MiniGameController(VocabGameService vocabGameService,
                              UserService userService) {
        this.vocabGameService = vocabGameService;
        this.userService = userService;
    }

    @GetMapping("/minigame")
    public String showMiniGame(@RequestParam(required = false) String category,
                               @RequestParam(required = false) String mode,
                               @RequestParam(required = false) String gameType,
                               HttpSession session,
                               Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // If gameType is "puzzle", show word puzzle game
        if ("puzzle".equals(gameType) && category != null) {
            model.addAttribute("user", user);
            model.addAttribute("category", category);
            model.addAttribute("vocabItems", vocabGameService.getVocabItemsByCategory(category));
            
            // Convert vocab items to JSON string for JavaScript
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String vocabItemsJson = objectMapper.writeValueAsString(vocabGameService.getVocabItemsByCategory(category));
                model.addAttribute("vocabItemsJson", vocabItemsJson);
            } catch (Exception e) {
                model.addAttribute("vocabItemsJson", "[]");
            }
            
            return "word-puzzle-game";
        }

        // If category is specified, show vocab game interface
        if (category != null) {
            model.addAttribute("user", user);
            model.addAttribute("category", category);
            model.addAttribute("vocabItems", vocabGameService.getVocabItemsByCategory(category));
            
            // Convert vocab items to JSON string for JavaScript
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String vocabItemsJson = objectMapper.writeValueAsString(vocabGameService.getVocabItemsByCategory(category));
                model.addAttribute("vocabItemsJson", vocabItemsJson);
            } catch (Exception e) {
                model.addAttribute("vocabItemsJson", "[]");
            }
            
            return "vocab-game";
        }

        // Otherwise show traditional quiz game
        model.addAttribute("user", user);
        model.addAttribute("questions", vocabGameService.generateRound());
        model.addAttribute("leaderboard", vocabGameService.getLeaderboard());
        return "minigame";
    }

    @PostMapping("/minigame/submit")
    public String submitGame(@RequestParam("questionIds") List<Long> questionIds,
                             @RequestParam Map<String, String> params,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Map<Long, String> answers = new HashMap<>();
        for (Long qId : questionIds) {
            String key = "answer_" + qId;
            answers.put(qId, params.get(key));
        }

        int points = vocabGameService.calculatePoints(questionIds, answers);
        long correct = vocabGameService.countCorrect(questionIds, answers);
        int total = questionIds.size();

        int xpReward = (int) (correct * XP_PER_CORRECT);
        if (xpReward > 0) {
            user = userService.addXp(user, xpReward);
            session.setAttribute("user", user);
        }

        VocabGameScore score = vocabGameService.recordScore(user, points);

        redirectAttributes.addFlashAttribute("gamePoints", points);
        redirectAttributes.addFlashAttribute("gameCorrect", correct);
        redirectAttributes.addFlashAttribute("gameTotal", total);
        redirectAttributes.addFlashAttribute("gameXp", xpReward);
        redirectAttributes.addFlashAttribute("gameBest", score.getBestRoundScore());

        return "redirect:/minigame";
    }

    @PostMapping("/minigame/image-game-result")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitImageGameResult(
            @RequestBody Map<String, Object> gameResult,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        // Get game results
        int score = ((Number) gameResult.getOrDefault("score", 0)).intValue();
        int correct = ((Number) gameResult.getOrDefault("correct", 0)).intValue();
        int heartsLost = ((Number) gameResult.getOrDefault("heartsLost", 0)).intValue();
        int gemsEarned = ((Number) gameResult.getOrDefault("gemsEarned", 0)).intValue();

        // Update hearts
        if (heartsLost > 0) {
            int newHearts = Math.max(0, user.getHearts() - heartsLost);
            user.setHearts(newHearts);
        }

        // Update gems
        if (gemsEarned > 0) {
            user.setGems(user.getGems() + gemsEarned);
        }

        // Calculate and add XP (5 XP per correct answer)
        int xpReward = correct * XP_PER_CORRECT;
        if (xpReward > 0) {
            user = userService.addXp(user, xpReward);
        }

        // Save user
        userService.save(user);
        session.setAttribute("user", user);

        // Record score
        vocabGameService.recordScore(user, score);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("hearts", user.getHearts());
        response.put("gems", user.getGems());
        response.put("xp", user.getXp());
        response.put("heartsLost", heartsLost);
        response.put("gemsEarned", gemsEarned);
        response.put("xpEarned", xpReward);

        return ResponseEntity.ok(response);
    }
}

