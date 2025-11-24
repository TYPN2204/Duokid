package com.example.duokid.repo;

import com.example.duokid.model.MyWord;
import com.example.duokid.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MyWordRepository extends JpaRepository<MyWord, Long> {
    List<MyWord> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndEnglishWordIgnoreCase(User user, String englishWord);
}

