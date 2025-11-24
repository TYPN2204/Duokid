package com.example.duokid.service;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.QuizQuestion;
import com.example.duokid.model.Vocabulary;
import com.example.duokid.repo.LessonRepository;
import com.example.duokid.repo.QuizQuestionRepository;
import com.example.duokid.repo.VocabularyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service để tạo Lesson từ Vocabulary và tích hợp vào lộ trình học
 */
@Service
public class VocabularyToLessonService {

    private final VocabularyRepository vocabularyRepository;
    private final LessonRepository lessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    public VocabularyToLessonService(VocabularyRepository vocabularyRepository,
                                     LessonRepository lessonRepository,
                                     QuizQuestionRepository quizQuestionRepository) {
        this.vocabularyRepository = vocabularyRepository;
        this.lessonRepository = lessonRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    /**
     * Tạo Lesson từ từ vựng ETS theo test type và test number
     * Ví dụ: Tạo Lesson cho "LISTENING TEST 1", "READING TEST 1", etc.
     */
    public CreateLessonsResult createLessonsFromVocabulary() {
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try {
            // Lấy tất cả từ vựng
            List<Vocabulary> allVocabularies = vocabularyRepository.findAll();
            
            if (allVocabularies.isEmpty()) {
                return new CreateLessonsResult(0, 0, List.of("Không có từ vựng nào trong database. Hãy import từ vựng trước!"));
            }

            // Nhóm theo testType và testNumber
            Map<String, Map<Integer, List<Vocabulary>>> groupedByTest = allVocabularies.stream()
                    .filter(v -> v.getTestType() != null && v.getTestNumber() != null)
                    .collect(Collectors.groupingBy(
                            Vocabulary::getTestType,
                            Collectors.groupingBy(Vocabulary::getTestNumber)
                    ));

            // Tạo Lesson cho mỗi nhóm
            for (Map.Entry<String, Map<Integer, List<Vocabulary>>> testTypeEntry : groupedByTest.entrySet()) {
                String testType = testTypeEntry.getKey();
                Map<Integer, List<Vocabulary>> testNumbers = testTypeEntry.getValue();

                for (Map.Entry<Integer, List<Vocabulary>> testNumberEntry : testNumbers.entrySet()) {
                    Integer testNumber = testNumberEntry.getKey();
                    List<Vocabulary> vocabularies = testNumberEntry.getValue();

                    if (vocabularies.isEmpty()) continue;

                    try {
                        String lessonTitle = testType + " - TEST " + testNumber;
                        
                        // Kiểm tra xem đã có Lesson này chưa
                        List<Lesson> existing = lessonRepository.findAll().stream()
                                .filter(l -> l.getTitle() != null && l.getTitle().equals(lessonTitle))
                                .toList();

                        if (!existing.isEmpty()) {
                            skipped++;
                            continue;
                        }

                        // Tạo Lesson mới
                        Lesson lesson = new Lesson();
                        lesson.setTitle(lessonTitle);
                        lesson.setDescription("Từ vựng từ đề thi TOEIC " + testType + " TEST " + testNumber);
                        lesson.setLevel("GRADE5"); // ETS là level cao hơn
                        lesson.setLessonType("VOCABULARY");
                        lesson.setXpReward(15); // XP cao hơn vì là từ vựng TOEIC
                        
                        // Đặt orderIndex cao để không ảnh hưởng lộ trình chính
                        // Tìm orderIndex cao nhất hiện tại
                        Integer maxOrderIndex = lessonRepository.findAll().stream()
                                .map(Lesson::getOrderIndex)
                                .filter(java.util.Objects::nonNull)
                                .max(Integer::compareTo)
                                .orElse(100);
                        lesson.setOrderIndex(maxOrderIndex + 1);
                        
                        lesson.setPartName("ETS 2024 - " + testType);

                        // Tạo HTML content từ từ vựng
                        String contentHtml = generateContentHtml(vocabularies);
                        lesson.setContentHtml(contentHtml);

                        lessonRepository.save(lesson);
                        
                        // Tạo câu hỏi quiz từ từ vựng
                        createQuizQuestionsForLesson(lesson, vocabularies);
                        
                        created++;

                    } catch (Exception e) {
                        errors.add("Lỗi khi tạo Lesson cho " + testType + " TEST " + testNumber + ": " + e.getMessage());
                    }
                }
            }

            // Tạo Lesson theo Part (Part 1, Part 2, Part 3, Part 4) cho LISTENING
            CreateLessonsResult tempResult = new CreateLessonsResult(created, skipped, errors);
            tempResult = createLessonsByPart("LISTENING", allVocabularies, tempResult);
            tempResult = createLessonsByPart("READING", allVocabularies, tempResult);
            created = tempResult.getCreated();
            skipped = tempResult.getSkipped();
            errors = tempResult.getErrors();

        } catch (Exception e) {
            errors.add("Lỗi tổng quát: " + e.getMessage());
        }

        return new CreateLessonsResult(created, skipped, errors);
    }

    /**
     * Tạo Lesson theo Part (Part 1, Part 2, Part 3, Part 4)
     */
    private CreateLessonsResult createLessonsByPart(String testType, List<Vocabulary> allVocabularies,
                                    CreateLessonsResult result) {
        int created = result.getCreated();
        int skipped = result.getSkipped();
        List<String> errors = new ArrayList<>(result.getErrors());
        Map<String, List<Vocabulary>> groupedByPart = allVocabularies.stream()
                .filter(v -> testType.equals(v.getTestType()) && v.getPartNumber() != null)
                .collect(Collectors.groupingBy(Vocabulary::getPartNumber));

        for (Map.Entry<String, List<Vocabulary>> partEntry : groupedByPart.entrySet()) {
            String partNumber = partEntry.getKey();
            List<Vocabulary> vocabularies = partEntry.getValue();

            if (vocabularies.size() < 5) continue; // Bỏ qua nếu ít hơn 5 từ

            try {
                String lessonTitle = testType + " - " + partNumber;
                
                List<Lesson> existing = lessonRepository.findAll().stream()
                        .filter(l -> l.getTitle() != null && l.getTitle().equals(lessonTitle))
                        .toList();

                if (!existing.isEmpty()) {
                    skipped++;
                    continue;
                }

                Lesson lesson = new Lesson();
                lesson.setTitle(lessonTitle);
                lesson.setDescription("Từ vựng từ " + testType + " " + partNumber + " - ETS 2024");
                lesson.setLevel("GRADE5");
                lesson.setLessonType("VOCABULARY");
                lesson.setXpReward(12);
                
                Integer maxOrderIndex = lessonRepository.findAll().stream()
                        .map(Lesson::getOrderIndex)
                        .filter(java.util.Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(100);
                lesson.setOrderIndex(maxOrderIndex + 1);
                
                lesson.setPartName("ETS 2024 - " + testType);

                String contentHtml = generateContentHtml(vocabularies);
                lesson.setContentHtml(contentHtml);

                lessonRepository.save(lesson);
                
                // Tạo câu hỏi quiz từ từ vựng
                createQuizQuestionsForLesson(lesson, vocabularies);
                
                created++;

            } catch (Exception e) {
                errors.add("Lỗi khi tạo Lesson cho " + testType + " " + partNumber + ": " + e.getMessage());
            }
        }
        
        return new CreateLessonsResult(created, skipped, errors);
    }

    /**
     * Tạo HTML content từ danh sách từ vựng (tối ưu để giảm kích thước)
     */
    private String generateContentHtml(List<Vocabulary> vocabularies) {
        StringBuilder html = new StringBuilder();
        html.append("<style>.vocab-item{margin-bottom:.75rem;padding:.5rem;background:#f9fafb;border-radius:4px;border-left:3px solid #3b82f6}.vocab-word{font-weight:bold;font-size:1.1rem;color:#1f2937;margin-bottom:.25rem}.vocab-type{background:#e0e7ff;color:#3730a3;padding:.15rem .4rem;border-radius:8px;font-size:.7rem;margin-right:.4rem}.vocab-ipa{color:#6b7280;font-style:italic;font-size:.8rem}.vocab-meaning{color:#4b5563;margin-top:.4rem}.vocab-syn{font-size:.8rem;color:#10b981;margin-top:.4rem}.vocab-ex{padding:.4rem;background:#eff6ff;border-radius:3px;font-size:.8rem;color:#1e40af;font-style:italic;margin-top:.4rem}</style>");
        html.append("<h3>📚 Từ vựng (").append(vocabularies.size()).append(" từ)</h3>");
        html.append("<ul style='list-style:none;padding:0'>");
        
        for (Vocabulary vocab : vocabularies) {
            html.append("<li class='vocab-item'>");
            
            // Từ tiếng Anh
            html.append("<div class='vocab-word'>").append(escapeHtml(vocab.getEnglishWord())).append("</div>");
            
            // Loại từ và phiên âm trên cùng một dòng
            html.append("<div>");
            if (vocab.getWordType() != null && !vocab.getWordType().trim().isEmpty()) {
                html.append("<span class='vocab-type'>").append(escapeHtml(vocab.getWordType())).append("</span>");
            }
            if (vocab.getIpaAmerican() != null && !vocab.getIpaAmerican().trim().isEmpty()) {
                html.append("<span class='vocab-ipa'>🇺🇸 ").append(escapeHtml(vocab.getIpaAmerican()));
                if (vocab.getIpaBritish() != null && !vocab.getIpaBritish().trim().isEmpty() && !vocab.getIpaBritish().equals(vocab.getIpaAmerican())) {
                    html.append(" | 🇬🇧 ").append(escapeHtml(vocab.getIpaBritish()));
                }
                html.append("</span>");
            }
            html.append("</div>");
            
            // Nghĩa tiếng Việt
            html.append("<div class='vocab-meaning'>").append(escapeHtml(vocab.getVietnameseMeaning())).append("</div>");
            
            // Từ đồng nghĩa (nếu có)
            if (vocab.getSynonyms() != null && !vocab.getSynonyms().trim().isEmpty()) {
                html.append("<div class='vocab-syn'><strong>Đồng nghĩa:</strong> ").append(escapeHtml(vocab.getSynonyms())).append("</div>");
            }
            
            // Câu ví dụ (nếu có)
            if (vocab.getExampleSentence() != null && !vocab.getExampleSentence().trim().isEmpty()) {
                html.append("<div class='vocab-ex'><strong>Ví dụ:</strong> ").append(escapeHtml(vocab.getExampleSentence())).append("</div>");
            }
            
            html.append("</li>");
        }
        
        html.append("</ul>");
        return html.toString();
    }

    /**
     * Tạo câu hỏi quiz từ từ vựng cho một lesson
     */
    private void createQuizQuestionsForLesson(Lesson lesson, List<Vocabulary> vocabularies) {
        if (vocabularies == null || vocabularies.isEmpty()) return;
        
        // Chọn ngẫu nhiên tối đa 10 từ vựng để tạo câu hỏi
        List<Vocabulary> selectedVocabs = new ArrayList<>(vocabularies);
        Collections.shuffle(selectedVocabs);
        int maxQuestions = Math.min(10, selectedVocabs.size());
        selectedVocabs = selectedVocabs.subList(0, maxQuestions);
        
        // Lấy tất cả từ vựng để làm đáp án sai
        List<Vocabulary> allVocabs = vocabularyRepository.findAll();
        
        for (Vocabulary vocab : selectedVocabs) {
            try {
                // Kiểm tra xem đã có câu hỏi này chưa
                List<QuizQuestion> existing = quizQuestionRepository.findByLesson(lesson).stream()
                        .filter(q -> q.getQuestion() != null && 
                                q.getQuestion().contains(vocab.getEnglishWord()))
                        .toList();
                
                if (!existing.isEmpty()) continue;
                
                // Tạo câu hỏi về nghĩa của từ
                QuizQuestion question = new QuizQuestion();
                question.setLesson(lesson);
                question.setQuestion("Từ \"" + vocab.getEnglishWord() + "\" có nghĩa là gì?");
                
                // Đáp án đúng
                String correctAnswer = vocab.getVietnameseMeaning();
                
                // Tạo 3 đáp án sai từ các từ vựng khác
                List<String> wrongAnswers = allVocabs.stream()
                        .filter(v -> !v.getId().equals(vocab.getId()) && 
                                v.getVietnameseMeaning() != null && 
                                !v.getVietnameseMeaning().equals(correctAnswer))
                        .map(Vocabulary::getVietnameseMeaning)
                        .distinct()
                        .limit(3)
                        .collect(Collectors.toList());
                
                // Đảm bảo có đủ 3 đáp án sai
                while (wrongAnswers.size() < 3) {
                    wrongAnswers.add("Không xác định");
                }
                
                Collections.shuffle(wrongAnswers);
                
                // Gán đáp án vào A, B, C, D (đáp án đúng ở vị trí ngẫu nhiên)
                List<String> allOptions = new ArrayList<>(wrongAnswers);
                int correctIndex = (int) (Math.random() * 4);
                allOptions.add(correctIndex, correctAnswer);
                
                question.setOptionA(allOptions.get(0));
                question.setOptionB(allOptions.get(1));
                question.setOptionC(allOptions.get(2));
                question.setOptionD(allOptions.get(3));
                
                // Xác định đáp án đúng
                String correctOption = switch (correctIndex) {
                    case 0 -> "A";
                    case 1 -> "B";
                    case 2 -> "C";
                    default -> "D";
                };
                question.setCorrectOption(correctOption);
                
                question.setExplanation("Từ \"" + vocab.getEnglishWord() + "\" có nghĩa là \"" + 
                        correctAnswer + "\". " + 
                        (vocab.getExampleSentence() != null && !vocab.getExampleSentence().trim().isEmpty() ? 
                                "Ví dụ: " + vocab.getExampleSentence() : ""));
                
                quizQuestionRepository.save(question);
            } catch (Exception e) {
                // Bỏ qua lỗi khi tạo câu hỏi, tiếp tục với từ vựng tiếp theo
                System.err.println("Lỗi khi tạo câu hỏi cho từ: " + vocab.getEnglishWord() + " - " + e.getMessage());
            }
        }
    }

    /**
     * Tạo quiz questions cho các bài học ETS đã tồn tại nhưng chưa có quiz
     */
    public CreateLessonsResult createQuizQuestionsForExistingLessons() {
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // Tìm tất cả bài học ETS
            List<Lesson> etsLessons = lessonRepository.findAll().stream()
                    .filter(l -> l.getTitle() != null && 
                            (l.getTitle().contains("ETS 2024") || 
                             l.getTitle().contains("LISTENING - TEST") || 
                             l.getTitle().contains("READING - TEST") ||
                             l.getTitle().contains("LISTENING - Part") ||
                             l.getTitle().contains("READING - Part")))
                    .toList();
            
            if (etsLessons.isEmpty()) {
                return new CreateLessonsResult(0, 0, List.of("Không tìm thấy bài học ETS nào."));
            }
            
            // Lấy tất cả từ vựng
            List<Vocabulary> allVocabularies = vocabularyRepository.findAll();
            
            for (Lesson lesson : etsLessons) {
                try {
                    // Kiểm tra xem đã có quiz questions chưa
                    List<QuizQuestion> existingQuestions = quizQuestionRepository.findByLesson(lesson);
                    if (!existingQuestions.isEmpty()) {
                        skipped++;
                        continue;
                    }
                    
                    // Tìm từ vựng liên quan đến bài học này
                    List<Vocabulary> relatedVocabs = findVocabulariesForLesson(lesson, allVocabularies);
                    
                    if (relatedVocabs.isEmpty()) {
                        errors.add("Không tìm thấy từ vựng cho bài học: " + lesson.getTitle());
                        continue;
                    }
                    
                    // Tạo quiz questions
                    createQuizQuestionsForLesson(lesson, relatedVocabs);
                    created++;
                    
                } catch (Exception e) {
                    errors.add("Lỗi khi tạo quiz cho bài học " + lesson.getTitle() + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            errors.add("Lỗi tổng quát: " + e.getMessage());
        }
        
        return new CreateLessonsResult(created, skipped, errors);
    }
    
    /**
     * Tìm từ vựng liên quan đến một lesson dựa trên title
     */
    private List<Vocabulary> findVocabulariesForLesson(Lesson lesson, List<Vocabulary> allVocabularies) {
        String title = lesson.getTitle();
        if (title == null) return new ArrayList<>();
        
        // Nếu là TEST lesson (ví dụ: "LISTENING - TEST 1")
        if (title.contains("TEST")) {
            String[] parts = title.split("TEST");
            if (parts.length >= 2) {
                String testType = parts[0].trim().replace("-", "").trim();
                try {
                    int testNumber = Integer.parseInt(parts[1].trim());
                    return allVocabularies.stream()
                            .filter(v -> testType.equals(v.getTestType()) && 
                                    testNumber == v.getTestNumber())
                            .toList();
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        
        // Nếu là Part lesson (ví dụ: "LISTENING - Part 1")
        if (title.contains("Part")) {
            String[] parts = title.split("Part");
            if (parts.length >= 2) {
                String testType = parts[0].trim().replace("-", "").trim();
                String partNumber = "Part " + parts[1].trim();
                return allVocabularies.stream()
                        .filter(v -> testType.equals(v.getTestType()) && 
                                partNumber.equals(v.getPartNumber()))
                        .toList();
            }
        }
        
        return new ArrayList<>();
    }

    /**
     * Escape HTML để tránh XSS
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    /**
     * Result class
     */
    public static class CreateLessonsResult {
        private final int created;
        private final int skipped;
        private final List<String> errors;

        public CreateLessonsResult(int created, int skipped, List<String> errors) {
            this.created = created;
            this.skipped = skipped;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public int getCreated() {
            return created;
        }

        public int getSkipped() {
            return skipped;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean isSuccess() {
            return errors.isEmpty();
        }
    }
}

