package com.example.duokid.service;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.QuizQuestion;
import com.example.duokid.model.Vocabulary;
import com.example.duokid.repo.LessonRepository;
import com.example.duokid.repo.QuizQuestionRepository;
import com.example.duokid.repo.VocabularyRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service để tự động tạo quiz questions cho các bài học
 */
@Service
public class QuizGenerationService {

    private final LessonRepository lessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final VocabularyRepository vocabularyRepository;

    public QuizGenerationService(LessonRepository lessonRepository,
                                QuizQuestionRepository quizQuestionRepository,
                                VocabularyRepository vocabularyRepository) {
        this.lessonRepository = lessonRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.vocabularyRepository = vocabularyRepository;
    }

    /**
     * Tạo quiz cho một lesson cụ thể
     * @param lesson Bài học cần tạo quiz
     * @return Số câu hỏi đã tạo
     */
    public int createQuizForLesson(Lesson lesson) {
        if (lesson == null) return 0;

        // Kiểm tra xem đã có quiz chưa
        List<QuizQuestion> existingQuestions = quizQuestionRepository.findByLesson(lesson);
        if (!existingQuestions.isEmpty()) {
            return 0; // Đã có quiz rồi
        }

        int created = 0;

        // Nếu là lesson từ ETS vocabulary, tìm từ vựng liên quan
        if (lesson.getTitle() != null && 
            (lesson.getTitle().contains("ETS 2024") || 
             lesson.getTitle().contains("LISTENING") || 
             lesson.getTitle().contains("READING"))) {
            
            List<Vocabulary> relatedVocabs = findVocabulariesForLesson(lesson);
            if (!relatedVocabs.isEmpty()) {
                created = createQuizFromVocabularies(lesson, relatedVocabs);
            }
        }

        // Nếu chưa tạo được từ vocabulary, thử tạo từ contentHtml
        if (created == 0 && lesson.getContentHtml() != null && !lesson.getContentHtml().trim().isEmpty()) {
            created = createQuizFromContentHtml(lesson);
        }

        return created;
    }

    /**
     * Tìm từ vựng liên quan đến lesson (cho ETS lessons)
     */
    private List<Vocabulary> findVocabulariesForLesson(Lesson lesson) {
        String title = lesson.getTitle();
        if (title == null) return new ArrayList<>();

        List<Vocabulary> allVocabularies = vocabularyRepository.findAll();

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

        // Nếu chứa "ETS 2024" và testType
        if (title.contains("ETS 2024")) {
            String testType = title.contains("LISTENING") ? "LISTENING" : 
                             title.contains("READING") ? "READING" : null;
            if (testType != null) {
                return allVocabularies.stream()
                        .filter(v -> testType.equals(v.getTestType()))
                        .toList();
            }
        }

        return new ArrayList<>();
    }

