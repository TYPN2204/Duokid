package com.example.duokid.controller;

import com.example.duokid.model.User;
import com.example.duokid.repo.VocabularyRepository;
import com.example.duokid.service.GateLessonService;
import com.example.duokid.service.LessonDataImportService;
import com.example.duokid.service.QuizGenerationService;
import com.example.duokid.service.VocabularyImportService;
import com.example.duokid.service.VocabularyToLessonService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;

@Controller
public class ImportController {

    private final LessonDataImportService importService;
    private final VocabularyImportService vocabularyImportService;
    private final VocabularyToLessonService vocabularyToLessonService;
    private final VocabularyRepository vocabularyRepository;
    private final QuizGenerationService quizGenerationService;
    private final GateLessonService gateLessonService;

    public ImportController(LessonDataImportService importService, 
                          VocabularyImportService vocabularyImportService,
                          VocabularyToLessonService vocabularyToLessonService,
                          VocabularyRepository vocabularyRepository,
                          QuizGenerationService quizGenerationService,
                          GateLessonService gateLessonService) {
        this.importService = importService;
        this.vocabularyImportService = vocabularyImportService;
        this.vocabularyToLessonService = vocabularyToLessonService;
        this.vocabularyRepository = vocabularyRepository;
        this.quizGenerationService = quizGenerationService;
        this.gateLessonService = gateLessonService;
    }

    @GetMapping("/admin/import")
    public String showImportPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // Thống kê từ vựng để hiển thị
        try {
            long vocabularyCount = vocabularyRepository.count();
            long listeningCount = vocabularyRepository.findByTestType("LISTENING").size();
            long readingCount = vocabularyRepository.findByTestType("READING").size();
            
            model.addAttribute("vocabularyCount", vocabularyCount);
            model.addAttribute("listeningCount", listeningCount);
            model.addAttribute("readingCount", readingCount);
            
            // Đếm số bài học có thể tạo (ước tính)
            long estimatedLessons = 0;
            if (vocabularyCount > 0) {
                // Ước tính: mỗi test có thể tạo 1 lesson, mỗi part có thể tạo 1 lesson
                estimatedLessons = (listeningCount > 0 ? 10 : 0) + (readingCount > 0 ? 10 : 0) + 8; // 8 parts (4 LISTENING + 4 READING)
            }
            model.addAttribute("estimatedLessons", estimatedLessons);
        } catch (Exception e) {
            // Nếu có lỗi, set về 0
            model.addAttribute("vocabularyCount", 0);
            model.addAttribute("listeningCount", 0);
            model.addAttribute("readingCount", 0);
            model.addAttribute("estimatedLessons", 0);
        }

