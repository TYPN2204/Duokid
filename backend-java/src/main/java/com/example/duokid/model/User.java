package com.example.duokid.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String password;
    private String displayName;
    private String avatar;

    private int streak;
    private int xp;
    private int gems; // Duolingo-style gems (in-game currency)
    private int hearts;

    private String gradeLevel;

    private LocalDate lastStudyDate;
    private LocalDate lastHeartRefillDate;

    private Boolean isAdmin = false; // Admin flag

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getHearts() { return hearts; }
    public void setHearts(int hearts) { this.hearts = hearts; }

    public int getGems() { return gems; }
    public void setGems(int gems) { this.gems = gems; }

    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }

    public LocalDate getLastStudyDate() { return lastStudyDate; }
    public void setLastStudyDate(LocalDate lastStudyDate) { this.lastStudyDate = lastStudyDate; }

    public LocalDate getLastHeartRefillDate() { return lastHeartRefillDate; }
    public void setLastHeartRefillDate(LocalDate lastHeartRefillDate) { this.lastHeartRefillDate = lastHeartRefillDate; }

    public Boolean getIsAdmin() { return isAdmin != null ? isAdmin : false; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }
}
