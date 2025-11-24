package com.example.duokid.controller;

import com.example.duokid.model.Grammar;
import com.example.duokid.model.Lesson;
import com.example.duokid.model.User;
import com.example.duokid.repo.GrammarRepository;
import com.example.duokid.service.LessonService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/grade")
public class GradeController {

    private final LessonService lessonService;
    private final GrammarRepository grammarRepository;

    public GradeController(LessonService lessonService, GrammarRepository grammarRepository) {
        this.lessonService = lessonService;
        this.grammarRepository = grammarRepository;
    }

    /**
     * Route mặc định cho /grade - redirect đến lớp của user hoặc GRADE1
     */
    @GetMapping
    public String defaultGrade(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        
        // Redirect đến lớp của user hoặc GRADE1 nếu không có
        String userGrade = user.getGradeLevel();
        if (userGrade == null || userGrade.trim().isEmpty()) {
            userGrade = "GRADE1";
        }
        
        return "redirect:/grade/" + userGrade;
    }

    @GetMapping("/{grade}")
    public String viewGrade(@PathVariable String grade,
                            HttpSession session,
                            Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // Get lessons for this grade
        List<Lesson> lessons = lessonService.findAll()
                .stream()
                .filter(l -> grade.equalsIgnoreCase(l.getLevel()))
                .sorted((a, b) -> {
                    Integer orderA = a.getOrderIndex() != null ? a.getOrderIndex() : Integer.MAX_VALUE;
                    Integer orderB = b.getOrderIndex() != null ? b.getOrderIndex() : Integer.MAX_VALUE;
                    return orderA.compareTo(orderB);
                })
                .collect(Collectors.toList());

        // Get grammar for this grade
        List<Grammar> grammarList = grammarRepository.findByLevelOrderByOrderIndex(grade);

        model.addAttribute("user", user);
        model.addAttribute("grade", grade);
        model.addAttribute("lessons", lessons);
        model.addAttribute("grammarList", grammarList);

        return "grade";
    }
}
