# 📋 Danh Sách Chi Tiết Thay Đổi

## 1. FILE XÓA CONTENT

### practice.html
```html
❌ XÓA PHẦN NÀY:

<div class="panel">
    <h3>🤖 AI chấm điểm câu trả lời</h3>
    <p><strong>Đề bài:</strong> <span th:text="${practiceQuestion}">Question</span></p>
    <p><strong>Câu mẫu:</strong> <span th:text="${practiceExpected}">Expected</span></p>
    <form th:action="@{/practice/grade}" method="post" class="ai-form">
        <input type="hidden" name="lessonId" th:value="${lesson.id}">
        <input type="hidden" name="question" th:value="${practiceQuestion}">
        <input type="hidden" name="expected" th:value="${practiceExpected}">
        <textarea name="studentAnswer"
                  rows="3"
                  placeholder="Nhập câu tiếng Anh của em..."
                  th:text="${studentAnswer}"></textarea>
        <button type="submit" class="btn-main">Chấm điểm</button>
    </form>
    <p class="error" th:if="${gradeError}" th:text="${gradeError}"></p>
    <div class="panel" th:if="${gradeScore != null}">
        <p><strong>Điểm:</strong> <span th:text="${gradeScore}">80</span>/100</p>
        <p><strong>Feedback (EN):</strong> <span th:text="${gradeCommentEn}"></span></p>
        <p><strong>Nhận xét (VI):</strong> <span th:text="${gradeCommentVi}"></span></p>
        <p><strong>Câu mẫu:</strong> <span th:text="${practiceExpected}"></span></p>
    </div>
</div>
```

✅ GIỮ LẠI: Nút làm mini test và game

---

### lesson_detail.html
```html
❌ XÓA PHẦN NÀY:

<div class="panel ai-panel">
    <h3>AI gợi ý câu tiếng Anh</h3>
    <p>Nhập 1 câu tiếng Việt (liên quan đến chủ đề bài học), hệ thống sẽ gợi ý 1 câu tiếng Anh đơn giản.</p>
    <form th:action="@{'/lessons/' + ${lesson.id} + '/suggest'}"
          method="post"
          class="ai-form">
        <textarea name="vnText"
                  rows="2"
                  placeholder="Ví dụ: Em thích con mèo màu trắng."
                  th:text="${vnText}"> </textarea>
        <button type="submit" class="btn-main">Gợi ý câu tiếng Anh</button>
    </form>
    <p class="error" th:if="${aiError}" th:text="${aiError}"></p>
    <div th:if="${aiSuggestion}" class="ai-result-block">
        <p><strong>Câu gợi ý:</strong></p>
        <p class="ai-result">
            <span th:text="${aiSuggestion}"></span>
            <button class="tts-btn"
                    th:if="${aiAudioUrl}"
                    type="button"
                    th:data-audio-url="${aiAudioUrl}"
                    onclick="playAudioFromButton(this)">
                🔊 Nghe
            </button>
        </p>
    </div>
</div>
```

✅ GIỮ LẠI: Phần "Thêm từ vựng của em"

---

## 2. CẬP NHẬT MENU

### practice.html & lesson_detail.html

```html
❌ CŨ:
<a th:href="@{/mywords}">📝 Sổ tay</a>
<a th:href="@{/vocabulary}">📚 Từ vựng</a>
<a th:href="@{/practice}">🔄 Ôn tập</a>
<a th:href="@{/minigame}">🎮 Mini game</a>
<a th:href="@{/leaderboard}">🏆 Bảng xếp hạng</a>
<a th:href="@{/shop}">🛒 Cửa hàng</a>
<a th:href="@{/chat}">💬 Chat</a>
<a th:href="@{/profile}">👤 Hồ sơ</a>

✅ MỚI:
<a th:href="@{/mywords}">📝 Sổ tay</a>
<a th:href="@{/vocabulary}">📚 Từ vựng</a>
<a th:href="@{/practice}">🔄 Ôn tập</a>
<a th:href="@{/minigame}">🎮 Mini game</a>
<a th:href="@{/leaderboard}">🏆 Bảng xếp hạng</a>
<a th:href="@{/shop}">🛒 Cửa hàng</a>
<a th:href="@{/chatbot}">🤖 ChatBot</a>           <!-- MỚI -->
<a th:href="@{/chat}">💬 Chat bạn</a>           <!-- ĐỔI TÊN -->
<a th:href="@{/profile}">👤 Hồ sơ</a>
```

---

