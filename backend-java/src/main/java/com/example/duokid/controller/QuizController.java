package com.example.duokid.controller;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.QuizQuestion;
import com.example.duokid.model.User;
import com.example.duokid.service.DailyGoalService;
import com.example.duokid.service.LessonProgressService;
import com.example.duokid.service.LessonService;
import com.example.duokid.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/quiz")
public class QuizController {

    private final LessonService lessonService;
    private final UserService userService;
    private final DailyGoalService dailyGoalService;
    private final LessonProgressService lessonProgressService;

    public QuizController(LessonService lessonService,
                          UserService userService,
                          DailyGoalService dailyGoalService,
                          LessonProgressService lessonProgressService) {
        this.lessonService = lessonService;
        this.userService = userService;
        this.dailyGoalService = dailyGoalService;
        this.lessonProgressService = lessonProgressService;
    }

    @GetMapping("/{lessonId}")
    public String showQuiz(@PathVariable Long lessonId,
                           HttpSession session,
                           Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        if (user.getHearts() <= 0) {
            return "redirect:/shop?reason=no-hearts";
        }

        Lesson lesson = lessonService.findById(lessonId);
        if (lesson == null) return "redirect:/lessons";

        List<QuizQuestion> questions = lessonService.getQuestionsByLesson(lesson);

        model.addAttribute("user", user);
        model.addAttribute("lesson", lesson);
        model.addAttribute("questions", questions);
        return "quiz";
    }

    @PostMapping("/{lessonId}")
    public String submitQuiz(@PathVariable Long lessonId,
                             @RequestParam Map<String, String> params,
                             HttpSession session,
                             Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        if (user.getHearts() <= 0) {
            return "redirect:/shop?reason=no-hearts";
        }

        Lesson lesson = lessonService.findById(lessonId);
        if (lesson == null) return "redirect:/lessons";

        List<QuizQuestion> questions = lessonService.getQuestionsByLesson(lesson);

        int total = questions.size();
        int correct = 0;

        for (QuizQuestion q : questions) {
            String key = "q_" + q.getId();
            String ans = params.get(key);
            if (ans != null && ans.equalsIgnoreCase(q.getCorrectOption())) {
                correct++;
            }
        }

        int wrong = total - correct;
        if (wrong < 0) wrong = 0;

        if (wrong > 0) {
            int newHearts = user.getHearts() - wrong;
            if (newHearts < 0) newHearts = 0;
            user.setHearts(newHearts);
            userService.save(user);
        }

        double ratio = total == 0 ? 0 : (double) correct / total;
        boolean passed = ratio >= 0.7;
        boolean perfect = (correct == total && total > 0); // Phải đúng 100% mới được vượt ải
        int score = (int) (ratio * 100);

        if (passed) {
            userService.addXpAndUpdateStreak(user, lesson.getXpReward());
            dailyGoalService.markQuizCompleted(user);
        }

        // Chỉ mark lesson completed khi đúng 100% (perfect score)
        LessonProgressService.GateRewardResult gateReward = null;
        if (perfect) {
            // Mark lesson as completed - chỉ khi đúng hết
            gateReward = lessonProgressService.markLessonCompleted(user, lesson, score);
            // Award gems (10 gems per lesson)
            user.setGems(user.getGems() + 10);
            userService.save(user);
        }

        session.setAttribute("user", user);

        model.addAttribute("user", user);
        model.addAttribute("lesson", lesson);
        model.addAttribute("questions", questions);
        model.addAttribute("correct", correct);
        model.addAttribute("total", total);
        model.addAttribute("passed", passed);
        model.addAttribute("perfect", perfect);
        model.addAttribute("wrong", wrong);
        model.addAttribute("outOfHearts", user.getHearts() <= 0);
        model.addAttribute("gateReward", gateReward);

        return "quiz_result";
    }
}