    /**
     * Tạo quiz từ danh sách từ vựng
     */
    private int createQuizFromVocabularies(Lesson lesson, List<Vocabulary> vocabularies) {
        if (vocabularies == null || vocabularies.isEmpty()) return 0;

        // Chọn ngẫu nhiên tối đa 10 từ vựng để tạo câu hỏi
        List<Vocabulary> selectedVocabs = new ArrayList<>(vocabularies);
        Collections.shuffle(selectedVocabs);
        int maxQuestions = Math.min(10, selectedVocabs.size());
        selectedVocabs = selectedVocabs.subList(0, maxQuestions);

        // Lấy tất cả từ vựng để làm đáp án sai
        List<Vocabulary> allVocabs = vocabularyRepository.findAll();

        int created = 0;
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
                created++;
            } catch (Exception e) {
                System.err.println("Lỗi khi tạo câu hỏi cho từ: " + vocab.getEnglishWord() + " - " + e.getMessage());
            }
        }

        return created;
    }

    /**
     * Tạo quiz từ contentHtml của lesson (parse HTML để lấy từ vựng)
     */
    private int createQuizFromContentHtml(Lesson lesson) {
        String contentHtml = lesson.getContentHtml();
        if (contentHtml == null || contentHtml.trim().isEmpty()) return 0;

        // Parse HTML để lấy từ vựng
        List<WordMeaning> wordMeanings = extractWordsFromHtml(contentHtml);
        if (wordMeanings.isEmpty()) return 0;

        // Chọn ngẫu nhiên tối đa 10 từ để tạo câu hỏi
        Collections.shuffle(wordMeanings);
        int maxQuestions = Math.min(10, wordMeanings.size());
        wordMeanings = wordMeanings.subList(0, maxQuestions);

        // Lấy tất cả từ vựng để làm đáp án sai
        List<Vocabulary> allVocabs = vocabularyRepository.findAll();
        List<String> allMeanings = allVocabs.stream()
                .map(Vocabulary::getVietnameseMeaning)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        int created = 0;
        for (WordMeaning wm : wordMeanings) {
            try {
                // Kiểm tra xem đã có câu hỏi này chưa
                List<QuizQuestion> existing = quizQuestionRepository.findByLesson(lesson).stream()
                        .filter(q -> q.getQuestion() != null && 
                                q.getQuestion().contains(wm.word))
                        .toList();

                if (!existing.isEmpty()) continue;

                // Tạo câu hỏi về nghĩa của từ
                QuizQuestion question = new QuizQuestion();
                question.setLesson(lesson);
                question.setQuestion("Từ \"" + wm.word + "\" có nghĩa là gì?");

                // Đáp án đúng
                String correctAnswer = wm.meaning;

                // Tạo 3 đáp án sai
                List<String> wrongAnswers = allMeanings.stream()
                        .filter(m -> !m.equals(correctAnswer))
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

                question.setExplanation("Từ \"" + wm.word + "\" có nghĩa là \"" + correctAnswer + "\".");

                quizQuestionRepository.save(question);
                created++;
            } catch (Exception e) {
                System.err.println("Lỗi khi tạo câu hỏi cho từ: " + wm.word + " - " + e.getMessage());
            }
        }

        return created;
    }

    /**
     * Extract từ vựng từ HTML content
     * Hỗ trợ các format: "word – meaning", "word: meaning", "word - meaning", <li>word – meaning</li>
     */
    private List<WordMeaning> extractWordsFromHtml(String html) {
        List<WordMeaning> wordMeanings = new ArrayList<>();

        try {
            // Parse HTML
            Document doc = Jsoup.parse(html);
            
            // Tìm trong các thẻ <li>
            Elements listItems = doc.select("li");
            for (Element li : listItems) {
                String text = li.text().trim();
                WordMeaning wm = parseWordMeaning(text);
                if (wm != null) {
                    wordMeanings.add(wm);
                }
            }

            // Nếu không tìm thấy trong <li>, thử parse toàn bộ text
            if (wordMeanings.isEmpty()) {
                String text = doc.text();
                // Tìm pattern: word – meaning hoặc word: meaning
                Pattern pattern = Pattern.compile("([a-zA-Z]+(?:\\s+[a-zA-Z]+)*)\\s*[–:-]\\s*([^\\n]+)");
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    String word = matcher.group(1).trim();
                    String meaning = matcher.group(2).trim();
                    if (word.length() > 1 && meaning.length() > 1) {
                        wordMeanings.add(new WordMeaning(word, meaning));
                    }
                }
            }

            // Tìm trong các thẻ có class vocab-word và vocab-meaning
            Elements vocabWords = doc.select(".vocab-word");
            Elements vocabMeanings = doc.select(".vocab-meaning");
            if (vocabWords.size() == vocabMeanings.size() && vocabWords.size() > 0) {
                for (int i = 0; i < vocabWords.size(); i++) {
                    String word = vocabWords.get(i).text().trim();
                    String meaning = vocabMeanings.get(i).text().trim();
                    if (!word.isEmpty() && !meaning.isEmpty()) {
                        wordMeanings.add(new WordMeaning(word, meaning));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi parse HTML: " + e.getMessage());
        }

        return wordMeanings;
    }

    /**
     * Parse text để lấy word và meaning
     * Format: "word – meaning" hoặc "word: meaning" hoặc "word - meaning"
     */
    private WordMeaning parseWordMeaning(String text) {
        if (text == null || text.trim().isEmpty()) return null;

        // Thử các pattern
        String[] patterns = {
            " – ", " - ", ": ", " — ", " — "
        };

        for (String pattern : patterns) {
            int index = text.indexOf(pattern);
            if (index > 0 && index < text.length() - pattern.length()) {
                String word = text.substring(0, index).trim();
                String meaning = text.substring(index + pattern.length()).trim();
                if (word.length() > 1 && meaning.length() > 1) {
                    return new WordMeaning(word, meaning);
                }
            }
        }

        return null;
    }

    /**
     * Tạo quiz cho tất cả lessons chưa có quiz
     */
    public QuizGenerationResult createQuizForAllLessons() {
        int created = 0;
        int skipped = 0;
        int totalQuestions = 0;
        List<String> errors = new ArrayList<>();

        try {
            List<Lesson> allLessons = lessonRepository.findAll();
            
            for (Lesson lesson : allLessons) {
                try {
                    // Kiểm tra xem đã có quiz chưa
                    List<QuizQuestion> existing = quizQuestionRepository.findByLesson(lesson);
                    if (!existing.isEmpty()) {
                        skipped++;
                        continue;
                    }

                    // Tạo quiz
                    int questionsCreated = createQuizForLesson(lesson);
                    if (questionsCreated > 0) {
                        created++;
                        totalQuestions += questionsCreated;
                    } else {
                        errors.add("Không thể tạo quiz cho bài học: " + 
                                (lesson.getTitle() != null ? lesson.getTitle() : "ID " + lesson.getId()));
                    }
                } catch (Exception e) {
                    errors.add("Lỗi khi tạo quiz cho bài học " + 
                            (lesson.getTitle() != null ? lesson.getTitle() : "ID " + lesson.getId()) + 
                            ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("Lỗi tổng quát: " + e.getMessage());
        }

        return new QuizGenerationResult(created, skipped, totalQuestions, errors);
    }

    /**
     * Kết quả tạo quiz
     */
    public static class QuizGenerationResult {
        private final int lessonsCreated;
        private final int lessonsSkipped;
        private final int totalQuestions;
        private final List<String> errors;

        public QuizGenerationResult(int lessonsCreated, int lessonsSkipped, int totalQuestions, List<String> errors) {
            this.lessonsCreated = lessonsCreated;
            this.lessonsSkipped = lessonsSkipped;
            this.totalQuestions = totalQuestions;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public int getLessonsCreated() { return lessonsCreated; }
        public int getLessonsSkipped() { return lessonsSkipped; }
        public int getTotalQuestions() { return totalQuestions; }
        public List<String> getErrors() { return errors; }
        public boolean isSuccess() { return errors.isEmpty(); }
    }

    /**
     * Helper class để lưu word và meaning
     */
    private static class WordMeaning {
        String word;
        String meaning;

        WordMeaning(String word, String meaning) {
            this.word = word;
            this.meaning = meaning;
        }
    }
}

