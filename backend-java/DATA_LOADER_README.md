# Data Loader Service - Hướng dẫn sử dụng

## Tổng quan

Service `DataLoaderService` được tạo để tự động trích xuất và tạo dữ liệu mẫu từ file `train_ai_teacher_1000.json` cho website.

## Chức năng

1. **Trích xuất từ vựng**: Tự động tìm và trích xuất các từ vựng từ các câu hỏi trong JSON
2. **Tạo bài học**: Tự động tạo bài học mới từ các từ vựng đã trích xuất
3. **Phân loại theo chủ đề**: Tự động phân loại từ vựng theo các chủ đề như Animals, Food, Colors, Family, School, etc.

## Cách hoạt động

1. Service sẽ tự động chạy khi ứng dụng khởi động (`@PostConstruct`)
2. Tìm file `train_ai_teacher_1000.json` trong:
   - Root directory của project (backend-java folder)
   - Hoặc trong `src/main/resources/`
3. Parse JSON và trích xuất:
   - Từ vựng từ các câu hỏi dạng: "từ 'word' trong chủ đề Topic"
   - Từ vựng từ các câu hỏi dạng: "từ 'word' nghĩa là gì"
4. Tạo bài học mới nếu:
   - Có ít nhất 3 từ vựng trong cùng một chủ đề
   - Chưa có bài học với chủ đề đó

## Cấu trúc dữ liệu được tạo

### Bài học (Lessons)
- **Title**: "{Topic} Vocabulary"
- **Description**: "Từ vựng về chủ đề {Topic}"
- **Level**: GRADE2
- **XP Reward**: 10
- **Content**: Danh sách từ vựng dạng HTML

### Từ vựng được trích xuất
Các từ vựng được tự động phân loại vào các chủ đề:
- **Animals**: cat, dog, bird, fish, duck, cow, horse, pig
- **Colors**: red, blue, green, yellow, orange, purple, pink, black, white
- **Food**: apple, banana, bread, milk, rice, noodles, chicken, egg
- **Family**: father, mother, brother, sister, grandfather, grandmother
- **Numbers**: one, two, three, four, five, six, seven, eight, nine, ten
- **School**: pencil, pen, book, school, teacher, student, desk, chair
- **Personal**: watch, phone, gift, suitcase, bag, key

## Lưu ý

1. Service chỉ chạy nếu database có ít hơn 10 bài học (để tránh duplicate)
2. File JSON phải có cấu trúc đúng format
3. Các từ vựng được trích xuất tự động, có thể cần chỉnh sửa nghĩa tiếng Việt sau

## Mở rộng

Có thể mở rộng service để:
- Trích xuất câu hỏi quiz từ các ví dụ multiple choice trong JSON
- Tạo thêm từ vựng cho game hình ảnh
- Tạo practice tasks từ các câu hỏi trong JSON

## Ví dụ output

```
✅ Đã load dữ liệu từ train_ai_teacher_1000.json thành công!
   - Từ vựng: 7 chủ đề
   - Câu hỏi quiz: 1 nhóm
```

