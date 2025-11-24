package com.example.duokid.service;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.QuizQuestion;
import com.example.duokid.model.Vocabulary;
import com.example.duokid.repo.LessonRepository;
import com.example.duokid.repo.QuizQuestionRepository;
import com.example.duokid.repo.VocabularyRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service để tạo bài học theo cấu trúc ô cửa (Gate)
 * - Ô cửa 1 = GRADE1
 * - Ô cửa 2 = GRADE2
 * - Ô cửa 3 = GRADE3
 * - Ô cửa 4 = GRADE4
 * - Ô cửa 5 = GRADE5
 * 
 * Mỗi test sẽ được chia nhỏ thành test1.1, test1.2, ... test1.10
 */
@Service
public class GateLessonService {

    private final VocabularyRepository vocabularyRepository;
    private final LessonRepository lessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    public GateLessonService(VocabularyRepository vocabularyRepository,
                            LessonRepository lessonRepository,
                            QuizQuestionRepository quizQuestionRepository) {
        this.vocabularyRepository = vocabularyRepository;
        this.lessonRepository = lessonRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    /**
     * Tính toán ô cửa (gate) dựa trên test number
     * Test 1-20 -> Gate 1 (GRADE1)
     * Test 21-40 -> Gate 2 (GRADE2)
     * Test 41-60 -> Gate 3 (GRADE3)
     * Test 61-80 -> Gate 4 (GRADE4)
     * Test 81-100 -> Gate 5 (GRADE5)
     */
    public int calculateGate(int testNumber) {
        if (testNumber <= 20) return 1;
        if (testNumber <= 40) return 2;
        if (testNumber <= 60) return 3;
        if (testNumber <= 80) return 4;
        return 5;
    }

    /**
     * Lấy level (GRADE) từ gate number
     */
    public String getGradeFromGate(int gate) {
        return "GRADE" + gate;
    }

    /**
     * Tính toán sub-test number (1.1, 1.2, ... 1.10) từ test number và index
     */
    public String getSubTestNumber(int testNumber, int subIndex) {
        return testNumber + "." + subIndex;
    }

    /**
     * Chia danh sách từ vựng thành các nhóm nhỏ (mỗi nhóm ~10 từ)
     */
    public List<List<Vocabulary>> splitVocabularyIntoSubTests(List<Vocabulary> vocabularies, int subTestsPerTest) {
        List<List<Vocabulary>> result = new ArrayList<>();
        int totalVocabs = vocabularies.size();
        int vocabsPerSubTest = Math.max(1, totalVocabs / subTestsPerTest);
        
        for (int i = 0; i < subTestsPerTest; i++) {
            int start = i * vocabsPerSubTest;
            int end = (i == subTestsPerTest - 1) ? totalVocabs : (i + 1) * vocabsPerSubTest;
            
            if (start < totalVocabs) {
                result.add(vocabularies.subList(start, end));
            }
        }
        
        return result;
    }

    /**
     * Tạo tất cả bài học theo cấu trúc ô cửa
     */
    public CreateGateLessonsResult createGateLessons() {
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try {
            List<Vocabulary> allVocabularies = vocabularyRepository.findAll();
            
            if (allVocabularies.isEmpty()) {
                return new CreateGateLessonsResult(0, 0, List.of("Không có từ vựng nào trong database. Hãy import từ vựng trước!"));
            }

            // Nhóm theo testType và testNumber
            Map<String, Map<Integer, List<Vocabulary>>> groupedByTest = allVocabularies.stream()
                    .filter(v -> v.getTestType() != null && v.getTestNumber() != null)
                    .collect(Collectors.groupingBy(
                            Vocabulary::getTestType,
                            Collectors.groupingBy(Vocabulary::getTestNumber)
                    ));

            // Tạo lessons cho mỗi test
            for (Map.Entry<String, Map<Integer, List<Vocabulary>>> testTypeEntry : groupedByTest.entrySet()) {
                String testType = testTypeEntry.getKey();
                Map<Integer, List<Vocabulary>> testNumbers = testTypeEntry.getValue();

                for (Map.Entry<Integer, List<Vocabulary>> testNumberEntry : testNumbers.entrySet()) {
                    Integer testNumber = testNumberEntry.getKey();
                    List<Vocabulary> vocabularies = testNumberEntry.getValue();

                    if (vocabularies.isEmpty()) continue;

                    try {
                        // Tính toán gate và grade
                        int gate = calculateGate(testNumber);
                        String grade = getGradeFromGate(gate);
                        String partName = "PHẦN 1, CỬA " + gate;

                        // Chia từ vựng thành 10 sub-tests (test1.1, test1.2, ... test1.10)
                        List<List<Vocabulary>> subTests = splitVocabularyIntoSubTests(vocabularies, 10);

                        // Tạo lesson cho mỗi sub-test
                        for (int subIndex = 1; subIndex <= subTests.size(); subIndex++) {
                            List<Vocabulary> subTestVocabs = subTests.get(subIndex - 1);
                            if (subTestVocabs.isEmpty()) continue;

                            String subTestNumber = getSubTestNumber(testNumber, subIndex);
                            String lessonTitle = testType + " - TEST " + subTestNumber;

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
                            lesson.setDescription("Từ vựng từ đề thi TOEIC " + testType + " TEST " + subTestNumber + " - Ô cửa " + gate);
                            lesson.setLevel(grade);
                            lesson.setLessonType("VOCABULARY");
                            lesson.setXpReward(15);
                            lesson.setPartName(partName);

                            // Tính orderIndex: gate * 10000 + testNumber * 100 + subIndex
                            // Đảm bảo lessons trong cùng gate được nhóm lại
                            int orderIndex = gate * 10000 + testNumber * 100 + subIndex;
                            lesson.setOrderIndex(orderIndex);

                            // Tạo HTML content
                            String contentHtml = generateContentHtml(subTestVocabs);
                            lesson.setContentHtml(contentHtml);

                            lessonRepository.save(lesson);
                            
                            // Tạo quiz questions
                            createQuizQuestionsForLesson(lesson, subTestVocabs);
                            
                            created++;
                        }
                    } catch (Exception e) {
                        errors.add("Lỗi khi tạo Lesson cho " + testType + " TEST " + testNumber + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            errors.add("Lỗi tổng quát: " + e.getMessage());
        }

        return new CreateGateLessonsResult(created, skipped, errors);
    }

    /**
     * Tạo HTML content từ danh sách từ vựng
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
            
            // Loại từ và phiên âm
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
            
            // Từ đồng nghĩa
            if (vocab.getSynonyms() != null && !vocab.getSynonyms().trim().isEmpty()) {
                html.append("<div class='vocab-syn'><strong>Đồng nghĩa:</strong> ").append(escapeHtml(vocab.getSynonyms())).append("</div>");
            }
            
            // Câu ví dụ
            if (vocab.getExampleSentence() != null && !vocab.getExampleSentence().trim().isEmpty()) {
                html.append("<div class='vocab-ex'><strong>Ví dụ:</strong> ").append(escapeHtml(vocab.getExampleSentence())).append("</div>");
            }
            
            html.append("</li>");
        }
        
        html.append("</ul>");
        return html.toString();
    }

    /**
     * Tạo quiz questions cho lesson
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
                
                // Tạo 3 đáp án sai
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
                
                // Gán đáp án vào A, B, C, D
                List<String> allOptions = new ArrayList<>(wrongAnswers);
                int correctIndex = (int) (Math.random() * 4);
                allOptions.add(correctIndex, correctAnswer);
                
                question.setOptionA(allOptions.get(0));
                question.setOptionB(allOptions.get(1));
                question.setOptionC(allOptions.get(2));
                question.setOptionD(allOptions.get(3));
                
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
                System.err.println("Lỗi khi tạo câu hỏi cho từ: " + vocab.getEnglishWord() + " - " + e.getMessage());
            }
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    public static class CreateGateLessonsResult {
        private final int created;
        private final int skipped;
        private final List<String> errors;

        public CreateGateLessonsResult(int created, int skipped, List<String> errors) {
            this.created = created;
            this.skipped = skipped;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public int getCreated() { return created; }
        public int getSkipped() { return skipped; }
        public List<String> getErrors() { return errors; }
        public boolean isSuccess() { return errors.isEmpty(); }
    }
}

