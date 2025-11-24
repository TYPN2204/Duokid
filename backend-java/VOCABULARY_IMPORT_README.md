# Hướng dẫn Import Từ vựng ETS 2024

## Tổng quan

Hệ thống đã được tích hợp chức năng import từ vựng từ các file CSV trong thư mục `Tieng-Anh` vào database. Dữ liệu bao gồm:

- **ETS 2024 LISTENING**: 10 file CSV (TEST 1 đến TEST 10)
- **ETS 2024 READING**: 10 file CSV (TEST 1 đến TEST 10)

## Cấu trúc Database

### Entity: Vocabulary

Bảng `vocabularies` lưu trữ các thông tin sau:

- `id`: ID tự động
- `englishWord`: Từ tiếng Anh
- `vietnameseMeaning`: Nghĩa tiếng Việt
- `wordType`: Loại từ (verb, noun, phrasal verb, phrase, adjective, etc.)
- `ipaAmerican`: Phiên âm Anh-Mỹ
- `ipaBritish`: Phiên âm Anh-Anh
- `synonyms`: Từ đồng nghĩa
- `antonyms`: Từ trái nghĩa
- `exampleSentence`: Câu ví dụ
- `testType`: Loại test ("LISTENING" hoặc "READING")
- `partNumber`: Số phần ("Part 1", "Part 2", "Part 3", "Part 4")
- `testNumber`: Số test (1-10)
- `createdAt`: Thời gian tạo

## Cách sử dụng

### 1. Tự động import khi khởi động ứng dụng

Service `VocabularyDataLoader` sẽ tự động import từ vựng khi ứng dụng khởi động **nếu database chưa có dữ liệu**.

- Đảm bảo thư mục `Tieng-Anh` nằm trong thư mục root của project (cùng cấp với `pom.xml`)
- Cấu trúc thư mục:
  ```
  backend-java/
    ├── Tieng-Anh/
    │   ├── ETS 2024 – LISTENING/
    │   │   ├── TEST 1.csv
    │   │   ├── TEST 2.csv
    │   │   └── ...
    │   └── ETS 2024 READING/
    │       ├── TEST 1.csv
    │       ├── TEST 2.csv
    │       └── ...
  ```

### 2. Import qua Web Interface

Truy cập `/admin/import` và sử dụng form import từ vựng:

- **Import tất cả**: Tự động import tất cả file CSV từ thư mục `Tieng-Anh`
- **Import từ file**: Upload file CSV và chọn loại test (LISTENING/READING) và số test (1-10)

### 3. Import qua API

#### Import tất cả từ thư mục

```java
@Autowired
private VocabularyImportService vocabularyImportService;

VocabularyImportService.ImportResult result = 
    vocabularyImportService.importAllFromDirectory("Tieng-Anh");

System.out.println("Imported: " + result.getImported());
System.out.println("Skipped: " + result.getSkipped());
```

#### Import từ file CSV cụ thể

```java
// Từ file path
ImportResult result = vocabularyImportService.importFromCsv(
    "Tieng-Anh/ETS 2024 – LISTENING/TEST 1.csv", 
    "LISTENING", 
    1
);

// Từ MultipartFile (upload)
ImportResult result = vocabularyImportService.importFromMultipartFile(
    multipartFile, 
    "READING", 
    2
);
```

## Truy vấn dữ liệu

### Sử dụng Repository

```java
@Autowired
private VocabularyRepository vocabularyRepository;

// Lấy tất cả từ vựng LISTENING
List<Vocabulary> listeningVocabs = vocabularyRepository.findByTestType("LISTENING");

// Lấy từ vựng của TEST 1 LISTENING
List<Vocabulary> test1Vocabs = vocabularyRepository.findByTestTypeAndTestNumber("LISTENING", 1);

// Lấy từ vựng Part 1 LISTENING
List<Vocabulary> part1Vocabs = vocabularyRepository.findByTestTypeAndPartNumber("LISTENING", "Part 1");

// Lấy danh sách số test
List<Integer> testNumbers = vocabularyRepository.findDistinctTestNumbersByTestType("LISTENING");

// Lấy danh sách phần
List<String> parts = vocabularyRepository.findDistinctPartNumbersByTestType("LISTENING");
```

## Format CSV

### LISTENING Format

Các cột chính:
- Cột 0: Phân loại (Part 1, Part 2, Part 3, Part 4)
- Cột 2: Từ tiếng Anh
- Cột 4: Từ loại
- Cột 6: Phiên âm Anh-Mỹ
- Cột 7: Phiên âm Anh-Anh
- Cột 8: Nghĩa tiếng Việt
- Cột 11: Từ đồng nghĩa
- Cột 14: Từ trái nghĩa
- Cột 17: Câu ví dụ

### READING Format

Các cột chính:
- Cột 1: Từ tiếng Anh
- Cột 3: Loại từ
- Cột 4: Phiên âm
- Cột 6: Nghĩa tiếng Việt
- Cột 10: Từ đồng nghĩa
- Cột 12: Từ trái nghĩa
- Cột 15: Câu ví dụ

## Lưu ý

1. **Trùng lặp**: Hệ thống tự động kiểm tra và bỏ qua các từ vựng đã tồn tại (dựa trên `englishWord`, `testType`, và `testNumber`)

2. **Encoding**: File CSV phải sử dụng UTF-8 encoding

3. **Quotes trong CSV**: Hệ thống tự động xử lý dấu ngoặc kép trong CSV

4. **Lỗi import**: Các lỗi sẽ được ghi lại trong `ImportResult.errors` nhưng không làm dừng quá trình import

## Troubleshooting

### Không import được

1. Kiểm tra đường dẫn thư mục `Tieng-Anh` có đúng không
2. Kiểm tra file CSV có đúng format không
3. Kiểm tra encoding file CSV (phải là UTF-8)
4. Xem log console để biết lỗi cụ thể

### Import nhưng thiếu dữ liệu

1. Kiểm tra các dòng trong CSV có đủ cột không
2. Kiểm tra các dòng trống hoặc dòng header có bị bỏ qua không
3. Xem `ImportResult.errors` để biết các dòng bị lỗi

## Ví dụ sử dụng

### Tạo Controller để hiển thị từ vựng

```java
@Controller
public class VocabularyController {
    
    @Autowired
    private VocabularyRepository vocabularyRepository;
    
    @GetMapping("/vocabulary")
    public String showVocabulary(
            @RequestParam(required = false) String testType,
            @RequestParam(required = false) Integer testNumber,
            Model model) {
        
        List<Vocabulary> vocabularies;
        if (testType != null && testNumber != null) {
            vocabularies = vocabularyRepository.findByTestTypeAndTestNumber(testType, testNumber);
        } else if (testType != null) {
            vocabularies = vocabularyRepository.findByTestType(testType);
        } else {
            vocabularies = vocabularyRepository.findAll();
        }
        
        model.addAttribute("vocabularies", vocabularies);
        return "vocabulary_list";
    }
}
```

## Kết luận

Hệ thống import từ vựng đã được tích hợp hoàn chỉnh và sẵn sàng sử dụng. Dữ liệu sẽ được tự động import khi ứng dụng khởi động lần đầu, hoặc có thể import thủ công qua web interface hoặc API.

