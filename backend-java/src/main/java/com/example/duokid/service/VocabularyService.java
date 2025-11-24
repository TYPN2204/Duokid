package com.example.duokid.service;

import com.example.duokid.model.Vocabulary;
import com.example.duokid.repo.VocabularyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service để xử lý business logic cho Vocabulary
 */
@Service
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;

    public VocabularyService(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    /**
     * Lấy tất cả từ vựng
     */
    public List<Vocabulary> getAllVocabularies() {
        return vocabularyRepository.findAll();
    }

    /**
     * Lấy từ vựng theo ID
     */
    public Optional<Vocabulary> getVocabularyById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return vocabularyRepository.findById(id);
    }

    /**
     * Lấy từ vựng theo loại test
     */
    public List<Vocabulary> getVocabulariesByTestType(String testType) {
        return vocabularyRepository.findByTestType(testType);
    }

    /**
     * Lấy từ vựng theo loại test và số test
     */
    public List<Vocabulary> getVocabulariesByTestTypeAndNumber(String testType, Integer testNumber) {
        return vocabularyRepository.findByTestTypeAndTestNumber(testType, testNumber);
    }

    /**
     * Lấy từ vựng theo loại test và phần
     */
    public List<Vocabulary> getVocabulariesByTestTypeAndPart(String testType, String partNumber) {
        return vocabularyRepository.findByTestTypeAndPartNumber(testType, partNumber);
    }

    /**
     * Tìm kiếm từ vựng
     */
    public List<Vocabulary> searchVocabularies(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllVocabularies();
        }

        String searchLower = keyword.toLowerCase().trim();
        return vocabularyRepository.findAll().stream()
                .filter(v -> 
                    (v.getEnglishWord() != null && v.getEnglishWord().toLowerCase().contains(searchLower)) ||
                    (v.getVietnameseMeaning() != null && v.getVietnameseMeaning().toLowerCase().contains(searchLower)) ||
                    (v.getExampleSentence() != null && v.getExampleSentence().toLowerCase().contains(searchLower)) ||
                    (v.getSynonyms() != null && v.getSynonyms().toLowerCase().contains(searchLower))
                )
                .toList();
    }

    /**
     * Lưu từ vựng
     */
    public Vocabulary saveVocabulary(Vocabulary vocabulary) {
        if (vocabulary == null) {
            throw new IllegalArgumentException("Vocabulary cannot be null");
        }
        return vocabularyRepository.save(vocabulary);
    }

    /**
     * Xóa từ vựng
     */
    public void deleteVocabulary(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Vocabulary ID cannot be null");
        }
        vocabularyRepository.deleteById(id);
    }

    /**
     * Đếm tổng số từ vựng
     */
    public long countAll() {
        return vocabularyRepository.count();
    }

    /**
     * Đếm số từ vựng theo loại test
     */
    public long countByTestType(String testType) {
        return vocabularyRepository.findByTestType(testType).size();
    }

    /**
     * Lấy danh sách số test theo loại
     */
    public List<Integer> getTestNumbersByType(String testType) {
        return vocabularyRepository.findDistinctTestNumbersByTestType(testType);
    }

    /**
     * Lấy danh sách phần theo loại test
     */
    public List<String> getPartNumbersByType(String testType) {
        return vocabularyRepository.findDistinctPartNumbersByTestType(testType);
    }
}

