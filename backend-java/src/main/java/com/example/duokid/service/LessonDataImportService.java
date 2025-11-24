package com.example.duokid.service;

import com.example.duokid.model.Grammar;
import com.example.duokid.model.Lesson;
import com.example.duokid.repo.GrammarRepository;
import com.example.duokid.repo.LessonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class LessonDataImportService {

    private final LessonRepository lessonRepo;
    private final GrammarRepository grammarRepo;
    private final QuizGenerationService quizGenerationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LessonDataImportService(LessonRepository lessonRepo, 
                                   GrammarRepository grammarRepo,
                                   QuizGenerationService quizGenerationService) {
        this.lessonRepo = lessonRepo;
        this.grammarRepo = grammarRepo;
        this.quizGenerationService = quizGenerationService;
    }

    /**
     * Import lessons and grammar from JSON file
     * JSON format:
     * {
     *   "lessons": [
     *     {
     *       "title": "Lesson title",
     *       "description": "Description",
     *       "level": "GRADE3",
     *       "lessonType": "VOCABULARY",
     *       "contentHtml": "<ul><li>word1 – nghĩa1</li>...</ul>",
     *       "orderIndex": 0,
     *       "partName": "PHẦN 1, CỬA 1",
     *       "xpReward": 10
     *     }
     *   ],
     *   "grammar": [
     *     {
     *       "title": "Grammar title",
     *       "description": "Description",
     *       "level": "GRADE3",
     *       "topic": "Present Simple",
     *       "contentHtml": "Grammar explanation...",
     *       "examples": "I play, You play, He plays",
     *       "orderIndex": 1,
     *       "partName": "PHẦN 1, CỬA 2",
     *       "xpReward": 15
     *     }
     *   ]
     * }
     */
    public ImportResult importFromJson(InputStream inputStream) throws Exception {
        JsonNode rootNode = objectMapper.readTree(inputStream);
        
        int lessonsImported = 0;
        int grammarImported = 0;
        List<String> errors = new ArrayList<>();

        // Import lessons
        if (rootNode.has("lessons") && rootNode.get("lessons").isArray()) {
            for (JsonNode lessonNode : rootNode.get("lessons")) {
                try {
                    Lesson lesson = new Lesson();
                    lesson.setTitle(lessonNode.get("title").asText());
                    lesson.setDescription(lessonNode.has("description") ? lessonNode.get("description").asText() : "");
                    lesson.setLevel(lessonNode.get("level").asText());
                    lesson.setLessonType(lessonNode.has("lessonType") ? lessonNode.get("lessonType").asText() : "VOCABULARY");
                    lesson.setContentHtml(lessonNode.has("contentHtml") ? lessonNode.get("contentHtml").asText() : "");
                    lesson.setOrderIndex(lessonNode.has("orderIndex") ? lessonNode.get("orderIndex").asInt() : null);
                    lesson.setPartName(lessonNode.has("partName") ? lessonNode.get("partName").asText() : null);
                    lesson.setXpReward(lessonNode.has("xpReward") ? lessonNode.get("xpReward").asInt() : 10);

                    // Check if lesson already exists
                    List<Lesson> existing = lessonRepo.findAll().stream()
                            .filter(l -> l.getTitle().equals(lesson.getTitle()) && 
                                       l.getLevel().equals(lesson.getLevel()))
                            .toList();
                    
                    if (existing.isEmpty()) {
                        lessonRepo.save(lesson);
                        lessonsImported++;
                        
                        // Tự động tạo quiz cho lesson vừa import
                        try {
                            quizGenerationService.createQuizForLesson(lesson);
                        } catch (Exception e) {
                            // Log nhưng không fail import
                            System.err.println("Không thể tạo quiz cho lesson: " + lesson.getTitle() + " - " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    errors.add("Error importing lesson: " + e.getMessage());
                }
            }
        }

        // Import grammar
        if (rootNode.has("grammar") && rootNode.get("grammar").isArray()) {
            for (JsonNode grammarNode : rootNode.get("grammar")) {
                try {
                    Grammar grammar = new Grammar();
                    grammar.setTitle(grammarNode.get("title").asText());
                    grammar.setDescription(grammarNode.has("description") ? grammarNode.get("description").asText() : "");
                    grammar.setLevel(grammarNode.get("level").asText());
                    grammar.setTopic(grammarNode.has("topic") ? grammarNode.get("topic").asText() : "");
                    grammar.setContentHtml(grammarNode.has("contentHtml") ? grammarNode.get("contentHtml").asText() : "");
                    grammar.setExamples(grammarNode.has("examples") ? grammarNode.get("examples").asText() : "");
                    grammar.setOrderIndex(grammarNode.has("orderIndex") ? grammarNode.get("orderIndex").asInt() : null);
                    grammar.setPartName(grammarNode.has("partName") ? grammarNode.get("partName").asText() : null);
                    grammar.setXpReward(grammarNode.has("xpReward") ? grammarNode.get("xpReward").asInt() : 15);

                    // Check if grammar already exists
                    List<Grammar> existing = grammarRepo.findAll().stream()
                            .filter(g -> g.getTitle().equals(grammar.getTitle()) && 
                                       g.getLevel().equals(grammar.getLevel()))
                            .toList();
                    
                    if (existing.isEmpty()) {
                        grammarRepo.save(grammar);
                        grammarImported++;
                    }
                } catch (Exception e) {
                    errors.add("Error importing grammar: " + e.getMessage());
                }
            }
        }

        return new ImportResult(lessonsImported, grammarImported, errors);
    }

    public static class ImportResult {
        private final int lessonsImported;
        private final int grammarImported;
        private final List<String> errors;

        public ImportResult(int lessonsImported, int grammarImported, List<String> errors) {
            this.lessonsImported = lessonsImported;
            this.grammarImported = grammarImported;
            this.errors = errors;
        }

        public int getLessonsImported() { return lessonsImported; }
        public int getGrammarImported() { return grammarImported; }
        public List<String> getErrors() { return errors; }
        public boolean isSuccess() { return errors.isEmpty(); }
    }
}

