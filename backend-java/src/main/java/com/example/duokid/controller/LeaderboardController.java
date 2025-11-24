package com.example.duokid.controller;

import com.example.duokid.model.User;
import com.example.duokid.service.LeaderboardService;
import com.example.duokid.service.LessonProgressService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final LessonProgressService lessonProgressService;

    public LeaderboardController(LeaderboardService leaderboardService,
                                LessonProgressService lessonProgressService) {
        this.leaderboardService = leaderboardService;
        this.lessonProgressService = lessonProgressService;
    }

    @GetMapping("/leaderboard")
    public String leaderboard(HttpSession session, Model model) {
        User current = (User) session.getAttribute("user");
        if (current == null) return "redirect:/login";

        // Check unlock requirement (10 completed lessons)
        int completedLessons = lessonProgressService.getCompletedLessonsCount(current);
        if (completedLessons < 10) {
            model.addAttribute("user", current);
            model.addAttribute("completedLessons", completedLessons);
            model.addAttribute("requiredLessons", 10);
            model.addAttribute("locked", true);
            return "leaderboard";
        }

        List<User> topUsers = leaderboardService.getTop10ByXp();
        model.addAttribute("user", current);
        model.addAttribute("topUsers", topUsers);
        model.addAttribute("locked", false);
        return "leaderboard";
    }
}
