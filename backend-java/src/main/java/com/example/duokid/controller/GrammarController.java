package com.example.duokid.controller;

import com.example.duokid.model.Grammar;
import com.example.duokid.model.User;
import com.example.duokid.repo.GrammarRepository;
import com.example.duokid.service.DailyGoalService;
import com.example.duokid.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/grammar")
public class GrammarController {

    private final GrammarRepository grammarRepo;
    private final UserService userService;
    private final DailyGoalService dailyGoalService;

    public GrammarController(GrammarRepository grammarRepo,
                            UserService userService,
                            DailyGoalService dailyGoalService) {
        this.grammarRepo = grammarRepo;
        this.userService = userService;
        this.dailyGoalService = dailyGoalService;
    }

    @GetMapping("/{id}")
    public String grammarDetail(@PathVariable Long id,
                                HttpSession session,
                                Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Grammar grammar = grammarRepo.findById(id).orElse(null);
        if (grammar == null) return "redirect:/dashboard";

        model.addAttribute("user", user);
        model.addAttribute("grammar", grammar);
        return "grammar_detail";
    }

    @PostMapping("/{id}/complete")
    public String completeGrammar(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Grammar grammar = grammarRepo.findById(id).orElse(null);
        if (grammar != null) {
            // Add XP and update streak
            userService.addXpAndUpdateStreak(user, grammar.getXpReward());
            dailyGoalService.markLessonCompleted(user);
            
            // Award gems
            user.setGems(user.getGems() + 10);
            userService.save(user);
            
            session.setAttribute("user", user);
            redirectAttributes.addFlashAttribute("message", "Chúc mừng! Bạn đã hoàn thành bài ngữ pháp!");
        }
        return "redirect:/grammar/" + id;
    }
}

