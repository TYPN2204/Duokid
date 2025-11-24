package com.example.duokid.service;

import com.example.duokid.model.Test;
import com.example.duokid.model.TestQuestion;
import com.example.duokid.model.User;
import com.example.duokid.repo.TestQuestionRepository;
import com.example.duokid.repo.TestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TestService {

    private final TestRepository testRepo;
    private final TestQuestionRepository testQuestionRepo;
    private final LessonProgressService lessonProgressService;

    public TestService(TestRepository testRepo,
                      TestQuestionRepository testQuestionRepo,
                      LessonProgressService lessonProgressService) {
        this.testRepo = testRepo;
        this.testQuestionRepo = testQuestionRepo;
        this.lessonProgressService = lessonProgressService;
    }

    public TestRepository getTestRepo() {
        return testRepo;
    }

    /**
     * Get test that should appear after completing N lessons
     */
    public Optional<Test> getTestForUser(User user, String level) {
        int completedLessons = lessonProgressService.getCompletedLessonsCount(user);
        
        // Find test that appears after the number of completed lessons
        // e.g., if user completed 5 lessons, find test with afterLessons = 5
        return testRepo.findByLevelAndAfterLessons(level, completedLessons);
    }

    /**
     * Get all available tests for a level
     */
    public List<Test> getTestsByLevel(String level) {
        return testRepo.findByLevel(level);
    }

    /**
     * Get questions for a test
     */
    public List<TestQuestion> getTestQuestions(Test test) {
        return testQuestionRepo.findByTest(test);
    }

    /**
     * Calculate score and determine if passed
     */
    public TestResult calculateScore(Test test, Map<Long, String> answers) {
        List<TestQuestion> questions = getTestQuestions(test);
        int total = questions.size();
        int correct = 0;

        for (TestQuestion question : questions) {
            String userAnswer = answers.get(question.getId());
            if (userAnswer != null && userAnswer.equalsIgnoreCase(question.getCorrectOption())) {
                correct++;
            }
        }

        int score = total > 0 ? (correct * 100 / total) : 0;
        boolean passed = score >= test.getPassingScore();

        return new TestResult(score, correct, total, passed);
    }

    public static class TestResult {
        private final int score;
        private final int correct;
        private final int total;
        private final boolean passed;

        public TestResult(int score, int correct, int total, boolean passed) {
            this.score = score;
            this.correct = correct;
            this.total = total;
            this.passed = passed;
        }

        public int getScore() { return score; }
        public int getCorrect() { return correct; }
        public int getTotal() { return total; }
        public boolean isPassed() { return passed; }
    }
}

