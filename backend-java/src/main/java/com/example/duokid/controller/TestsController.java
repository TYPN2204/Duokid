package com.example.duokid.controller;

import com.example.duokid.model.Test;
import com.example.duokid.model.User;
import com.example.duokid.service.LessonProgressService;
import com.example.duokid.service.TestService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/tests")
public class TestsController {

    private final TestService testService;
    private final LessonProgressService lessonProgressService;

    public TestsController(TestService testService, LessonProgressService lessonProgressService) {
        this.testService = testService;
        this.lessonProgressService = lessonProgressService;
    }

    @GetMapping
    public String listTests(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        String userLevel = user.getGradeLevel();
        List<Test> tests = testService.getTestsByLevel(userLevel);
        int completedLessonsCount = lessonProgressService.getCompletedLessonsCount(user);

        model.addAttribute("user", user);
        model.addAttribute("tests", tests);
        model.addAttribute("completedLessonsCount", completedLessonsCount);
        return "tests";
    }
}

