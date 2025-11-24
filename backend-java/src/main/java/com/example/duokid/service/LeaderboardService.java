package com.example.duokid.service;

import com.example.duokid.model.User;
import com.example.duokid.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardService {

    private final UserRepository userRepo;

    public LeaderboardService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public List<User> getTop10ByXp() {
        return userRepo.findTop10ByOrderByXpDesc();
    }
}
