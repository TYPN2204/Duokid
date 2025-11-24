package com.example.duokid.controller;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.QuizQuestion;
import com.example.duokid.model.User;

import java.util.List;
import com.example.duokid.service.AiPythonClient;
import com.example.duokid.service.DailyGoalService;
import com.example.duokid.service.LessonProgressService;
import com.example.duokid.service.LessonService;
import com.example.duokid.service.MyWordService;
import com.example.duokid.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lessons")
public class LessonController {

    private final LessonService lessonService;
    private final UserService userService;
    private final DailyGoalService dailyGoalService;
    private final LessonProgressService lessonProgressService;
    private final AiPythonClient aiPythonClient;
    private final MyWordService myWordService;

    public LessonController(LessonService lessonService,
                            UserService userService,
                            DailyGoalService dailyGoalService,
                            LessonProgressService lessonProgressService,
                            AiPythonClient aiPythonClient,
                            MyWordService myWordService) {
        this.lessonService = lessonService;
        this.userService = userService;
        this.dailyGoalService = dailyGoalService;
        this.lessonProgressService = lessonProgressService;
        this.aiPythonClient = aiPythonClient;
        this.myWordService = myWordService;
    }

    @GetMapping
    public String listLessons(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("lessons", lessonService.findAll());
        return "lessons";
    }

    @GetMapping("/{id}")
    public String lessonDetail(@PathVariable Long id,
                               HttpSession session,
                               Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Lesson lesson = lessonService.findById(id);
        if (lesson == null) return "redirect:/lessons";

        // Check if lesson is already completed
        var progressMap = lessonProgressService.getProgressMap(user);
        var progress = progressMap.get(lesson.getId());
        boolean isCompleted = progress != null && progress.isCompleted();
        
        // Check if lesson has quiz questions
        List<QuizQuestion> quizQuestions = lessonService.getQuestionsByLesson(lesson);
        boolean hasQuiz = !quizQuestions.isEmpty();
        boolean isAdmin = user.getIsAdmin() != null && user.getIsAdmin();

        model.addAttribute("user", user);
        model.addAttribute("lesson", lesson);
        model.addAttribute("isCompleted", isCompleted);
        model.addAttribute("hasQuiz", hasQuiz);
        model.addAttribute("isAdmin", isAdmin);
        if (!model.containsAttribute("wordEnglish")) model.addAttribute("wordEnglish", "");
        if (!model.containsAttribute("wordVietnamese")) model.addAttribute("wordVietnamese", "");
        if (!model.containsAttribute("wordIpa")) model.addAttribute("wordIpa", "");
        if (!model.containsAttribute("wordExample")) model.addAttribute("wordExample", "");
        return "lesson_detail";
    }

    @PostMapping("/{id}/complete")
    public String completeLesson(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Lesson lesson = lessonService.findById(id);
        if (lesson != null) {
            // Kiểm tra quyền admin - chỉ admin mới có thể complete mà không cần quiz
            boolean isAdmin = user.getIsAdmin() != null && user.getIsAdmin();
            
            if (!isAdmin) {
                redirectAttributes.addFlashAttribute("message", 
                    "⚠️ Bạn phải làm mini test và trả lời đúng hết các câu mới được vượt ải! Hãy làm mini test để tiếp tục.");
                return "redirect:/lessons/" + id;
            }

            // Check if already completed
            var progressMap = lessonProgressService.getProgressMap(user);
            var progress = progressMap.get(lesson.getId());
            if (progress != null && progress.isCompleted()) {
                redirectAttributes.addFlashAttribute("message", "Bài học này đã được hoàn thành rồi!");
                return "redirect:/lessons/" + id;
            }

            // Chỉ admin mới có thể complete thủ công
            LessonProgressService.GateRewardResult gateReward = lessonProgressService.markLessonCompleted(user, lesson, 100);
            
            // Add XP and update streak
            userService.addXpAndUpdateStreak(user, lesson.getXpReward());
            dailyGoalService.markLessonCompleted(user);
            
            // Award gems (10 gems per lesson)
            user.setGems(user.getGems() + 10);
            userService.save(user);
            
            session.setAttribute("user", user);
            
            // Thông báo rương thưởng nếu có
            if (gateReward != null && gateReward.isGateCompleted()) {
                redirectAttributes.addFlashAttribute("gateReward", 
                    String.format("🎁 Rương thưởng Ô cửa %d: +%d Gems, +%d XP! %s", 
                        gateReward.getGateNumber(), 
                        gateReward.getGemsReward(), 
                        gateReward.getXpReward(),
                        gateReward.isNextGateUnlocked() ? 
                            String.format("Ô cửa %d đã được mở khóa!", gateReward.getNextGateNumber()) : ""));
            }
            
            redirectAttributes.addFlashAttribute("message", "Chúc mừng! Bạn đã hoàn thành bài học và mở khóa bài học tiếp theo!");
        }
        return "redirect:/lessons/" + id;
    }

    @PostMapping("/{id}/suggest")
    public String suggestSentence(@PathVariable Long id,
                                  @RequestParam("vnText") String vnText,
                                  HttpSession session,
                                  Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Lesson lesson = lessonService.findById(id);
        if (lesson == null) return "redirect:/lessons";

        model.addAttribute("user", user);
        model.addAttribute("lesson", lesson);

        if (vnText == null || vnText.trim().isEmpty()) {
            model.addAttribute("aiError", "Vui lòng nhập câu tiếng Việt trước khi yêu cầu gợi ý.");
            return "lesson_detail";
        }

        String suggestion = aiPythonClient.suggestSentence(
                vnText.trim(),
                lesson.getTitle(),
                lesson.getLevel()
        );

        String audioUrl = aiPythonClient.getTtsAudioUrl(suggestion);

        model.addAttribute("aiSuggestion", suggestion);
        model.addAttribute("aiAudioUrl", audioUrl);
        model.addAttribute("vnText", vnText);

        return "lesson_detail";
    }

    @PostMapping("/{id}/mywords")
    public String addWordFromLesson(@PathVariable Long id,
                                    @RequestParam String englishWord,
                                    @RequestParam String vietnameseMeaning,
                                    @RequestParam(required = false) String ipa,
                                    @RequestParam(required = false) String exampleSentence,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Lesson lesson = lessonService.findById(id);
        if (lesson == null) return "redirect:/lessons";

        try {
            myWordService.addWord(user, englishWord, vietnameseMeaning, ipa, exampleSentence);
            redirectAttributes.addFlashAttribute("wordSuccess", "Đã lưu từ mới vào sổ tay!");
            return "redirect:/lessons/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("wordError", e.getMessage());
            redirectAttributes.addFlashAttribute("wordEnglish", englishWord);
            redirectAttributes.addFlashAttribute("wordVietnamese", vietnameseMeaning);
            redirectAttributes.addFlashAttribute("wordIpa", ipa);
            redirectAttributes.addFlashAttribute("wordExample", exampleSentence);
            return "redirect:/lessons/" + id;
        }
    }
}
