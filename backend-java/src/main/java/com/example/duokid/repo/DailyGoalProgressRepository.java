package com.example.duokid.repo;


import com.example.duokid.model.DailyGoalProgress;
import com.example.duokid.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyGoalProgressRepository extends JpaRepository<DailyGoalProgress, Long> {

    Optional<DailyGoalProgress> findByUserAndDate(User user, LocalDate date);

}
