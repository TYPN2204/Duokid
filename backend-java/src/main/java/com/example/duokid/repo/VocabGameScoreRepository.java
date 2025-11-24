package com.example.duokid.repo;

import com.example.duokid.model.User;
import com.example.duokid.model.VocabGameScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VocabGameScoreRepository extends JpaRepository<VocabGameScore, Long> {

    Optional<VocabGameScore> findByUser(User user);

    List<VocabGameScore> findTop10ByOrderByTotalPointsDesc();
}

