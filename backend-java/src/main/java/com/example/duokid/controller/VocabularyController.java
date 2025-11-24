package com.example.duokid.controller;

import com.example.duokid.model.User;
import com.example.duokid.model.Vocabulary;
import com.example.duokid.repo.VocabularyRepository;
import com.example.duokid.service.VocabularyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/vocabulary")
public class VocabularyController {

    private final VocabularyRepository vocabularyRepository;
    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyRepository vocabularyRepository,
                               VocabularyService vocabularyService) {
        this.vocabularyRepository = vocabularyRepository;
        this.vocabularyService = vocabularyService;
    }

    /**
     * Hiển thị danh sách từ vựng với các bộ lọc
     */
    @GetMapping
    public String listVocabulary(
            @RequestParam(required = false) String testType,
            @RequestParam(required = false) Integer testNumber,
            @RequestParam(required = false) String partNumber,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            HttpSession session,
            Model model) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // Lấy danh sách từ vựng theo filter
        List<Vocabulary> vocabularies;
        
        if (testType != null && !testType.isEmpty() && testNumber != null) {
            vocabularies = vocabularyRepository.findByTestTypeAndTestNumber(testType, testNumber);
        } else if (testType != null && !testType.isEmpty() && partNumber != null && !partNumber.isEmpty()) {
            vocabularies = vocabularyRepository.findByTestTypeAndPartNumber(testType, partNumber);
        } else if (testType != null && !testType.isEmpty()) {
            vocabularies = vocabularyRepository.findByTestType(testType);
        } else {
            vocabularies = vocabularyRepository.findAll();
        }

        // Tìm kiếm nếu có
        if (search != null && !search.trim().isEmpty()) {
            vocabularies = vocabularyService.searchVocabularies(search);
            // Apply filters after search
            if (testType != null && !testType.isEmpty()) {
                vocabularies = vocabularies.stream()
                        .filter(v -> testType.equals(v.getTestType()))
                        .toList();
            }
            if (testNumber != null) {
                vocabularies = vocabularies.stream()
                        .filter(v -> testNumber.equals(v.getTestNumber()))
                        .toList();
            }
            if (partNumber != null && !partNumber.isEmpty()) {
                vocabularies = vocabularies.stream()
                        .filter(v -> partNumber.equals(v.getPartNumber()))
                        .toList();
            }
        }

        // Phân trang
        int total = vocabularies.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        List<Vocabulary> pagedVocabularies = vocabularies.subList(Math.min(start, total), end);

        // Thống kê
        long totalCount = vocabularyService.countAll();
        long listeningCount = vocabularyService.countByTestType("LISTENING");
        long readingCount = vocabularyService.countByTestType("READING");

        // Lấy danh sách test numbers và part numbers để hiển thị filter
        List<Integer> listeningTestNumbers = vocabularyService.getTestNumbersByType("LISTENING");
        List<Integer> readingTestNumbers = vocabularyService.getTestNumbersByType("READING");
        List<String> listeningParts = vocabularyService.getPartNumbersByType("LISTENING");
        List<String> readingParts = vocabularyService.getPartNumbersByType("READING");

        model.addAttribute("user", user);
        model.addAttribute("vocabularies", pagedVocabularies);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("listeningCount", listeningCount);
        model.addAttribute("readingCount", readingCount);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", (total + size - 1) / size);
        model.addAttribute("pageSize", size);
        model.addAttribute("total", total);
        
        // Filter values
        model.addAttribute("testType", testType);
        model.addAttribute("testNumber", testNumber);
        model.addAttribute("partNumber", partNumber);
        model.addAttribute("search", search);
        
        // Filter options
        model.addAttribute("listeningTestNumbers", listeningTestNumbers);
        model.addAttribute("readingTestNumbers", readingTestNumbers);
        model.addAttribute("listeningParts", listeningParts);
        model.addAttribute("readingParts", readingParts);
        
        model.addAttribute("isAdmin", isAdmin(user));

        return "vocabulary_list";
    }

    /**
     * Xem chi tiết một từ vựng
     */
    @GetMapping("/{id}")
    public String viewVocabulary(@PathVariable Long id,
                                HttpSession session,
                                Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Vocabulary vocabulary = vocabularyService.getVocabularyById(id).orElse(null);
        if (vocabulary == null) {
            return "redirect:/vocabulary?error=not-found";
        }

        model.addAttribute("user", user);
        model.addAttribute("vocabulary", vocabulary);
        model.addAttribute("isAdmin", isAdmin(user));

        return "vocabulary_detail";
    }

    /**
     * Xóa từ vựng (chỉ admin)
     */
    @PostMapping("/{id}/delete")
    public String deleteVocabulary(@PathVariable Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        
        if (!isAdmin(user)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xóa từ vựng!");
            return "redirect:/vocabulary";
        }

        Vocabulary vocabulary = vocabularyService.getVocabularyById(id).orElse(null);
        if (vocabulary == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy từ vựng!");
            return "redirect:/vocabulary";
        }

        vocabularyService.deleteVocabulary(id);
        redirectAttributes.addFlashAttribute("success", 
            String.format("Đã xóa từ vựng '%s'!", vocabulary.getEnglishWord()));

        return "redirect:/vocabulary";
    }

    /**
     * Trang thống kê từ vựng (admin)
     */
    @GetMapping("/stats")
    public String vocabularyStats(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        
        if (!isAdmin(user)) {
            return "redirect:/vocabulary?error=no-permission";
        }

        long totalCount = vocabularyService.countAll();
        long listeningCount = vocabularyService.countByTestType("LISTENING");
        long readingCount = vocabularyService.countByTestType("READING");

        // Thống kê theo test number
        List<Integer> listeningTestNumbers = vocabularyService.getTestNumbersByType("LISTENING");
        List<Integer> readingTestNumbers = vocabularyService.getTestNumbersByType("READING");
        
        // Thống kê theo part
        List<String> listeningParts = vocabularyService.getPartNumbersByType("LISTENING");
        List<String> readingParts = vocabularyService.getPartNumbersByType("READING");

        model.addAttribute("user", user);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("listeningCount", listeningCount);
        model.addAttribute("readingCount", readingCount);
        model.addAttribute("listeningTestNumbers", listeningTestNumbers);
        model.addAttribute("readingTestNumbers", readingTestNumbers);
        model.addAttribute("listeningParts", listeningParts);
        model.addAttribute("readingParts", readingParts);

        return "vocabulary_stats";
    }

    /**
     * API endpoint để lấy từ vựng theo test (cho AJAX)
     */
    @GetMapping("/api")
    @ResponseBody
    public List<Vocabulary> getVocabularyApi(
            @RequestParam(required = false) String testType,
            @RequestParam(required = false) Integer testNumber,
            @RequestParam(required = false) String partNumber) {
        
        if (testType != null && !testType.isEmpty() && testNumber != null) {
            return vocabularyService.getVocabulariesByTestTypeAndNumber(testType, testNumber);
        } else if (testType != null && !testType.isEmpty() && partNumber != null && !partNumber.isEmpty()) {
            return vocabularyService.getVocabulariesByTestTypeAndPart(testType, partNumber);
        } else if (testType != null && !testType.isEmpty()) {
            return vocabularyService.getVocabulariesByTestType(testType);
        } else {
            return vocabularyService.getAllVocabularies();
        }
    }

    private boolean isAdmin(User user) {
        return user != null && Boolean.TRUE.equals(user.getIsAdmin());
    }
}

