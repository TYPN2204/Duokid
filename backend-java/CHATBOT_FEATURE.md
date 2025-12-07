# ChatBot Feature - Hướng Dẫn Sử Dụng

## Tổng Quan

Tính năng ChatBot được tạo để thay thế phần "AI chấm điểm" trong practice và lessons. ChatBot cung cấp:

1. **Trợ lý học tập AI** - Trả lời câu hỏi về tiếng Anh
2. **Giải thích từ vựng** - Tìm kiếm, định nghĩa, phát âm từ
3. **Chat tương tác** - Giao diện chat thân thiện

## Các Thay Đổi Đã Thực Hiện

### 1. Frontend (HTML/CSS/JavaScript)

#### File Đã Xóa Nội Dung
- **`practice.html`**: Xóa phần "🤖 AI chấm điểm câu trả lời"
- **`lesson_detail.html`**: Xóa phần "AI gợi ý câu tiếng Anh" (giữ lại "Thêm từ vựng")

#### File Mới Tạo
- **`chatbot.html`**: Trang ChatBot với 2 panel:
  - **Panel 1 - Chat**: Khu vực chat với AI, tin nhắn nhanh, typing indicator
  - **Panel 2 - Vocabulary Lookup**: Tìm kiếm từ vựng, xem chi tiết, thêm vào sổ tay

#### Menu Cập Nhật
- Thêm link mới: `<a th:href="@{/chatbot}">🤖 ChatBot</a>`
- Đổi tên cũ: `💬 Chat` → `💬 Chat bạn` (để phân biệt)
- Cập nhật trong: `practice.html`, `lesson_detail.html`

### 2. Backend (Java)

#### File Mới Tạo
- **`ChatBotController.java`**: 
  - Route: `/chatbot`
  - Hiển thị trang `chatbot.html`
  - Kiểm tra user login

### 3. Python Service

#### Endpoint Mới Thêm

**1. Chat Endpoint**
```
POST /api/chat
Body: {
  "message": "string",
  "context": "learning" (optional)
}
Response: {
  "reply": "string"
}
```

**2. Vocabulary Lookup Endpoint**
```
POST /api/vocabulary
Body: {
  "word": "apple"
}
Response: {
  "word": "apple",
  "phonetic": "/ˈæp.əl/",
  "meaning": "Quả táo",
  "example": "I eat an apple every day."
}
```

#### Vocabulary Library
Có 10 từ cơ bản:
- apple, book, cat, dog, happy, hello, school, friend, family, water

## Cách Chạy

### 1. Backend Java
```bash
cd /workspaces/Duokid/backend-java
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
./mvnw spring-boot:run
```

Backend chạy trên: http://localhost:8080

### 2. Python Service (QUAN TRỌNG - phải chạy để ChatBot hoạt động)
```bash
cd /workspaces/Duokid/python-service
pip install -r requirements.txt
python main.py
```

Python service chạy trên: http://localhost:5000

## Cách Sử Dụng

### Từ Trang Dashboard/Lessons
1. Click vào menu **🤖 ChatBot**
2. Sẽ mở trang `/chatbot`

### Panel Chat với AI
1. Nhập câu hỏi vào field "Nhập câu hỏi của em..."
2. Nhấn "Gửi" hoặc Shift+Enter
3. AI sẽ trả lời

Hoặc sử dụng nút nhanh:
- 🔊 Phát âm
- 📖 Ví dụ
- ❓ Định nghĩa
- 📚 Hướng dẫn

### Panel Giải Thích Từ Vựng
1. Nhập từ tiếng Anh vào field "Nhập từ tiếng Anh..."
2. Nhấn "🔍 Tìm" hoặc Enter
3. Xem kết quả:
   - Phiên âm
   - Định nghĩa tiếng Việt
   - Ví dụ sử dụng
4. Nhấn "🔊 Nghe Phát Âm" để nghe
5. Nhấn "⭐ Thêm vào Sổ Tay" để lưu từ

## Giao Diện

### Màu Sắc Chủ Đề
- **Blue (#3B82F6)**: Chat user
- **Green (#10B981)**: Vocabulary lookup
- **Neutral**: Bot responses

### Responsive Design
- Desktop: 2 cột (Chat + Vocabulary)
- Mobile: 1 cột (xếp chồng)

## Tính Năng Nâng Cao (Có thể mở rộng)

1. **Lưu lịch chat** - Lưu vào database
2. **Thêm từ tự động** - Từ trong chat được nhận diện
3. **Voice input** - Nhập bằng giọng nói
4. **AI Model nâng cao** - Tích hợp LLM (GPT, Claude, Llama)
5. **Grammar checking** - Kiểm tra ngữ pháp câu nhập vào
6. **Learning path** - Gợi ý các bài học phù hợp

## Troubleshooting

### ChatBot không phản hồi
- Kiểm tra Python service có chạy: `http://localhost:5000/health`
- Nếu lỗi, bật lại Python service

### Âm thanh không phát
- Kiểm tra Python service có chạy
- Kiểm tra browser có cho phép audio không
- Kiểm tra TTS folder: `python-service/tts_audio/`

### Từ vựng không tìm được
- Từ chưa có trong library
- Hãy mở rộng `VOCABULARY_LIBRARY` trong `main.py`

## File Cấu Hình

### requirements.txt
```
fastapi
uvicorn
gTTS
```

### application.properties (Java)
Không cần thay đổi, tự động kết nối tới localhost:5000

## Tương Lai

Có thể tích hợp thêm:
- Lưu lịch chat vào database
- Ranking học sinh dựa trên tương tác ChatBot
- Integration với OpenAI API hoặc Hugging Face
- WebSocket cho real-time chat

---

**Ngày tạo:** 2025-12-07
**Phiên bản:** 1.0
**Trạng thái:** Active ✅
