package com.example.duokid.service;

import com.example.duokid.model.Achievement;
import com.example.duokid.model.DailyGoalProgress;
import com.example.duokid.model.User;
import com.example.duokid.repo.AchievementRepository;
import com.example.duokid.repo.DailyGoalProgressRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyGoalService {

    private final DailyGoalProgressRepository repo;
    private final AchievementRepository achievementRepo;

    public DailyGoalService(DailyGoalProgressRepository repo,
                           AchievementRepository achievementRepo) {
        this.repo = repo;
        this.achievementRepo = achievementRepo;
    }

    // Lấy record ngày hôm nay hoặc tạo mới
    public DailyGoalProgress getTodayProgress(User user) {
        LocalDate today = LocalDate.now();

        return repo.findByUserAndDate(user, today).orElseGet(() -> {
            DailyGoalProgress d = new DailyGoalProgress();
            d.setUser(user);
            d.setDate(today);
            d.setLessonCompleted(false);
            d.setQuizCompleted(false);
            return repo.save(d);
        });
    }

    // Đánh dấu đã hoàn thành 1 bài học
    public void setLessonCompleted(User user) {
        DailyGoalProgress d = getTodayProgress(user);
        d.setLessonCompleted(true);
        repo.save(d);
    }

    // Đánh dấu đã hoàn thành 1 mini test
    public void setQuizCompleted(User user) {
        DailyGoalProgress d = getTodayProgress(user);
        d.setQuizCompleted(true);
        repo.save(d);
    }

    // Alias methods for compatibility
    public DailyGoalProgress getOrCreateToday(User user) {
        return getTodayProgress(user);
    }

    public void markLessonCompleted(User user) {
        setLessonCompleted(user);
    }

    public void markQuizCompleted(User user) {
        setQuizCompleted(user);
    }

    // Get achievements for user
    public List<Achievement> getAchievements(User user) {
        return achievementRepo.findByUser(user);
    }

    // Check and award streak badge
    public void checkStreakBadge(User user) {
        if (user.getStreak() >= 7) {
            // Check if user already has this achievement
            boolean hasStreak7 = achievementRepo.findByUserAndName(user, "7 Day Streak").isPresent();
            if (!hasStreak7) {
                Achievement achievement = new Achievement();
                achievement.setUser(user);
                achievement.setName("7 Day Streak");
                achievement.setDescription("Duy trì streak 7 ngày liên tiếp!");
                achievement.setEarnedDate(LocalDate.now());
                achievementRepo.save(achievement);
            }
        }
        if (user.getStreak() >= 30) {
            boolean hasStreak30 = achievementRepo.findByUserAndName(user, "30 Day Streak").isPresent();
            if (!hasStreak30) {
                Achievement achievement = new Achievement();
                achievement.setUser(user);
                achievement.setName("30 Day Streak");
                achievement.setDescription("Duy trì streak 30 ngày liên tiếp!");
                achievement.setEarnedDate(LocalDate.now());
                achievementRepo.save(achievement);
            }
        }
    }
}
