package com.example.duokid.service;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.QuizQuestion;
import com.example.duokid.repo.LessonRepository;
import com.example.duokid.repo.QuizQuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service để load dữ liệu từ train_ai_teacher_1000.json
 * và tạo thêm bài học, câu hỏi quiz, từ vựng cho website
 */
@Service
public class DataLoaderService {

    private final LessonRepository lessonRepo;
    private final QuizQuestionRepository quizRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map để lưu các từ vựng đã trích xuất theo chủ đề
    private final Map<String, Set<String>> vocabByTopic = new HashMap<>();
    private final Map<String, List<QuizData>> quizQuestions = new HashMap<>();
    
    // Map để lưu từ vựng cho game hình ảnh (word -> emoji mapping)
    private final Map<String, String> vocabForGame = new HashMap<>();
    
    // List để lưu practice tasks
    private final List<PracticeTaskData> practiceTasks = new ArrayList<>();

    public DataLoaderService(LessonRepository lessonRepo, QuizQuestionRepository quizRepo) {
        this.lessonRepo = lessonRepo;
        this.quizRepo = quizRepo;
    }

    @PostConstruct
    public void loadDataFromJson() {
        InputStream inputStream = null;
        try {
            // Chỉ load nếu chưa có dữ liệu
            if (lessonRepo.count() > 10) {
                return; // Đã có dữ liệu, không cần load thêm
            }

            // Tìm file trong root directory của project (backend-java folder)
            java.io.File file = new java.io.File("train_ai_teacher_1000.json");
            if (!file.exists()) {
                // Thử tìm trong resources
                ClassPathResource resource = new ClassPathResource("train_ai_teacher_1000.json");
                if (!resource.exists()) {
                    System.out.println("File train_ai_teacher_1000.json not found. Skipping data load.");
                    return;
                }
                inputStream = resource.getInputStream();
            } else {
                inputStream = new java.io.FileInputStream(file);
            }
            
            JsonNode rootNode = objectMapper.readTree(inputStream);

            if (rootNode.isArray()) {
                for (JsonNode entry : rootNode) {
                    processEntry(entry);
                }
            }

            // Tạo bài học và câu hỏi từ dữ liệu đã trích xuất
            createLessonsFromVocab();
            createQuizQuestions();
            addVocabToGame();
            addPracticeTasks();

            System.out.println("✅ Đã load dữ liệu từ train_ai_teacher_1000.json thành công!");
            System.out.println("   - Từ vựng: " + vocabByTopic.size() + " chủ đề");
            System.out.println("   - Câu hỏi quiz: " + quizQuestions.values().stream().mapToInt(List::size).sum() + " câu");
            System.out.println("   - Từ vựng cho game: " + vocabForGame.size() + " từ");
            System.out.println("   - Practice tasks: " + practiceTasks.size() + " tasks");

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi load dữ liệu từ JSON: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    private void processEntry(JsonNode entry) {
        JsonNode messages = entry.get("messages");
        if (messages == null || !messages.isArray()) return;

        String userMessage = null;
        String assistantMessage = null;

        for (int i = 0; i < messages.size(); i++) {
            JsonNode msg = messages.get(i);
            if (msg == null || !msg.has("role") || !msg.has("content")) continue;

            String role = msg.get("role").asText();
            String content = msg.get("content").asText();

            if ("user".equals(role)) {
                userMessage = content;
                // Trích xuất từ vựng từ câu hỏi
                extractVocabularyFromUserMessage(content);
            } else if ("assistant".equals(role)) {
                assistantMessage = content;
                // Trích xuất câu hỏi quiz từ câu trả lời của assistant
                extractQuizQuestionsFromAssistantMessage(content);
                // Trích xuất practice tasks từ câu trả lời
                if (userMessage != null) {
                    extractPracticeTasksFromAssistantMessage(userMessage, content);
                }
            }
        }
    }

    private void extractVocabularyFromUserMessage(String content) {
        // Pattern để tìm từ vựng: "từ 'word' trong chủ đề Topic"
        Pattern pattern = Pattern.compile("từ ['\"](\\w+)['\"] trong chủ đề (\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String word = matcher.group(1).toLowerCase();
            String topic = matcher.group(2);
            vocabByTopic.computeIfAbsent(topic, k -> new HashSet<>()).add(word);
        }

        // Pattern khác: "từ 'word' nghĩa là gì"
        pattern = Pattern.compile("từ ['\"](\\w+)['\"] nghĩa là", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            String word = matcher.group(1).toLowerCase();
            // Thử đoán chủ đề từ từ vựng
            String topic = guessTopicFromWord(word);
            vocabByTopic.computeIfAbsent(topic, k -> new HashSet<>()).add(word);
        }
    }

    private void extractQuizQuestionsFromAssistantMessage(String content) {
        // Parse câu hỏi multiple choice từ format:
        // "1) I ___ to school by bike.\nA. go   B. goes   C. going   D. gone\n✅ Answer: A. go"
        
        if (!content.contains("multiple-choice") && !content.contains("Answer:")) {
            return;
        }

        // Pattern để tìm các câu hỏi
        Pattern questionPattern = Pattern.compile(
            "(\\d+)\\)\\s*([^\\n]+)\\s*\\n" +
            "A\\.\\s*([^\\s]+)\\s+B\\.\\s*([^\\s]+)\\s+C\\.\\s*([^\\s]+)\\s+D\\.\\s*([^\\s]+)" +
            "\\s*[✅✓]?\\s*Answer:\\s*([A-D])\\.\\s*([^\\n]+)",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = questionPattern.matcher(content);
        while (matcher.find()) {
            String questionText = matcher.group(2).trim();
            String optionA = matcher.group(3).trim();
            String optionB = matcher.group(4).trim();
            String optionC = matcher.group(5).trim();
            String optionD = matcher.group(6).trim();
            String correctOption = matcher.group(7).trim();
            String correctAnswer = matcher.group(8).trim();

            QuizData quizData = new QuizData(questionText, correctAnswer);
            quizData.optionA = optionA;
            quizData.optionB = optionB;
            quizData.optionC = optionC;
            quizData.optionD = optionD;
            quizData.correctOption = correctOption;

            quizQuestions.computeIfAbsent("general", k -> new ArrayList<>()).add(quizData);
        }
    }

    private void extractPracticeTasksFromAssistantMessage(String userMessage, String assistantMessage) {
        // Tìm các practice tasks từ các câu hỏi về sửa câu hoặc viết câu
        if (userMessage == null || assistantMessage == null) return;

        // Pattern: "Sửa giúp em câu này: ..."
        if (userMessage.contains("Sửa giúp em câu này") || userMessage.contains("Sửa câu")) {
            Pattern pattern = Pattern.compile("Sửa giúp em câu này:?\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(userMessage);
            if (matcher.find()) {
                String wrongSentence = matcher.group(1).trim();
                
                // Tìm câu đúng từ assistant message
                Pattern correctPattern = Pattern.compile("\\*\\*([^*]+)\\*\\*|➡️\\s*\\*\\*([^*]+)\\*\\*", Pattern.MULTILINE);
                Matcher correctMatcher = correctPattern.matcher(assistantMessage);
                if (correctMatcher.find()) {
                    String correctSentence = correctMatcher.group(1) != null ? 
                        correctMatcher.group(1).trim() : correctMatcher.group(2).trim();
                    
                    practiceTasks.add(new PracticeTaskData(
                        "Sửa câu: " + wrongSentence,
                        correctSentence
                    ));
                }
            }
        }

        // Pattern: "Viết câu..." hoặc "Hãy viết câu..."
        if (userMessage.contains("Viết câu") || userMessage.contains("Hãy viết")) {
            Pattern pattern = Pattern.compile("(?:Viết|Hãy viết)\\s*câu:?\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(userMessage);
            if (matcher.find()) {
                String question = matcher.group(1).trim();
                
                // Tìm câu trả lời mẫu từ assistant message
                Pattern answerPattern = Pattern.compile("\\*\\*([^*]+)\\*\\*|Câu mẫu:?\\s*([^\\n]+)", Pattern.MULTILINE);
                Matcher answerMatcher = answerPattern.matcher(assistantMessage);
                if (answerMatcher.find()) {
                    String answer = answerMatcher.group(1) != null ? 
                        answerMatcher.group(1).trim() : answerMatcher.group(2).trim();
                    
                    practiceTasks.add(new PracticeTaskData(question, answer));
                }
            }
        }
    }

    private String guessTopicFromWord(String word) {
        // Đoán chủ đề dựa trên từ vựng
        if (word.matches("(cat|dog|bird|fish|duck|cow|horse|pig)")) return "Animals";
        if (word.matches("(red|blue|green|yellow|orange|purple|pink|black|white)")) return "Colors";
        if (word.matches("(apple|banana|bread|milk|rice|noodles|chicken|egg)")) return "Food";
        if (word.matches("(father|mother|brother|sister|grandfather|grandmother)")) return "Family";
        if (word.matches("(one|two|three|four|five|six|seven|eight|nine|ten)")) return "Numbers";
        if (word.matches("(pencil|pen|book|school|teacher|student|desk|chair)")) return "School";
        if (word.matches("(watch|phone|gift|suitcase|bag|key)")) return "Personal";
        return "General";
    }

    private void createLessonsFromVocab() {
        // Tạo bài học từ các từ vựng đã trích xuất
        for (Map.Entry<String, Set<String>> entry : vocabByTopic.entrySet()) {
            String topic = entry.getKey();
            Set<String> words = entry.getValue();

            if (words.size() < 3) continue; // Bỏ qua nếu ít hơn 3 từ

            // Kiểm tra xem đã có bài học với chủ đề này chưa
            List<Lesson> existing = lessonRepo.findAll().stream()
                    .filter(l -> l.getTitle() != null && l.getTitle().toLowerCase().contains(topic.toLowerCase()))
                    .toList();

            if (!existing.isEmpty()) continue; // Đã có bài học

            Lesson lesson = new Lesson();
            lesson.setTitle(topic + " Vocabulary");
            lesson.setDescription("Từ vựng về chủ đề " + topic);
            lesson.setXpReward(10);
            lesson.setLevel("GRADE2");
            lesson.setOrderIndex(100); // Đặt ở cuối
            lesson.setPartName("PHẦN BỔ SUNG");

            // Tạo HTML content
            StringBuilder html = new StringBuilder("<h3>Từ vựng</h3><ul>");
            for (String word : words) {
                html.append("<li><b>").append(word).append("</b> – [nghĩa tiếng Việt]</li>");
            }
            html.append("</ul>");

            lesson.setContentHtml(html.toString());
            lessonRepo.save(lesson);
        }
    }

    private void createQuizQuestions() {
        // Tạo câu hỏi quiz từ dữ liệu đã trích xuất và lưu vào database
        List<Lesson> allLessons = lessonRepo.findAll();
        if (allLessons.isEmpty()) return;

        Lesson generalLesson = allLessons.stream()
                .filter(l -> l.getTitle() != null && l.getTitle().toLowerCase().contains("vocabulary"))
                .findFirst()
                .orElse(allLessons.get(0));

        int created = 0;
        for (List<QuizData> quizList : quizQuestions.values()) {
            for (QuizData quizData : quizList) {
                if (quizData.optionA == null || quizData.correctOption == null) continue;

                // Kiểm tra xem đã có câu hỏi này chưa
                List<QuizQuestion> existing = quizRepo.findByLesson(generalLesson).stream()
                        .filter(q -> q.getQuestion() != null && q.getQuestion().equals(quizData.question))
                        .toList();
                
                if (!existing.isEmpty()) continue;

                QuizQuestion quiz = new QuizQuestion();
                quiz.setLesson(generalLesson);
                quiz.setQuestion(quizData.question);
                quiz.setOptionA(quizData.optionA);
                quiz.setOptionB(quizData.optionB);
                quiz.setOptionC(quizData.optionC);
                quiz.setOptionD(quizData.optionD);
                quiz.setCorrectOption(quizData.correctOption);
                quiz.setExplanation("Đáp án đúng: " + quizData.answer);

                quizRepo.save(quiz);
                created++;
            }
        }
        
        if (created > 0) {
            System.out.println("   ✅ Đã tạo " + created + " câu hỏi quiz mới");
        }
    }

    private void addVocabToGame() {
        // Thêm từ vựng vào vocabForGame map với emoji tương ứng
        Map<String, String> emojiMap = new HashMap<>();
        emojiMap.put("cat", "🐱"); emojiMap.put("dog", "🐶"); emojiMap.put("bird", "🐦"); emojiMap.put("fish", "🐟");
        emojiMap.put("red", "🔴"); emojiMap.put("blue", "🔵"); emojiMap.put("green", "🟢"); emojiMap.put("yellow", "🟡");
        emojiMap.put("apple", "🍎"); emojiMap.put("banana", "🍌"); emojiMap.put("bread", "🍞"); emojiMap.put("milk", "🥛");
        emojiMap.put("father", "👨"); emojiMap.put("mother", "👩"); emojiMap.put("brother", "👦"); emojiMap.put("sister", "👧");
        emojiMap.put("one", "1️⃣"); emojiMap.put("two", "2️⃣"); emojiMap.put("three", "3️⃣"); emojiMap.put("four", "4️⃣");
        emojiMap.put("pencil", "✏️"); emojiMap.put("book", "📖"); emojiMap.put("school", "🏫"); emojiMap.put("watch", "⌚");
        emojiMap.put("phone", "📱"); emojiMap.put("gift", "🎁"); emojiMap.put("suitcase", "🧳");

        for (Set<String> words : vocabByTopic.values()) {
            for (String word : words) {
                String emoji = emojiMap.get(word.toLowerCase());
                if (emoji != null) {
                    vocabForGame.put(word.toLowerCase(), emoji);
                }
            }
        }
    }

    private void addPracticeTasks() {
        // Lưu practice tasks vào một nơi có thể truy cập được
        // Có thể lưu vào database hoặc file, tạm thời chỉ log
        if (!practiceTasks.isEmpty()) {
            System.out.println("   ✅ Đã trích xuất " + practiceTasks.size() + " practice tasks");
            // Có thể mở rộng để lưu vào database sau
        }
    }

    // Helper class để lưu dữ liệu quiz
    private static class QuizData {
        String question;
        String answer;
        String optionA;
        String optionB;
        String optionC;
        String optionD;
        String correctOption;

        QuizData(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    // Helper class để lưu practice task
    private static class PracticeTaskData {
        String question;
        String expected;

        PracticeTaskData(String question, String expected) {
            this.question = question;
            this.expected = expected;
        }
    }

    // Public method để lấy từ vựng cho game (có thể dùng từ VocabGameService)
    public Map<String, String> getVocabForGame() {
        return new HashMap<>(vocabForGame);
    }

    // Public method để lấy practice tasks
    public List<PracticeTaskData> getPracticeTasks() {
        return new ArrayList<>(practiceTasks);
    }
}

