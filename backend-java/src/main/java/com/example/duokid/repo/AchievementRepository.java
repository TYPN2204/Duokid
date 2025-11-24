package com.example.duokid.repo;

import com.example.duokid.model.Achievement;
import com.example.duokid.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByUser(User user);
    Optional<Achievement> findByUserAndCode(User user, String code);
    Optional<Achievement> findByUserAndName(User user, String name);
}
