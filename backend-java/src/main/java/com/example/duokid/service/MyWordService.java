package com.example.duokid.service;

import com.example.duokid.model.MyWord;
import com.example.duokid.model.User;
import com.example.duokid.repo.MyWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MyWordService {

    private final MyWordRepository myWordRepository;

    public MyWordService(MyWordRepository myWordRepository) {
        this.myWordRepository = myWordRepository;
    }

    public List<MyWord> getWords(User user) {
        return myWordRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public MyWord addWord(User user,
                          String englishWord,
                          String vietnameseMeaning,
                          String ipa,
                          String exampleSentence) {

        if (!StringUtils.hasText(englishWord) || !StringUtils.hasText(vietnameseMeaning)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ cả tiếng Anh và nghĩa tiếng Việt.");
        }
        if (myWordRepository.existsByUserAndEnglishWordIgnoreCase(user, englishWord.trim())) {
            throw new IllegalArgumentException("Từ này đã có trong sổ tay của bạn.");
        }

        MyWord word = new MyWord();
        word.setUser(user);
        word.setEnglishWord(englishWord.trim());
        word.setVietnameseMeaning(vietnameseMeaning.trim());
        word.setIpa(StringUtils.hasText(ipa) ? ipa.trim() : null);
        word.setExampleSentence(StringUtils.hasText(exampleSentence) ? exampleSentence.trim() : null);
        word.setCreatedAt(LocalDateTime.now());
        return myWordRepository.save(word);
    }

    public void deleteWord(User user, Long id) {
        myWordRepository.findById(id)
                .filter(word -> word.getUser().getId().equals(user.getId()))
                .ifPresent(myWordRepository::delete);
    }
}

