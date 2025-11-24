package com.example.duokid.repo;

import com.example.duokid.model.Grammar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrammarRepository extends JpaRepository<Grammar, Long> {
    List<Grammar> findByLevel(String level);
    List<Grammar> findByLevelOrderByOrderIndex(String level);
}

