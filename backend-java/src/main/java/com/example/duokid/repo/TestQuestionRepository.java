package com.example.duokid.repo;

import com.example.duokid.model.Test;
import com.example.duokid.model.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, Long> {
    List<TestQuestion> findByTest(Test test);
}

