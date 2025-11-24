package com.example.duokid.repo;

import com.example.duokid.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByLevel(String level);
    Optional<Test> findByLevelAndAfterLessons(String level, Integer afterLessons);
}

