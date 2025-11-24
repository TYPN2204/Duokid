package com.example.duokid.service;

import com.example.duokid.repo.LessonRepository;
import com.example.duokid.repo.VocabularyRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service tự động import từ vựng từ thư mục Tieng-Anh khi ứng dụng khởi động
 * và tự động tạo bài học từ từ vựng đã import
 * Chỉ chạy nếu chưa có dữ liệu trong database
 */
@Service
public class VocabularyDataLoader {

    private final VocabularyImportService vocabularyImportService;
    private final VocabularyRepository vocabularyRepository;
    private final VocabularyToLessonService vocabularyToLessonService;
    private final LessonRepository lessonRepository;

    @Autowired
    public VocabularyDataLoader(VocabularyImportService vocabularyImportService,
                               VocabularyRepository vocabularyRepository,
                               VocabularyToLessonService vocabularyToLessonService,
                               LessonRepository lessonRepository) {
        this.vocabularyImportService = vocabularyImportService;
        this.vocabularyRepository = vocabularyRepository;
        this.vocabularyToLessonService = vocabularyToLessonService;
        this.lessonRepository = lessonRepository;
    }

    @PostConstruct
    public void loadVocabularyData() {
        try {
            // Kiểm tra xem đã có dữ liệu chưa
            long existingCount = vocabularyRepository.count();
            if (existingCount > 0) {
                System.out.println("ℹ️  Đã có " + existingCount + " từ vựng trong database, bỏ qua tự động import");
                
                // Kiểm tra xem đã có lesson từ vocabulary chưa
                long etsLessonCount = lessonRepository.findAll().stream()
                    .filter(l -> l.getTitle() != null && 
                            (l.getTitle().contains("ETS 2024") || 
                             l.getTitle().contains("LISTENING - TEST") || 
                             l.getTitle().contains("READING - TEST")))
                    .count();
                
                if (etsLessonCount == 0) {
                    System.out.println("🔄 Tự động tạo bài học từ từ vựng ETS...");
                    VocabularyToLessonService.CreateLessonsResult lessonResult = 
                        vocabularyToLessonService.createLessonsFromVocabulary();
                    if (lessonResult.getCreated() > 0) {
                        System.out.println("✅ Đã tự động tạo " + lessonResult.getCreated() + 
                            " bài học từ từ vựng ETS (bỏ qua " + lessonResult.getSkipped() + " bài đã tồn tại)");
                    }
                } else {
                    System.out.println("ℹ️  Đã có " + etsLessonCount + " bài học ETS, bỏ qua tạo bài học");
                }
                return;
            }

            // Import từ thư mục Tieng-Anh
            VocabularyImportService.ImportResult result = 
                vocabularyImportService.importAllFromDirectory("Tieng-Anh");
            
            // Log kết quả
            if (result.getImported() > 0) {
                System.out.println("✅ Đã tự động import " + result.getImported() + 
                    " từ vựng từ thư mục Tieng-Anh");
                if (result.getSkipped() > 0) {
                    System.out.println("   (Bỏ qua " + result.getSkipped() + " từ đã tồn tại)");
                }
                
                // Tự động tạo bài học từ từ vựng vừa import
                System.out.println("🔄 Tự động tạo bài học từ từ vựng ETS...");
                VocabularyToLessonService.CreateLessonsResult lessonResult = 
                    vocabularyToLessonService.createLessonsFromVocabulary();
                if (lessonResult.getCreated() > 0) {
                    System.out.println("✅ Đã tự động tạo " + lessonResult.getCreated() + 
                        " bài học từ từ vựng ETS (bỏ qua " + lessonResult.getSkipped() + " bài đã tồn tại)");
                } else if (!lessonResult.getErrors().isEmpty()) {
                    System.out.println("⚠️  Một số lỗi khi tạo bài học: " + 
                        String.join("; ", lessonResult.getErrors().subList(0, Math.min(3, lessonResult.getErrors().size()))));
                }
            } else {
                System.out.println("⚠️  Không tìm thấy file CSV trong thư mục Tieng-Anh");
            }
            
            if (!result.getErrors().isEmpty()) {
                System.out.println("⚠️  Một số lỗi khi import: " + 
                    String.join("; ", result.getErrors().subList(0, Math.min(5, result.getErrors().size()))));
            }
        } catch (Exception e) {
            // Không throw exception để không làm crash ứng dụng
            System.out.println("⚠️  Không thể tự động import từ vựng: " + e.getMessage());
            e.printStackTrace();
            // Có thể thư mục chưa tồn tại hoặc chưa có file CSV
        }
    }
}

