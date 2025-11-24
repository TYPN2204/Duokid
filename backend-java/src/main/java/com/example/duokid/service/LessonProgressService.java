package com.example.duokid.service;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.User;
import com.example.duokid.model.UserLessonProgress;
import com.example.duokid.repo.LessonRepository;
import com.example.duokid.repo.UserLessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LessonProgressService {

    private final UserLessonProgressRepository progressRepo;
    private final LessonRepository lessonRepo;
    private final UserService userService;

    public LessonProgressService(UserLessonProgressRepository progressRepo,
                                 LessonRepository lessonRepo,
                                 UserService userService) {
        this.progressRepo = progressRepo;
        this.lessonRepo = lessonRepo;
        this.userService = userService;
    }

    public List<Lesson> getLessonPathForUser(User user) {
        List<Lesson> allLessons = lessonRepo.findAll();
        List<UserLessonProgress> userProgress = progressRepo.findByUser(user);
        
        Map<Long, UserLessonProgress> progressMap = userProgress.stream()
            .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p));

        // Sort by orderIndex
        allLessons.sort(Comparator.comparing(Lesson::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())));

        // Determine unlock status
        for (int i = 0; i < allLessons.size(); i++) {
            Lesson lesson = allLessons.get(i);
            UserLessonProgress progress = progressMap.get(lesson.getId());
            
            if (progress == null) {
                // First lesson is always unlocked
                if (i == 0) {
                    progress = new UserLessonProgress();
                    progress.setUser(user);
                    progress.setLesson(lesson);
                    progress.setUnlocked(true);
                    progress.setCompleted(false);
                    progressRepo.save(progress);
                } else {
                    // Check if previous lesson is completed
                    Lesson prevLesson = allLessons.get(i - 1);
                    UserLessonProgress prevProgress = progressMap.get(prevLesson.getId());
                    boolean unlocked = prevProgress != null && prevProgress.isCompleted();
                    
                    progress = new UserLessonProgress();
                    progress.setUser(user);
                    progress.setLesson(lesson);
                    progress.setUnlocked(unlocked);
                    progress.setCompleted(false);
                    progressRepo.save(progress);
                }
            }
        }

        return allLessons;
    }

    public Map<Long, UserLessonProgress> getProgressMap(User user) {
        List<UserLessonProgress> progressList = progressRepo.findByUser(user);
        return progressList.stream()
            .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p));
    }

    @Transactional
    public GateRewardResult markLessonCompleted(User user, Lesson lesson, int score) {
        UserLessonProgress progress = progressRepo.findByUserAndLesson(user, lesson)
            .orElseGet(() -> {
                UserLessonProgress p = new UserLessonProgress();
                p.setUser(user);
                p.setLesson(lesson);
                return p;
            });

        progress.setCompleted(true);
        progress.setScore(score);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setUnlocked(true);
        progressRepo.save(progress);

        // Unlock next lesson
        unlockNextLesson(user, lesson);
        
        // Kiểm tra xem đã hoàn thành hết bài trong một ô cửa chưa
        return checkAndRewardGateCompletion(user, lesson);
    }
    
    /**
     * Result class cho gate reward
     */
    public static class GateRewardResult {
        private final boolean gateCompleted;
        private final int gateNumber;
        private final int gemsReward;
        private final int xpReward;
        private final boolean nextGateUnlocked;
        private final int nextGateNumber;
        
        public GateRewardResult(boolean gateCompleted, int gateNumber, int gemsReward, int xpReward, 
                               boolean nextGateUnlocked, int nextGateNumber) {
            this.gateCompleted = gateCompleted;
            this.gateNumber = gateNumber;
            this.gemsReward = gemsReward;
            this.xpReward = xpReward;
            this.nextGateUnlocked = nextGateUnlocked;
            this.nextGateNumber = nextGateNumber;
        }
        
        public boolean isGateCompleted() { return gateCompleted; }
        public int getGateNumber() { return gateNumber; }
        public int getGemsReward() { return gemsReward; }
        public int getXpReward() { return xpReward; }
        public boolean isNextGateUnlocked() { return nextGateUnlocked; }
        public int getNextGateNumber() { return nextGateNumber; }
    }
    
    /**
     * Kiểm tra xem user đã hoàn thành hết bài trong một ô cửa chưa
     * Nếu có, trao rương thưởng và unlock ô cửa tiếp theo
     */
    private GateRewardResult checkAndRewardGateCompletion(User user, Lesson completedLesson) {
        String partName = completedLesson.getPartName();
        if (partName == null || !partName.contains("CỬA")) {
            return new GateRewardResult(false, 0, 0, 0, false, 0);
        }
        
        // Extract gate number từ partName (ví dụ: "PHẦN 1, CỬA 1" -> gate 1)
        int currentGate = extractGateNumber(partName);
        if (currentGate <= 0 || currentGate > 5) {
            return new GateRewardResult(false, 0, 0, 0, false, 0);
        }
        
        // Lấy tất cả bài học trong ô cửa hiện tại
        List<Lesson> gateLessons = lessonRepo.findAll().stream()
                .filter(l -> partName.equals(l.getPartName()))
                .sorted(Comparator.comparing(Lesson::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        
        // Kiểm tra xem đã hoàn thành hết chưa
        List<UserLessonProgress> gateProgress = progressRepo.findByUser(user).stream()
                .filter(p -> gateLessons.stream().anyMatch(l -> l.getId().equals(p.getLesson().getId())))
                .toList();
        
        long completedCount = gateProgress.stream()
                .filter(UserLessonProgress::isCompleted)
                .count();
        
        // Nếu đã hoàn thành hết bài trong ô cửa
        if (completedCount == gateLessons.size() && gateLessons.size() > 0) {
            // Trao rương thưởng
            int gemsReward = 50;
            int xpReward = 100;
            awardGateReward(user, currentGate, gemsReward, xpReward);
            
            // Unlock ô cửa tiếp theo (nếu có)
            boolean nextGateUnlocked = false;
            int nextGateNumber = 0;
            if (currentGate < 5) {
                unlockNextGate(user, currentGate + 1);
                nextGateUnlocked = true;
                nextGateNumber = currentGate + 1;
            }
            
            return new GateRewardResult(true, currentGate, gemsReward, xpReward, nextGateUnlocked, nextGateNumber);
        }
        
        return new GateRewardResult(false, 0, 0, 0, false, 0);
    }
    
    /**
     * Extract gate number từ partName
     */
    private int extractGateNumber(String partName) {
        if (partName == null) return 0;
        // Tìm "CỬA X" trong partName
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("CỬA\\s*(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(partName);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * Trao rương thưởng khi hoàn thành một ô cửa
     */
    private void awardGateReward(User user, int gate, int gemsReward, int xpReward) {
        user.setGems(user.getGems() + gemsReward);
        user.setXp(user.getXp() + xpReward);
        
        // Lưu user
        userService.save(user);
        
        System.out.println("🎁 Rương thưởng Ô cửa " + gate + ": +" + gemsReward + " gems, +" + xpReward + " XP cho user " + user.getDisplayName());
    }
    
    /**
     * Unlock tất cả bài học trong ô cửa tiếp theo
     */
    private void unlockNextGate(User user, int nextGate) {
        String nextPartName = "PHẦN 1, CỬA " + nextGate;
        
        List<Lesson> nextGateLessons = lessonRepo.findAll().stream()
                .filter(l -> nextPartName.equals(l.getPartName()))
                .sorted(Comparator.comparing(Lesson::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        
        // Unlock bài học đầu tiên của ô cửa tiếp theo
        if (!nextGateLessons.isEmpty()) {
            Lesson firstLesson = nextGateLessons.get(0);
            Optional<UserLessonProgress> progress = progressRepo.findByUserAndLesson(user, firstLesson);
            
            if (progress.isEmpty()) {
                UserLessonProgress newProgress = new UserLessonProgress();
                newProgress.setUser(user);
                newProgress.setLesson(firstLesson);
                newProgress.setUnlocked(true);
                newProgress.setCompleted(false);
                progressRepo.save(newProgress);
            } else {
                UserLessonProgress existingProgress = progress.get();
                if (!existingProgress.isUnlocked()) {
                    existingProgress.setUnlocked(true);
                    progressRepo.save(existingProgress);
                }
            }
        }
    }

    private void unlockNextLesson(User user, Lesson completedLesson) {
        List<Lesson> allLessons = lessonRepo.findAll();
        allLessons.sort(Comparator.comparing(Lesson::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())));
        
        for (int i = 0; i < allLessons.size(); i++) {
            if (allLessons.get(i).getId().equals(completedLesson.getId()) && i + 1 < allLessons.size()) {
                Lesson nextLesson = allLessons.get(i + 1);
                Optional<UserLessonProgress> nextProgress = progressRepo.findByUserAndLesson(user, nextLesson);
                
                if (nextProgress.isEmpty()) {
                    UserLessonProgress progress = new UserLessonProgress();
                    progress.setUser(user);
                    progress.setLesson(nextLesson);
                    progress.setUnlocked(true);
                    progress.setCompleted(false);
                    progressRepo.save(progress);
                } else {
                    UserLessonProgress existingProgress = nextProgress.get();
                    if (!existingProgress.isUnlocked()) {
                        existingProgress.setUnlocked(true);
                        progressRepo.save(existingProgress);
                    }
                }
                break;
            }
        }
    }

    public int getCompletedLessonsCount(User user) {
        return progressRepo.countCompletedLessonsByUser(user);
    }
}

