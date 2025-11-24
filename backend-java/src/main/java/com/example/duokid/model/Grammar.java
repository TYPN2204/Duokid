package com.example.duokid.model;

import jakarta.persistence.*;

@Entity
@Table(name = "grammar")
public class Grammar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private int xpReward;
    private String level; // GRADE3, GRADE4, GRADE5

    @Column(length = 4000)
    private String contentHtml; // Grammar explanation and examples

    // Grammar path structure
    private Long parentId;
    private Integer orderIndex;
    private String partName;

    // Grammar topic (e.g., "Present Simple", "Past Tense", "Prepositions")
    private String topic;

    // Example sentences
    @Column(length = 2000)
    private String examples; // JSON or comma-separated examples

    public Grammar() {}

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

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getExamples() { return examples; }
    public void setExamples(String examples) { this.examples = examples; }
}