## 3. FILE TẠO MỚI

### chatbot.html
📍 `/workspaces/Duokid/backend-java/src/main/resources/templates/chatbot.html`

- **Responsive grid layout**: 2 cột trên desktop, 1 cột trên mobile
- **Panel 1 - Chat**:
  - Tin nhắn nhanh (Quick responses)
  - Khu vực hiển thị tin nhắn
  - Input area + Send button
  - Typing indicator animation
  
- **Panel 2 - Vocabulary**:
  - Tips box
  - Vocabulary lookup form
  - Vocabulary result display
  - Add to MyWords button

**Script chính**:
```javascript
async function sendMessage() {
  // Gửi message tới http://localhost:5000/api/chat
}

async function lookupVocabulary() {
  // Gửi word tới http://localhost:5000/api/vocabulary
}
```

---

### ChatBotController.java
📍 `/workspaces/Duokid/backend-java/src/main/java/com/example/duokid/controller/ChatBotController.java`

```java
@Controller
@RequestMapping("/chatbot")
public class ChatBotController {
    
    @GetMapping
    public String chatbotPage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";
        
        model.addAttribute("user", currentUser);
        model.addAttribute("isAdmin", Boolean.TRUE.equals(currentUser.getIsAdmin()));
        
        return "chatbot";
    }
}
```

---

## 4. PYTHON SERVICE - THÊM ENDPOINT

### main.py - Thêm Models
```python
class ChatRequest(BaseModel):
    message: str
    context: str | None = None

class ChatResponse(BaseModel):
    reply: str

class VocabularyRequest(BaseModel):
    word: str

class VocabularyResponse(BaseModel):
    word: str
    phonetic: str
    meaning: str
    example: str
```

### main.py - Endpoint mới

**1. POST /api/chat**
```python
@app.post("/api/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    # Pattern matching cho các câu hỏi thường gặp
    # Return ChatResponse với reply
```

**2. POST /api/vocabulary**
```python
@app.post("/api/vocabulary", response_model=VocabularyResponse)
def lookup_vocabulary(req: VocabularyRequest):
    # Tìm từ trong VOCABULARY_LIBRARY
    # Return VocabularyResponse
```

**3. Vocabulary Library** (10 từ cơ bản)
```python
VOCABULARY_LIBRARY = {
    "apple": {"phonetic": "/ˈæp.əl/", "meaning": "Quả táo", "example": "..."},
    "book": {"phonetic": "/bʊk/", "meaning": "Cuốn sách", "example": "..."},
    "cat": {"phonetic": "/kæt/", "meaning": "Con mèo", "example": "..."},
    # ... 7 từ khác
}
```

### main.py - Chạy Server
```python
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
```

---

## 5. TÀI LIỆU HƯỚNG DẪN

### CHATBOT_FEATURE.md
📍 `/workspaces/Duokid/backend-java/CHATBOT_FEATURE.md`

Tài liệu chi tiết gồm:
- Tổng quan tính năng
- Các thay đổi đã thực hiện
- Cách chạy hệ thống
- Cách sử dụng
- Giao diện chi tiết
- Troubleshooting
- Tính năng nâng cao trong tương lai

---

## 📊 TÓMT TẮT THỐNG KÊ

| Loại | Số lượng |
|------|---------|
| File xóa content | 2 |
| File tạo mới | 3 |
| File cập nhật | 3 |
| Endpoint mới (Python) | 2 |
| Model mới (Python) | 2 |
| Dòng code (chatbot.html) | ~550 |
| Dòng code (main.py thêm) | ~100 |

---

## 🔄 Dòng Thời Gian Thay Đổi

1. ✅ Xóa "AI chấm điểm" từ practice.html
2. ✅ Xóa "AI gợi ý" từ lesson_detail.html
3. ✅ Cập nhật menu 2 file
4. ✅ Tạo chatbot.html
5. ✅ Tạo ChatBotController.java
6. ✅ Thêm endpoint Python
7. ✅ Tài liệu hướng dẫn

---

## 🚀 CÓ THỂ CHẠY VÀ TEST NGAY

Tất cả code đã sẵn sàng:

```bash
# Terminal 1
cd /workspaces/Duokid/backend-java
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./mvnw spring-boot:run

# Terminal 2
cd /workspaces/Duokid/python-service
pip install -r requirements.txt
python main.py

# Trình duyệt
http://localhost:8080/chatbot
```

---

**Created:** 2025-12-07  
**Ready:** ✅ YES  
**Test Status:** Ready to test
