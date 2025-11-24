package com.example.duokid.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity để lưu từ vựng từ ETS 2024 (LISTENING và READING)
 * Khác với MyWord - đây là từ vựng chung cho tất cả user
 */
@Entity
@Table(name = "vocabularies")
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String englishWord;

    @Column(nullable = false, length = 500)
    private String vietnameseMeaning;

    private String wordType; // verb, noun, phrasal verb, phrase, adjective, adverb, etc.

    private String ipaAmerican; // Phiên âm Anh-Mỹ
    private String ipaBritish; // Phiên âm Anh-Anh

    @Column(length = 1000)
    private String synonyms; // Từ đồng nghĩa (có thể nhiều từ, cách nhau bởi dấu phẩy)

    @Column(length = 1000)
    private String antonyms; // Từ trái nghĩa

    @Column(length = 1000)
    private String exampleSentence; // Câu ví dụ

    // Thông tin về test
    @Column(nullable = false)
    private String testType; // "LISTENING" hoặc "READING"

    private String partNumber; // "Part 1", "Part 2", "Part 3", "Part 4"

    private Integer testNumber; // 1, 2, 3, ..., 10

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Vocabulary() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEnglishWord() {
        return englishWord;
    }

    public void setEnglishWord(String englishWord) {
        this.englishWord = englishWord;
    }

    public String getVietnameseMeaning() {
        return vietnameseMeaning;
    }

    public void setVietnameseMeaning(String vietnameseMeaning) {
        this.vietnameseMeaning = vietnameseMeaning;
    }

    public String getWordType() {
        return wordType;
    }

    public void setWordType(String wordType) {
        this.wordType = wordType;
    }

    public String getIpaAmerican() {
        return ipaAmerican;
    }

    public void setIpaAmerican(String ipaAmerican) {
        this.ipaAmerican = ipaAmerican;
    }

    public String getIpaBritish() {
        return ipaBritish;
    }

    public void setIpaBritish(String ipaBritish) {
        this.ipaBritish = ipaBritish;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public String getAntonyms() {
        return antonyms;
    }

    public void setAntonyms(String antonyms) {
        this.antonyms = antonyms;
    }

    public String getExampleSentence() {
        return exampleSentence;
    }

    public void setExampleSentence(String exampleSentence) {
        this.exampleSentence = exampleSentence;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public Integer getTestNumber() {
        return testNumber;
    }

    public void setTestNumber(Integer testNumber) {
        this.testNumber = testNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

