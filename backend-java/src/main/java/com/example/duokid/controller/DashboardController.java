package com.example.duokid.controller;

import com.example.duokid.model.Achievement;
import com.example.duokid.model.DailyGoalProgress;
import com.example.duokid.model.Lesson;
import com.example.duokid.model.Test;
import com.example.duokid.model.User;
import com.example.duokid.model.UserLessonProgress;
import com.example.duokid.service.DailyGoalService;
import com.example.duokid.service.LessonProgressService;
import com.example.duokid.service.TestService;
import com.example.duokid.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private final DailyGoalService dailyGoalService;
    private final UserService userService;
    private final LessonProgressService lessonProgressService;
    private final TestService testService;

    public DashboardController(DailyGoalService dailyGoalService,
                               UserService userService,
                               LessonProgressService lessonProgressService,
                               TestService testService) {
        this.dailyGoalService = dailyGoalService;
        this.userService = userService;
        this.lessonProgressService = lessonProgressService;
        this.testService = testService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        user = userService.checkDailyHeartRefill(user);
        session.setAttribute("user", user);

        DailyGoalProgress progress = dailyGoalService.getOrCreateToday(user);
        List<Achievement> achievements = dailyGoalService.getAchievements(user);
        
        
        // Get lesson path and progress
        List<Lesson> lessonPath = lessonProgressService.getLessonPathForUser(user);
        Map<Long, UserLessonProgress> progressMap = lessonProgressService.getProgressMap(user);
        int completedLessonsCount = lessonProgressService.getCompletedLessonsCount(user);
        
        // Get available test for user's level
        String userLevel = user.getGradeLevel();
        java.util.Optional<Test> availableTest = testService.getTestForUser(user, userLevel);
        
        // Calculate daily quest progress
        int dailyQuestProgress = 0;
        int dailyQuestTarget = 10; // Earn 10 XP
        if (user.getXp() >= dailyQuestTarget) {
            dailyQuestProgress = 100;
        } else {
            dailyQuestProgress = (int) ((user.getXp() * 100.0) / dailyQuestTarget);
        }

        model.addAttribute("user", user);
        model.addAttribute("daily", progress);
        model.addAttribute("achievements", achievements);
        model.addAttribute("lessonPath", lessonPath);
        model.addAttribute("progressMap", progressMap);
        model.addAttribute("completedLessonsCount", completedLessonsCount);
        model.addAttribute("dailyQuestProgress", dailyQuestProgress);
        model.addAttribute("dailyQuestTarget", dailyQuestTarget);
        model.addAttribute("availableTest", availableTest.orElse(null));
        model.addAttribute("isAdmin", Boolean.TRUE.equals(user.getIsAdmin()));

        return "dashboard";
    }
}
