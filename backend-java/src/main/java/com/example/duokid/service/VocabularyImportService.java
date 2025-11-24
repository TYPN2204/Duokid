package com.example.duokid.service;

import com.example.duokid.model.Vocabulary;
import com.example.duokid.repo.VocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Service để import từ vựng từ file CSV vào database
 */
@Service
public class VocabularyImportService {

    private final VocabularyRepository vocabularyRepo;

    public VocabularyImportService(VocabularyRepository vocabularyRepo) {
        this.vocabularyRepo = vocabularyRepo;
    }

    /**
     * Import từ vựng từ file CSV
     * @param filePath Đường dẫn đến file CSV
     * @param testType "LISTENING" hoặc "READING"
     * @param testNumber Số test (1-100)
     * @return ImportResult chứa số lượng từ vựng đã import và lỗi (nếu có)
     */
    public ImportResult importFromCsv(String filePath, String testType, Integer testNumber) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return new ImportResult(0, 0, List.of("File not found: " + filePath));
            }

            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                boolean isFirstLine = true;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    // Bỏ qua dòng header
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    // Bỏ qua dòng trống
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        Vocabulary vocab = parseCsvLine(line, testType, testNumber);
                        if (vocab != null) {
                            // Kiểm tra xem đã tồn tại chưa
                            if (!vocabularyRepo.existsByEnglishWordIgnoreCaseAndTestTypeAndTestNumber(
                                    vocab.getEnglishWord(), testType, testNumber)) {
                                vocabularyRepo.save(vocab);
                                imported++;
                            } else {
                                skipped++;
                            }
                        }
                    } catch (Exception e) {
                        errors.add("Line " + lineNumber + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            errors.add("Error reading file: " + e.getMessage());
        }

        return new ImportResult(imported, skipped, errors);
    }

    /**
     * Import từ MultipartFile (upload từ web)
     */
    public ImportResult importFromMultipartFile(MultipartFile file, String testType, Integer testNumber) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                boolean isFirstLine = true;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        Vocabulary vocab = parseCsvLine(line, testType, testNumber);
                        if (vocab != null) {
                            if (!vocabularyRepo.existsByEnglishWordIgnoreCaseAndTestTypeAndTestNumber(
                                    vocab.getEnglishWord(), testType, testNumber)) {
                                vocabularyRepo.save(vocab);
                                imported++;
                            } else {
                                skipped++;
                            }
                        }
                    } catch (Exception e) {
                        errors.add("Line " + lineNumber + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            errors.add("Error reading file: " + e.getMessage());
        }

        return new ImportResult(imported, skipped, errors);
    }

    /**
     * Parse một dòng CSV thành Vocabulary object
     * Format CSV có thể khác nhau giữa LISTENING và READING
     */
    private Vocabulary parseCsvLine(String line, String testType, Integer testNumber) {
        // Parse CSV với xử lý dấu phẩy trong quotes
        List<String> fields = parseCsvFields(line);
        
        if (fields.size() < 3) {
            return null; // Không đủ dữ liệu
        }

        Vocabulary vocab = new Vocabulary();
        vocab.setTestType(testType);
        vocab.setTestNumber(testNumber);

        if ("LISTENING".equals(testType)) {
            // Format LISTENING: Phân loại, Cột 2, Từ tiếng Anh, Luyện từ, Từ loại, Kiểm tra, 
            // Anh - Mỹ, Anh - Anh, Nghĩa tiếng Việt, ...
            if (fields.size() >= 9) {
                vocab.setPartNumber(cleanField(fields.get(0))); // Phân loại (Part 1, Part 2...)
                vocab.setEnglishWord(cleanField(fields.get(2))); // Từ tiếng Anh
                vocab.setWordType(cleanField(fields.get(4))); // Từ loại
                vocab.setIpaAmerican(cleanField(fields.get(6))); // Anh - Mỹ
                vocab.setIpaBritish(cleanField(fields.get(7))); // Anh - Anh
                vocab.setVietnameseMeaning(cleanField(fields.get(8))); // Nghĩa tiếng Việt
                
                // Từ đồng nghĩa (cột 11)
                if (fields.size() > 11) {
                    vocab.setSynonyms(cleanField(fields.get(11)));
                }
                
                // Từ trái nghĩa (cột 14)
                if (fields.size() > 14) {
                    vocab.setAntonyms(cleanField(fields.get(14)));
                }
                
                // Câu ví dụ (cột 17)
                if (fields.size() > 17) {
                    vocab.setExampleSentence(cleanField(fields.get(17)));
                }
            }
        } else if ("READING".equals(testType)) {
            // Format READING có thể khác một chút
            // Cột 1, Từ tiếng Anh, Luyện từ tiếng anh, Loại từ, Phiên âm, Check 0, 
            // Nghĩa tiếng Việt, ...
            if (fields.size() >= 7) {
                vocab.setEnglishWord(cleanField(fields.get(1))); // Từ tiếng Anh
                vocab.setWordType(cleanField(fields.get(3))); // Loại từ
                
                // Phiên âm (có thể chỉ có 1, hoặc có cả 2)
                if (fields.size() > 4) {
                    String ipa = cleanField(fields.get(4));
                    // Nếu có dấu / thì là phiên âm
                    if (ipa.contains("/")) {
                        vocab.setIpaAmerican(ipa);
                        vocab.setIpaBritish(ipa); // Tạm thời dùng chung
                    }
                }
                
                vocab.setVietnameseMeaning(cleanField(fields.get(6))); // Nghĩa tiếng Việt
                
                // Từ đồng nghĩa (cột 10)
                if (fields.size() > 10) {
                    vocab.setSynonyms(cleanField(fields.get(10)));
                }
                
                // Từ trái nghĩa (cột 12)
                if (fields.size() > 12) {
                    vocab.setAntonyms(cleanField(fields.get(12)));
                }
                
                // Câu ví dụ (cột 15)
                if (fields.size() > 15) {
                    vocab.setExampleSentence(cleanField(fields.get(15)));
                }
            }
        }

        // Validate
        if (vocab.getEnglishWord() == null || vocab.getEnglishWord().trim().isEmpty()) {
            return null;
        }
        if (vocab.getVietnameseMeaning() == null || vocab.getVietnameseMeaning().trim().isEmpty()) {
            return null;
        }

        return vocab;
    }

    /**
     * Parse CSV line với xử lý quotes và commas
     */
    private List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // Thêm field cuối cùng
        fields.add(currentField.toString());
        
        return fields;
    }

    /**
     * Clean field: remove quotes, trim whitespace
     */
    private String cleanField(String field) {
        if (field == null) {
            return null;
        }
        field = field.trim();
        // Remove surrounding quotes
        if (field.startsWith("\"") && field.endsWith("\"")) {
            field = field.substring(1, field.length() - 1);
        }
        // Replace double quotes with single
        field = field.replace("\"\"", "\"");
        return field.isEmpty() ? null : field;
    }

    /**
     * Import tất cả file CSV từ thư mục Tieng-Anh
     */
    public ImportResult importAllFromDirectory(String baseDirectory) {
        List<String> errors = new ArrayList<>();
        int totalImported = 0;
        int totalSkipped = 0;

        try {
            Path basePath = Paths.get(baseDirectory);
            
            // Import LISTENING files (hỗ trợ test 1-100)
            Path listeningPath = basePath.resolve("ETS 2024 – LISTENING");
            if (Files.exists(listeningPath) && Files.isDirectory(listeningPath)) {
                for (int i = 1; i <= 100; i++) {
                    Path testFile = listeningPath.resolve("TEST " + i + ".csv");
                    if (Files.exists(testFile)) {
                        ImportResult result = importFromCsv(testFile.toString(), "LISTENING", i);
                        totalImported += result.getImported();
                        totalSkipped += result.getSkipped();
                        errors.addAll(result.getErrors());
                    }
                }
            }

            // Import READING files (hỗ trợ test 1-100)
            Path readingPath = basePath.resolve("ETS 2024 READING");
            if (Files.exists(readingPath) && Files.isDirectory(readingPath)) {
                for (int i = 1; i <= 100; i++) {
                    Path testFile = readingPath.resolve("TEST " + i + ".csv");
                    if (Files.exists(testFile)) {
                        ImportResult result = importFromCsv(testFile.toString(), "READING", i);
                        totalImported += result.getImported();
                        totalSkipped += result.getSkipped();
                        errors.addAll(result.getErrors());
                    }
                }
            }

        } catch (Exception e) {
            errors.add("Error importing from directory: " + e.getMessage());
        }

        return new ImportResult(totalImported, totalSkipped, errors);
    }

    /**
     * Result class cho import operation
     */
    public static class ImportResult {
        private final int imported;
        private final int skipped;
        private final List<String> errors;

        public ImportResult(int imported, int skipped, List<String> errors) {
            this.imported = imported;
            this.skipped = skipped;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public int getImported() {
            return imported;
        }

        public int getSkipped() {
            return skipped;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean isSuccess() {
            return errors.isEmpty();
        }
    }
}

