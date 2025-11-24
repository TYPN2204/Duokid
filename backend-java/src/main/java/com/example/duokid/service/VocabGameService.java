package com.example.duokid.service;

import com.example.duokid.model.User;
import com.example.duokid.model.VocabGameScore;
import com.example.duokid.repo.VocabGameScoreRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class VocabGameService {

    public record Question(
            Long id,
            String prompt,
            List<String> options,
            String correctAnswer,
            String hint
    ) {}

    private static final int QUESTIONS_PER_ROUND = 5;
    private static final Map<Long, Question> QUESTION_BANK = buildBank();

    private final VocabGameScoreRepository scoreRepository;

    public VocabGameService(VocabGameScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    public List<Question> generateRound() {
        List<Question> questions = new ArrayList<>(QUESTION_BANK.values());
        Collections.shuffle(questions);
        return questions.subList(0, Math.min(QUESTIONS_PER_ROUND, questions.size()));
    }

    public Optional<Question> findQuestion(Long id) {
        return Optional.ofNullable(QUESTION_BANK.get(id));
    }

    public int calculatePoints(List<Long> questionIds, Map<Long, String> answers) {
        int points = 0;
        for (Long qId : questionIds) {
            Question question = QUESTION_BANK.get(qId);
            if (question == null) continue;
            String userAnswer = answers.get(qId);
            if (userAnswer != null && userAnswer.equalsIgnoreCase(question.correctAnswer())) {
                points += 10;
            }
        }
        return points;
    }

    public long countCorrect(List<Long> questionIds, Map<Long, String> answers) {
        return questionIds.stream()
                .map(QUESTION_BANK::get)
                .filter(Objects::nonNull)
                .filter(q -> {
                    String ans = answers.get(q.id());
                    return ans != null && ans.equalsIgnoreCase(q.correctAnswer());
                })
                .count();
    }

    public VocabGameScore recordScore(User user, int roundPoints) {
        if (roundPoints <= 0) {
            return scoreRepository.findByUser(user).orElseGet(() -> {
                VocabGameScore score = new VocabGameScore();
                score.setUser(user);
                score.setBestRoundScore(0);
                score.setTotalPoints(0);
                score.setLastPlayed(LocalDateTime.now());
                return scoreRepository.save(score);
            });
        }

        VocabGameScore score = scoreRepository.findByUser(user)
                .orElseGet(() -> {
                    VocabGameScore s = new VocabGameScore();
                    s.setUser(user);
                    s.setTotalPoints(0);
                    s.setBestRoundScore(0);
                    return s;
                });

        score.setTotalPoints(score.getTotalPoints() + roundPoints);
        score.setBestRoundScore(Math.max(score.getBestRoundScore(), roundPoints));
        score.setLastPlayed(LocalDateTime.now());
        return scoreRepository.save(score);
    }

    public List<VocabGameScore> getLeaderboard() {
        return scoreRepository.findTop10ByOrderByTotalPointsDesc();
    }

    // Vocabulary item record for game interface
    public record VocabItem(String word, String emoji, String ipa) {}

    // Get vocabulary items by category
    public List<VocabItem> getVocabItemsByCategory(String category) {
        Map<String, List<VocabItem>> vocabBank = buildVocabBank();
        return vocabBank.getOrDefault(category, vocabBank.get("Personal"));
    }

    private static Map<String, List<VocabItem>> buildVocabBank() {
        Map<String, List<VocabItem>> bank = new LinkedHashMap<>();
        
        bank.put("Personal", List.of(
            new VocabItem("gift", "🎁", "/ɡɪft/"),
            new VocabItem("suitcase", "🧳", "/ˈsuːtkeɪs/"),
            new VocabItem("phone", "📱", "/foʊn/"),
            new VocabItem("watch", "⌚", "/wɑːtʃ/")
        ));
        
        bank.put("Animals", List.of(
            new VocabItem("cat", "🐱", "/kæt/"),
            new VocabItem("dog", "🐶", "/dɔːɡ/"),
            new VocabItem("bird", "🐦", "/bɜːrd/"),
            new VocabItem("fish", "🐟", "/fɪʃ/")
        ));
        
        bank.put("Colors", List.of(
            new VocabItem("red", "🔴", "/red/"),
            new VocabItem("blue", "🔵", "/bluː/"),
            new VocabItem("green", "🟢", "/ɡriːn/"),
            new VocabItem("yellow", "🟡", "/ˈjeloʊ/")
        ));
        
        bank.put("Food", List.of(
            new VocabItem("apple", "🍎", "/ˈæpl/"),
            new VocabItem("banana", "🍌", "/bəˈnænə/"),
            new VocabItem("bread", "🍞", "/bred/"),
            new VocabItem("milk", "🥛", "/mɪlk/")
        ));
        
        bank.put("Family", List.of(
            new VocabItem("father", "👨", "/ˈfɑːðər/"),
            new VocabItem("mother", "👩", "/ˈmʌðər/"),
            new VocabItem("brother", "👦", "/ˈbrʌðər/"),
            new VocabItem("sister", "👧", "/ˈsɪstər/")
        ));
        
        bank.put("Numbers", List.of(
            new VocabItem("one", "1️⃣", "/wʌn/"),
            new VocabItem("two", "2️⃣", "/tuː/"),
            new VocabItem("three", "3️⃣", "/θriː/"),
            new VocabItem("four", "4️⃣", "/fɔːr/")
        ));
        
        return bank;
    }

    private static Map<Long, Question> buildBank() {
        AtomicLong counter = new AtomicLong(1);
        List<Question> questions = List.of(
                question(counter, "Từ nào nghĩa là \"con mèo\"?", "cat", List.of("cat", "dog", "bird", "duck")),
                question(counter, "Từ nào nghĩa là \"con chó\"?", "dog", List.of("cat", "dog", "fish", "cow")),
                question(counter, "Từ \"red\" nghĩa là màu gì?", "red", List.of("blue", "green", "red", "yellow")),
                question(counter, "Từ \"brother\" nghĩa là?", "brother", List.of("mother", "sister", "brother", "grandma")),
                question(counter, "\"Good morning\" dùng khi nào?", "morning", List.of("night", "morning", "evening", "midnight")),
                question(counter, "Từ \"fish\" là con gì?", "fish", List.of("cat", "dog", "fish", "duck")),
                question(counter, "Từ \"ten\" là số mấy?", "ten", List.of("two", "five", "ten", "twelve")),
                question(counter, "\"I have five pens\" nghĩa là gì?", "Tôi có 5 cây bút", List.of("Tôi có 5 cây bút", "Tôi có 5 quyển sách", "Tôi có 5 con mèo", "Tôi có 5 cái ghế")),
                question(counter, "Từ nào nghĩa là \"màu xanh dương\"?", "blue", List.of("blue", "red", "yellow", "green")),
                question(counter, "Từ \"grandmother\" nghĩa là?", "bà", List.of("ông", "mẹ", "bà", "chị"))
        );

        Map<Long, Question> bank = new LinkedHashMap<>();
        for (Question q : questions) {
            bank.put(q.id(), q);
        }
        return bank;
    }

    private static Question question(AtomicLong counter,
                                     String prompt,
                                     String correct,
                                     List<String> options) {
        List<String> shuffledOptions = new ArrayList<>(options);
        Collections.shuffle(shuffledOptions);
        return new Question(counter.getAndIncrement(), prompt, shuffledOptions, correct, null);
    }
}

