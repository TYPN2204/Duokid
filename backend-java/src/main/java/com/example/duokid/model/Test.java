package com.example.duokid.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tests")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String level; // GRADE1, GRADE2, etc.
    
    // Test appears after completing N lessons
    private Integer afterLessons; // e.g., 5 means test appears after 5 lessons
    
    // Minimum score to pass (0-100)
    private Integer passingScore = 70;
    
    // Hearts lost if fail
    private Integer heartsLostOnFail = 1;
    
    // XP reward if pass
    private Integer xpReward = 20;
    
    // Gems reward if pass
    private Integer gemsReward = 10;

    @Column(length = 2000)
    private String instructions; // Instructions for the test

    public Test() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Integer getAfterLessons() { return afterLessons; }
    public void setAfterLessons(Integer afterLessons) { this.afterLessons = afterLessons; }

    public Integer getPassingScore() { return passingScore; }
    public void setPassingScore(Integer passingScore) { this.passingScore = passingScore; }

    public Integer getHeartsLostOnFail() { return heartsLostOnFail; }
    public void setHeartsLostOnFail(Integer heartsLostOnFail) { this.heartsLostOnFail = heartsLostOnFail; }

    public Integer getXpReward() { return xpReward; }
    public void setXpReward(Integer xpReward) { this.xpReward = xpReward; }

    public Integer getGemsReward() { return gemsReward; }
    public void setGemsReward(Integer gemsReward) { this.gemsReward = gemsReward; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}

