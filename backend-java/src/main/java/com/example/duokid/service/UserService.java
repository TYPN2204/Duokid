package com.example.duokid.service;

import com.example.duokid.model.ShopTransaction;
import com.example.duokid.model.User;
import com.example.duokid.repo.ShopTransactionRepository;
import com.example.duokid.repo.UserRepository;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final DailyGoalService dailyGoalService;
    private final ShopTransactionRepository shopTxRepo;

    private static final int MAX_HEARTS = 5;

    public UserService(UserRepository userRepo,
                       DailyGoalService dailyGoalService,
                       ShopTransactionRepository shopTxRepo) {
        this.userRepo = userRepo;
        this.dailyGoalService = dailyGoalService;
        this.shopTxRepo = shopTxRepo;
    }

    public User register(String email, String password, String name, String gradeLevel) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setDisplayName(name);

        String[] avatars = {"/avatar1.svg", "/avatar2.svg", "/avatar3.svg"};
        user.setAvatar(avatars[(int)(Math.random() * avatars.length)]);
        user.setStreak(0);
        user.setXp(0);
        user.setGems(500); // Starting gems like Duolingo
        user.setHearts(5);
        user.setLastStudyDate(null);
        user.setLastHeartRefillDate(LocalDate.now());
        user.setGradeLevel(gradeLevel != null ? gradeLevel : "GRADE1");

        return userRepo.save(user);
    }

    public User login(String email, String password) {
        Optional<User> opt = userRepo.findByEmail(email);
        if (opt.isEmpty()) return null;
        User user = opt.get();
        if (BCrypt.checkpw(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    public void addXpAndUpdateStreak(User user, int xpToAdd) {
        LocalDate today = LocalDate.now();
        LocalDate last = user.getLastStudyDate();

        if (last == null || last.isBefore(today.minusDays(1))) {
            user.setStreak(1);
        } else if (last.equals(today.minusDays(1))) {
            user.setStreak(user.getStreak() + 1);
        }

        user.setLastStudyDate(today);
        user.setXp(user.getXp() + xpToAdd);
        userRepo.save(user);

        dailyGoalService.checkStreakBadge(user);
    }

    public User addXp(User user, int xpToAdd) {
        if (xpToAdd <= 0) return user;
        user.setXp(user.getXp() + xpToAdd);
        return userRepo.save(user);
    }

    public User save(User u) {
        return userRepo.save(u);
    }

    public User checkDailyHeartRefill(User user) {
        LocalDate today = LocalDate.now();
        LocalDate last = user.getLastHeartRefillDate();

        if (last == null || last.isBefore(today)) {
            int currentHearts = user.getHearts();
            int missing = MAX_HEARTS - currentHearts;

            if (missing > 0) {
                int refill = Math.min(2, missing);
                int newHearts = currentHearts + refill;
                user.setHearts(newHearts);
                user.setLastHeartRefillDate(today);

                ShopTransaction tx = new ShopTransaction();
                tx.setUser(user);
                tx.setTime(LocalDateTime.now());
                tx.setHeartsChanged(refill);
                tx.setXpChanged(0);
                tx.setType("FREE_REFILL");
                tx.setNote("Refill tim miễn phí hằng ngày");
                shopTxRepo.save(tx);

                return userRepo.save(user);
            } else {
                user.setLastHeartRefillDate(today);
                return userRepo.save(user);
            }
        }
        return user;
    }
}
