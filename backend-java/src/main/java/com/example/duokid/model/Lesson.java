package com.example.duokid.model;

import jakarta.persistence.*;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private int xpReward;
    private String level;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String contentHtml;

    // Lesson path structure (Duolingo-style)
    private Long parentId; // ID of parent lesson (for path structure)
    private Integer orderIndex; // Order in the path (0, 1, 2, ...)
    private String partName; // e.g., "PHẦN 1, CỬA 1"
    
    // Lesson type: "VOCABULARY" or "GRAMMAR"
    private String lessonType = "VOCABULARY";

    public Lesson() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }

    public String getLessonType() { return lessonType; }
    public void setLessonType(String lessonType) { this.lessonType = lessonType; }
}
