package com.example.duokid.repo;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByLesson(Lesson lesson);
}
