package com.example.duokid.repo;

import com.example.duokid.model.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    
    List<Vocabulary> findByTestType(String testType);
    
    List<Vocabulary> findByTestTypeAndTestNumber(String testType, Integer testNumber);
    
    List<Vocabulary> findByTestTypeAndPartNumber(String testType, String partNumber);
    
    @Query("SELECT DISTINCT v.testNumber FROM Vocabulary v WHERE v.testType = ?1 ORDER BY v.testNumber")
    List<Integer> findDistinctTestNumbersByTestType(String testType);
    
    @Query("SELECT DISTINCT v.partNumber FROM Vocabulary v WHERE v.testType = ?1 ORDER BY v.partNumber")
    List<String> findDistinctPartNumbersByTestType(String testType);
    
    boolean existsByEnglishWordIgnoreCaseAndTestTypeAndTestNumber(
        String englishWord, String testType, Integer testNumber
    );
}

