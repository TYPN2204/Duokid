package com.example.duokid.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vocab_game_scores")
public class VocabGameScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private int totalPoints;
    private int bestRoundScore;
    private LocalDateTime lastPlayed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getBestRoundScore() {
        return bestRoundScore;
    }

    public void setBestRoundScore(int bestRoundScore) {
        this.bestRoundScore = bestRoundScore;
    }

    public LocalDateTime getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(LocalDateTime lastPlayed) {
        this.lastPlayed = lastPlayed;
    }
}