        return "import_data";
    }

    @PostMapping("/admin/import")
    public String importData(@RequestParam("file") MultipartFile file,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file JSON để import!");
            return "redirect:/admin/import";
        }

        try {
            InputStream inputStream = file.getInputStream();
            LessonDataImportService.ImportResult result = importService.importFromJson(inputStream);
            
            if (result.isSuccess()) {
                redirectAttributes.addFlashAttribute("success", 
                    String.format("Import thành công! Đã import %d bài học và %d bài ngữ pháp.", 
                        result.getLessonsImported(), result.getGrammarImported()));
            } else {
                redirectAttributes.addFlashAttribute("warning", 
                    String.format("Import hoàn tất với một số lỗi. Đã import %d bài học và %d bài ngữ pháp. Lỗi: %s", 
                        result.getLessonsImported(), result.getGrammarImported(), 
                        String.join(", ", result.getErrors())));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi import: " + e.getMessage());
        }

        return "redirect:/admin/import";
    }

    @PostMapping("/admin/import/vocabulary")
    public String importVocabulary(@RequestParam(value = "file", required = false) MultipartFile file,
                                   @RequestParam(value = "testType", required = false) String testType,
                                   @RequestParam(value = "testNumber", required = false) Integer testNumber,
                                   @RequestParam(value = "importAll", required = false) Boolean importAll,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        try {
            VocabularyImportService.ImportResult result;

            if (importAll != null && importAll) {
                // Import tất cả từ thư mục Tieng-Anh
                String baseDirectory = "Tieng-Anh";
                result = vocabularyImportService.importAllFromDirectory(baseDirectory);
                
                if (result.getImported() > 0 || result.getSkipped() > 0) {
                    // Tự động tạo bài học từ từ vựng vừa import
                    try {
                        VocabularyToLessonService.CreateLessonsResult lessonResult = 
                            vocabularyToLessonService.createLessonsFromVocabulary();
                        if (lessonResult.getCreated() > 0) {
                            redirectAttributes.addFlashAttribute("success", 
                                String.format("✅ Import thành công! Đã import %d từ vựng (bỏ qua %d từ đã tồn tại). " +
                                    "Đã tự động tạo %d bài học từ từ vựng ETS (bỏ qua %d bài đã tồn tại).", 
                                    result.getImported(), result.getSkipped(), 
                                    lessonResult.getCreated(), lessonResult.getSkipped()));
                        } else {
                            redirectAttributes.addFlashAttribute("success", 
                                String.format("Import thành công! Đã import %d từ vựng (bỏ qua %d từ đã tồn tại). " +
                                    "Tất cả bài học đã tồn tại.", 
                                    result.getImported(), result.getSkipped()));
                        }
                    } catch (Exception e) {
                        redirectAttributes.addFlashAttribute("warning", 
                            String.format("Import từ vựng thành công (%d từ), nhưng có lỗi khi tạo bài học: %s", 
                                result.getImported(), e.getMessage()));
                    }
                } else if (result.isSuccess()) {
                    redirectAttributes.addFlashAttribute("warning", 
                        "Không có từ vựng mới nào được import (có thể tất cả đã tồn tại).");
                } else {
                    redirectAttributes.addFlashAttribute("warning", 
                        String.format("Import hoàn tất với một số lỗi. Đã import %d từ vựng (bỏ qua %d từ). Lỗi: %s", 
                            result.getImported(), result.getSkipped(), 
                            String.join("; ", result.getErrors())));
                }
            } else {
                // Import từ file upload
                if (file == null || file.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file CSV hoặc chọn 'Import tất cả'!");
                    return "redirect:/admin/import";
                }

                if (testType == null || testNumber == null) {
                    redirectAttributes.addFlashAttribute("error", "Vui lòng chọn loại test và số test!");
                    return "redirect:/admin/import";
                }

                result = vocabularyImportService.importFromMultipartFile(file, testType, testNumber);
                
                if (result.getImported() > 0) {
                    // Tự động tạo bài học từ từ vựng vừa import
                    try {
                        VocabularyToLessonService.CreateLessonsResult lessonResult = 
                            vocabularyToLessonService.createLessonsFromVocabulary();
                        if (lessonResult.getCreated() > 0) {
                            redirectAttributes.addFlashAttribute("success", 
                                String.format("✅ Import thành công! Đã import %d từ vựng (bỏ qua %d từ đã tồn tại). " +
                                    "Đã tự động tạo %d bài học từ từ vựng ETS.", 
                                    result.getImported(), result.getSkipped(), lessonResult.getCreated()));
                        } else {
                            redirectAttributes.addFlashAttribute("success", 
                                String.format("Import thành công! Đã import %d từ vựng (bỏ qua %d từ đã tồn tại). " +
                                    "Tất cả bài học đã tồn tại.", 
                                    result.getImported(), result.getSkipped()));
                        }
                    } catch (Exception e) {
                        redirectAttributes.addFlashAttribute("warning", 
                            String.format("Import từ vựng thành công (%d từ), nhưng có lỗi khi tạo bài học: %s", 
                                result.getImported(), e.getMessage()));
                    }
                } else if (result.isSuccess()) {
                    redirectAttributes.addFlashAttribute("warning", 
                        "Không có từ vựng mới nào được import (có thể tất cả đã tồn tại).");
                } else {
                    redirectAttributes.addFlashAttribute("warning", 
                        String.format("Import hoàn tất với một số lỗi. Đã import %d từ vựng. Lỗi: %s", 
                            result.getImported(), String.join("; ", result.getErrors())));
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi import từ vựng: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/admin/import";
    }

    @PostMapping("/admin/import/vocabulary-to-lessons")
    public String createLessonsFromVocabulary(HttpSession session,
                                             RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        
        if (user.getIsAdmin() == null || !user.getIsAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/admin/import";
        }

        try {
            VocabularyToLessonService.CreateLessonsResult result = 
                vocabularyToLessonService.createLessonsFromVocabulary();
            
            if (result.isSuccess()) {
                redirectAttributes.addFlashAttribute("success", 
                    String.format("✅ Đã tạo %d bài học từ từ vựng ETS (bỏ qua %d bài đã tồn tại)!", 
                        result.getCreated(), result.getSkipped()));
            } else {
                redirectAttributes.addFlashAttribute("warning", 
                    String.format("Đã tạo %d bài học (bỏ qua %d bài). Một số lỗi: %s", 
                        result.getCreated(), result.getSkipped(), 
                        String.join("; ", result.getErrors().subList(0, Math.min(5, result.getErrors().size())))));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tạo bài học: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/admin/import";
    }

    @PostMapping("/admin/import/create-quiz-for-lessons")
    public String createQuizForExistingLessons(HttpSession session,
                                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        
        if (user.getIsAdmin() == null || !user.getIsAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/admin/import";
        }

        try {
            VocabularyToLessonService.CreateLessonsResult result = 
                vocabularyToLessonService.createQuizQuestionsForExistingLessons();
            
            if (result.getCreated() > 0) {
                redirectAttributes.addFlashAttribute("success", 
                    String.format("✅ Đã tạo quiz cho %d bài học ETS (bỏ qua %d bài đã có quiz)!", 
                        result.getCreated(), result.getSkipped()));
            } else if (result.getSkipped() > 0) {
                redirectAttributes.addFlashAttribute("info", 
                    String.format("Tất cả bài học ETS đã có quiz (bỏ qua %d bài).", result.getSkipped()));
            } else {
                redirectAttributes.addFlashAttribute("warning", 
                    "Không tìm thấy bài học ETS nào hoặc có lỗi: " + 
                    String.join("; ", result.getErrors().subList(0, Math.min(3, result.getErrors().size()))));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tạo quiz: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/admin/import";
    }

    /**
     * Tạo quiz cho TẤT CẢ các bài học chưa có quiz
     */
    @PostMapping("/admin/import/create-quiz-for-all-lessons")
    public String createQuizForAllLessons(HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        
        if (user.getIsAdmin() == null || !user.getIsAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/admin/import";
        }

        try {
            QuizGenerationService.QuizGenerationResult result = 
                quizGenerationService.createQuizForAllLessons();
            
            if (result.getLessonsCreated() > 0) {
                redirectAttributes.addFlashAttribute("success", 
                    String.format("✅ Đã tạo quiz cho %d bài học (tổng %d câu hỏi)! " +
                        "(Bỏ qua %d bài đã có quiz)", 
                        result.getLessonsCreated(), 
                        result.getTotalQuestions(),
                        result.getLessonsSkipped()));
            } else if (result.getLessonsSkipped() > 0) {
                redirectAttributes.addFlashAttribute("info", 
                    String.format("Tất cả bài học đã có quiz (bỏ qua %d bài).", result.getLessonsSkipped()));
            } else {
                redirectAttributes.addFlashAttribute("warning", 
                    "Không tìm thấy bài học nào hoặc có lỗi: " + 
                    (result.getErrors().isEmpty() ? "Không có lỗi" : 
                     String.join("; ", result.getErrors().subList(0, Math.min(3, result.getErrors().size())))));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tạo quiz: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/admin/import";
    }

    /**
     * Tạo bài học theo cấu trúc ô cửa (Gate)
     */
    @PostMapping("/admin/import/create-gate-lessons")
    public String createGateLessons(HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        
        if (user.getIsAdmin() == null || !user.getIsAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/admin/import";
        }

        try {
            GateLessonService.CreateGateLessonsResult result = 
                gateLessonService.createGateLessons();
            
            if (result.getCreated() > 0) {
                redirectAttributes.addFlashAttribute("success", 
                    String.format("✅ Đã tạo %d bài học theo cấu trúc ô cửa! " +
                        "(Bỏ qua %d bài đã tồn tại)\n" +
                        "• Ô cửa 1 (GRADE1): Test 1-20\n" +
                        "• Ô cửa 2 (GRADE2): Test 21-40\n" +
                        "• Ô cửa 3 (GRADE3): Test 41-60\n" +
                        "• Ô cửa 4 (GRADE4): Test 61-80\n" +
                        "• Ô cửa 5 (GRADE5): Test 81-100\n" +
                        "Mỗi test được chia thành 10 bài nhỏ (test1.1, test1.2, ... test1.10)", 
                        result.getCreated(), 
                        result.getSkipped()));
            } else if (result.getSkipped() > 0) {
                redirectAttributes.addFlashAttribute("info", 
                    String.format("Tất cả bài học đã được tạo (bỏ qua %d bài).", result.getSkipped()));
            } else {
                redirectAttributes.addFlashAttribute("warning", 
                    "Không tìm thấy từ vựng nào hoặc có lỗi: " + 
                    (result.getErrors().isEmpty() ? "Không có lỗi" : 
                     String.join("; ", result.getErrors().subList(0, Math.min(3, result.getErrors().size())))));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tạo bài học: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/admin/import";
    }
}

