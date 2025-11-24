package com.example.duokid.controller;

import com.example.duokid.model.User;
import com.example.duokid.service.MyWordService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mywords")
public class MyWordController {

    private final MyWordService myWordService;

    public MyWordController(MyWordService myWordService) {
        this.myWordService = myWordService;
    }

    @GetMapping
    public String viewMyWords(HttpSession session, Model model) {
        User user = currentUser(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("words", myWordService.getWords(user));
        return "mywords";
    }

    @PostMapping
    public String addWord(@RequestParam String englishWord,
                          @RequestParam String vietnameseMeaning,
                          @RequestParam(required = false) String ipa,
                          @RequestParam(required = false) String exampleSentence,
                          HttpSession session,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        User user = currentUser(session);
        if (user == null) return "redirect:/login";

        try {
            myWordService.addWord(user, englishWord, vietnameseMeaning, ipa, exampleSentence);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu từ mới vào sổ tay! ✨");
            return "redirect:/mywords";
        } catch (IllegalArgumentException e) {
            model.addAttribute("user", user);
            model.addAttribute("words", myWordService.getWords(user));
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("englishWord", englishWord);
            model.addAttribute("vietnameseMeaning", vietnameseMeaning);
            model.addAttribute("ipa", ipa);
            model.addAttribute("exampleSentence", exampleSentence);
            return "mywords";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteWord(@PathVariable Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = currentUser(session);
        if (user == null) return "redirect:/login";

        myWordService.deleteWord(user, id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa từ khỏi sổ tay.");
        return "redirect:/mywords";
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("user");
    }
}

