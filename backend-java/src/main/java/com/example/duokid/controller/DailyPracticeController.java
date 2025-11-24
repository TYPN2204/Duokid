package com.example.duokid.controller;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.User;
import com.example.duokid.service.AiPythonClient;
import com.example.duokid.service.LessonService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
public class DailyPracticeController {

    private static final Map<String, PracticeTask> TASKS = Map.of(
            "greetings", new PracticeTask(
                    "Hãy viết câu chào cô giáo buổi sáng.",
                    "Good morning, teacher."
            ),
            "colors", new PracticeTask(
                    "Em hãy viết câu mô tả quả táo màu đỏ.",
                    "The apple is red."
            ),
            "numbers", new PracticeTask(
                    "Viết câu: Em có ba cây bút chì.",
                    "I have three pencils."
            ),
            "animals", new PracticeTask(
                    "Hãy viết câu nói rằng em thích chó.",
                    "I like dogs."
            ),
            "family", new PracticeTask(
                    "Giới thiệu rằng đây là mẹ của em.",
                    "This is my mother."
            )
    );

    private static final List<PracticeTask> DEFAULT_TASKS = List.of(
            new PracticeTask(
                    "Viết câu giới thiệu bản thân.",
                    "Hello, I am Lan."
            ),
            new PracticeTask(
                    "Viết câu mô tả một con vật em yêu thích.",
                    "This is a cute cat."
            )
    );

    private final LessonService lessonService;
    private final AiPythonClient aiPythonClient;
    private final Random random = new Random();

    public DailyPracticeController(LessonService lessonService,
                                   AiPythonClient aiPythonClient) {
        this.lessonService = lessonService;
        this.aiPythonClient = aiPythonClient;
    }

    @GetMapping("/practice")
    public String practice(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        List<Lesson> all = lessonService.findAll();
        if (all.isEmpty()) return "redirect:/lessons";
        Collections.shuffle(all);
        Lesson randomLesson = all.get(0);

        PracticeTask task = pickTask(randomLesson);

        model.addAttribute("user", user);
        model.addAttribute("lesson", randomLesson);
        model.addAttribute("practiceQuestion", task.question());
        model.addAttribute("practiceExpected", task.expected());

        return "practice";
    }

    @GetMapping("/practice/game")
    public String practiceGame(@RequestParam(required = false) Long lessonId,
                              HttpSession session,
                              Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Lesson lesson;
        if (lessonId != null) {
            lesson = lessonService.findById(lessonId);
        } else {
            List<Lesson> all = lessonService.findAll();
            if (all.isEmpty()) return "redirect:/practice";
            Collections.shuffle(all);
            lesson = all.get(0);
        }

        if (lesson == null) return "redirect:/practice";

        model.addAttribute("user", user);
        model.addAttribute("lesson", lesson);
        return "practice_game";
    }

    @PostMapping("/practice/grade")
    public String gradeAnswer(@RequestParam Long lessonId,
                              @RequestParam String question,
                              @RequestParam String expected,
                              @RequestParam("studentAnswer") String answer,
                              HttpSession session,
                              Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Lesson lesson = lessonService.findById(lessonId);
        if (lesson == null) return "redirect:/practice";

        model.addAttribute("user", user);
        model.addAttribute("lesson", lesson);
        model.addAttribute("practiceQuestion", question);
        model.addAttribute("practiceExpected", expected);
        model.addAttribute("studentAnswer", answer);

        if (answer == null || answer.trim().isEmpty()) {
            model.addAttribute("gradeError", "Vui lòng nhập câu trả lời trước khi chấm điểm.");
            return "practice";
        }

        AiPythonClient.GradeResult result = aiPythonClient.gradeAnswer(question, expected, answer);
        model.addAttribute("gradeScore", result.score());
        model.addAttribute("gradeCommentEn", result.commentEn());
        model.addAttribute("gradeCommentVi", result.commentVi());

        return "practice";
    }

    private PracticeTask pickTask(Lesson lesson) {
        String title = lesson.getTitle() != null ? lesson.getTitle().toLowerCase() : "";
        for (Map.Entry<String, PracticeTask> entry : TASKS.entrySet()) {
            if (title.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT_TASKS.get(random.nextInt(DEFAULT_TASKS.size()));
    }

    private record PracticeTask(String question, String expected) {}
}
