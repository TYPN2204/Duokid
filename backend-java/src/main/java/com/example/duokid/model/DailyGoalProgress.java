package com.example.duokid.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_goal_progress")
public class DailyGoalProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private LocalDate date;

    private boolean lessonCompleted;
    private boolean quizCompleted;

    public DailyGoalProgress() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public boolean isLessonCompleted() { return lessonCompleted; }
    public void setLessonCompleted(boolean lessonCompleted) { this.lessonCompleted = lessonCompleted; }

    public boolean isQuizCompleted() { return quizCompleted; }
    public void setQuizCompleted(boolean quizCompleted) { this.quizCompleted = quizCompleted; }
}
