package com.example.duokid.controller;

import com.example.duokid.model.Test;
import com.example.duokid.model.TestQuestion;
import com.example.duokid.model.User;
import com.example.duokid.service.TestService;
import com.example.duokid.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/test")
public class TestController {

    private final TestService testService;
    private final UserService userService;

    public TestController(TestService testService, UserService userService) {
        this.testService = testService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public String showTest(@PathVariable Long id,
                          HttpSession session,
                          Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // Check if user has enough hearts
        if (user.getHearts() <= 0) {
            return "redirect:/shop?reason=no-hearts";
        }

        Optional<Test> testOpt = testService.getTestRepo().findById(id);
        if (testOpt.isEmpty()) {
            return "redirect:/dashboard";
        }

        Test test = testOpt.get();
        List<TestQuestion> questions = testService.getTestQuestions(test);

        model.addAttribute("user", user);
        model.addAttribute("test", test);
        model.addAttribute("questions", questions);
        return "test";
    }

    @PostMapping("/{id}/submit")
    public String submitTest(@PathVariable Long id,
                            @RequestParam Map<String, String> params,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Optional<Test> testOpt = testService.getTestRepo().findById(id);
        if (testOpt.isEmpty()) {
            return "redirect:/dashboard";
        }

        Test test = testOpt.get();

        // Extract answers
        Map<Long, String> answers = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("answer_")) {
                try {
                    Long questionId = Long.parseLong(entry.getKey().substring(7));
                    answers.put(questionId, entry.getValue());
                } catch (NumberFormatException e) {
                    // Skip invalid question IDs
                }
            }
        }

        // Calculate score
        TestService.TestResult result = testService.calculateScore(test, answers);

        // Update user based on result
        if (result.isPassed()) {
            // Passed: reward XP and gems
            userService.addXpAndUpdateStreak(user, test.getXpReward());
            user.setGems(user.getGems() + test.getGemsReward());
            redirectAttributes.addFlashAttribute("message", 
                String.format("Chúc mừng! Bạn đã vượt qua bài kiểm tra với %d điểm! Nhận %d XP và %d Gems!", 
                    result.getScore(), test.getXpReward(), test.getGemsReward()));
        } else {
            // Failed: lose hearts
            int heartsLost = test.getHeartsLostOnFail();
            int newHearts = Math.max(0, user.getHearts() - heartsLost);
            user.setHearts(newHearts);
            redirectAttributes.addFlashAttribute("error", 
                String.format("Bạn đã không vượt qua bài kiểm tra (%d điểm). Mất %d tim. Hãy học lại và thử lại nhé!", 
                    result.getScore(), heartsLost));
            
            if (newHearts <= 0) {
                redirectAttributes.addFlashAttribute("outOfHearts", true);
            }
        }

        userService.save(user);
        session.setAttribute("user", user);

        redirectAttributes.addFlashAttribute("testScore", result.getScore());
        redirectAttributes.addFlashAttribute("testCorrect", result.getCorrect());
        redirectAttributes.addFlashAttribute("testTotal", result.getTotal());
        redirectAttributes.addFlashAttribute("testPassed", result.isPassed());

        return "redirect:/test/" + id + "/result";
    }

    @GetMapping("/{id}/result")
    public String testResult(@PathVariable Long id,
                            HttpSession session,
                            Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Optional<Test> testOpt = testService.getTestRepo().findById(id);
        if (testOpt.isEmpty()) {
            return "redirect:/dashboard";
        }

        Test test = testOpt.get();
        List<TestQuestion> questions = testService.getTestQuestions(test);

        model.addAttribute("user", user);
        model.addAttribute("test", test);
        model.addAttribute("questions", questions);
        return "test_result";
    }
}

