package com.example.duokid.repo;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.User;
import com.example.duokid.model.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {
    Optional<UserLessonProgress> findByUserAndLesson(User user, Lesson lesson);
    List<UserLessonProgress> findByUser(User user);
    
    @Query("SELECT COUNT(ulp) FROM UserLessonProgress ulp WHERE ulp.user = :user AND ulp.completed = true")
    int countCompletedLessonsByUser(User user);
}

