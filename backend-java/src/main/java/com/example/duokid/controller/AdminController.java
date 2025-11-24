package com.example.duokid.controller;

import com.example.duokid.model.*;
import com.example.duokid.repo.*;
import com.example.duokid.service.DatabaseSeederService;
import com.example.duokid.service.LessonDataImportService;
import com.example.duokid.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.InputStream;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final LessonDataImportService lessonImportService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final GrammarRepository grammarRepository;
    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final DatabaseSeederService databaseSeederService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminController(
            LessonDataImportService lessonImportService,
            UserService userService,
            UserRepository userRepository,
            LessonRepository lessonRepository,
            GrammarRepository grammarRepository,
            TestRepository testRepository,
            TestQuestionRepository testQuestionRepository,
            DatabaseSeederService databaseSeederService) {
        this.lessonImportService = lessonImportService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.grammarRepository = grammarRepository;
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.databaseSeederService = databaseSeederService;
    }

    private boolean isAdmin(User user) {
        return user != null && Boolean.TRUE.equals(user.getIsAdmin());
    }

    @GetMapping
    public String adminDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isAdmin(user)) {
            return "redirect:/dashboard?error=no-permission";
        }

        // Statistics
        long totalLessons = lessonRepository.count();
        long totalGrammar = grammarRepository.count();
        long totalTests = testRepository.count();
        long totalUsers = userRepository.count();

        model.addAttribute("user", user);
        model.addAttribute("totalLessons", totalLessons);
        model.addAttribute("totalGrammar", totalGrammar);
        model.addAttribute("totalTests", totalTests);
        model.addAttribute("totalUsers", totalUsers);

        return "admin-dashboard";
    }

    @GetMapping("/import-lessons")
    public String showImportLessonsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isAdmin(user)) return "redirect:/dashboard?error=no-permission";
        return "admin-import-lessons";
    }

    @PostMapping("/import-lessons")
    public String importLessons(@RequestParam("file") MultipartFile file,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isAdmin(user)) return "redirect:/dashboard?error=no-permission";

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file JSON để import!");
            return "redirect:/admin/import-lessons";
        }

        try {
            InputStream inputStream = file.getInputStream();
            LessonDataImportService.ImportResult result = lessonImportService.importFromJson(inputStream);
            
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

        return "redirect:/admin/import-lessons";
    }

    @GetMapping("/import-tests")
    public String showImportTestsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isAdmin(user)) return "redirect:/dashboard?error=no-permission";
        return "admin-import-tests";
    }

    @PostMapping("/import-tests")
    public String importTests(@RequestParam("file") MultipartFile file,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isAdmin(user)) return "redirect:/dashboard?error=no-permission";

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file JSON để import!");
            return "redirect:/admin/import-tests";
        }

        try {
            InputStream inputStream = file.getInputStream();
            JsonNode rootNode = objectMapper.readTree(inputStream);

            int testsImported = 0;
            int questionsImported = 0;
            List<String> errors = new java.util.ArrayList<>();

            if (rootNode.has("tests") && rootNode.get("tests").isArray()) {
                for (JsonNode testNode : rootNode.get("tests")) {
                    try {
                        Test test = new Test();
                        test.setTitle(testNode.get("title").asText());
                        test.setDescription(testNode.has("description") ? testNode.get("description").asText() : "");
                        test.setLevel(testNode.get("level").asText());
                        test.setAfterLessons(testNode.has("afterLessons") ? testNode.get("afterLessons").asInt() : 0);
                        test.setPassingScore(testNode.has("passingScore") ? testNode.get("passingScore").asInt() : 70);
                        test.setHeartsLostOnFail(testNode.has("heartsLostOnFail") ? testNode.get("heartsLostOnFail").asInt() : 1);
                        test.setXpReward(testNode.has("xpReward") ? testNode.get("xpReward").asInt() : 20);
                        test.setGemsReward(testNode.has("gemsReward") ? testNode.get("gemsReward").asInt() : 10);
                        test.setInstructions(testNode.has("instructions") ? testNode.get("instructions").asText() : "");

                        test = testRepository.save(test);
                        testsImported++;

                        // Import questions
                        if (testNode.has("questions") && testNode.get("questions").isArray()) {
                            for (JsonNode questionNode : testNode.get("questions")) {
                                try {
                                    TestQuestion question = new TestQuestion();
                                    question.setTest(test);
                                    question.setQuestion(questionNode.get("question").asText());
                                    question.setOptionA(questionNode.get("optionA").asText());
                                    question.setOptionB(questionNode.get("optionB").asText());
                                    question.setOptionC(questionNode.has("optionC") ? questionNode.get("optionC").asText() : "");
                                    question.setOptionD(questionNode.has("optionD") ? questionNode.get("optionD").asText() : "");
                                    question.setCorrectOption(questionNode.get("correctOption").asText());
                                    question.setExplanation(questionNode.has("explanation") ? questionNode.get("explanation").asText() : "");

                                    testQuestionRepository.save(question);
                                    questionsImported++;
                                } catch (Exception e) {
                                    errors.add("Lỗi khi import câu hỏi: " + e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        errors.add("Lỗi khi import bài kiểm tra: " + e.getMessage());
                    }
                }
            }

            if (errors.isEmpty()) {
                redirectAttributes.addFlashAttribute("success", 
                    String.format("Import thành công! Đã import %d bài kiểm tra và %d câu hỏi.", 
                        testsImported, questionsImported));
            } else {
                redirectAttributes.addFlashAttribute("warning", 
                    String.format("Import hoàn tất với một số lỗi. Đã import %d bài kiểm tra và %d câu hỏi. Lỗi: %s", 
                        testsImported, questionsImported, String.join(", ", errors)));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi import: " + e.getMessage());
        }

        return "redirect:/admin/import-tests";
    }

    @GetMapping("/rewards")
    public String showRewardsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isAdmin(user)) return "redirect:/dashboard?error=no-permission";
        return "admin-rewards";
    }

    @PostMapping("/rewards/give")
    public String giveRewards(@RequestParam("userEmail") String userEmail,
                             @RequestParam(value = "xp", defaultValue = "0") int xp,
                             @RequestParam(value = "gems", defaultValue = "0") int gems,
                             @RequestParam(value = "hearts", defaultValue = "0") int hearts,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        if (admin == null) return "redirect:/login";
        if (!isAdmin(admin)) return "redirect:/dashboard?error=no-permission";

        try {
            User targetUser = userRepository.findByEmail(userEmail).orElse(null);
            if (targetUser == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng với email: " + userEmail);
                return "redirect:/admin/rewards";
            }

            targetUser.setXp(targetUser.getXp() + xp);
            targetUser.setGems(targetUser.getGems() + gems);
            targetUser.setHearts(targetUser.getHearts() + hearts);
            userService.save(targetUser);

            redirectAttributes.addFlashAttribute("success", 
                String.format("Đã tặng phần thưởng cho %s: +%d XP, +%d Gems, +%d Hearts", 
                    targetUser.getDisplayName(), xp, gems, hearts));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tặng phần thưởng: " + e.getMessage());
        }

        return "redirect:/admin/rewards";
    }

    @GetMapping("/vip-avatar")
    public String showVipAvatarPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isAdmin(user)) return "redirect:/dashboard?error=no-permission";

        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        return "admin-vip-avatar";
    }

    @PostMapping("/vip-avatar/give")
    public String giveVipAvatar(@RequestParam("userId") Long userId,
                               @RequestParam("avatarUrl") String avatarUrl,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        if (admin == null) return "redirect:/login";
        if (!isAdmin(admin)) return "redirect:/dashboard?error=no-permission";

        try {
            User targetUser = userRepository.findById(userId).orElse(null);
            if (targetUser == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
                return "redirect:/admin/vip-avatar";
            }

            targetUser.setAvatar(avatarUrl);
            userService.save(targetUser);

            redirectAttributes.addFlashAttribute("success", 
                String.format("Đã tặng avatar VIP cho %s!", targetUser.getDisplayName()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tặng avatar VIP: " + e.getMessage());
        }

        return "redirect:/admin/vip-avatar";
    }

    @GetMapping("/setup")
    public String setupPage(Model model) {
        // Cho phép tạo admin mới luôn
        return "admin-setup";
    }

    @PostMapping("/setup")
    public String createAdmin(@RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String displayName,
                             RedirectAttributes redirectAttributes) {
        // Kiểm tra email đã tồn tại chưa
        if (userRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được sử dụng! Vui lòng chọn email khác.");
            return "redirect:/admin/setup";
        }

        try {
            // Tạo admin account
            User admin = new User();
            admin.setEmail(email);
            admin.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
            admin.setDisplayName(displayName);
            admin.setIsAdmin(true);
            admin.setAvatar("/avatar1.svg");
            admin.setStreak(0);
            admin.setXp(0);
            admin.setGems(1000);
            admin.setHearts(5);
            admin.setGradeLevel("GRADE1");

            userRepository.save(admin);

            redirectAttributes.addFlashAttribute("success", "Tạo tài khoản admin thành công! Bạn có thể đăng nhập ngay.");
            return "redirect:/admin/setup?success=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tạo admin: " + e.getMessage());
            return "redirect:/admin/setup";
        }
    }

    @PostMapping("/seed-data")
    public String seedData(HttpSession session,
                          RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isAdmin(user)) return "redirect:/dashboard?error=no-permission";

        try {
            databaseSeederService.seedDatabase();
            redirectAttributes.addFlashAttribute("success", "Đã seed dữ liệu mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi seed dữ liệu: " + e.getMessage());
        }

        return "redirect:/admin";
    }
}

