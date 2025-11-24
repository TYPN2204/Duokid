English Mochi-style Spring Boot App (code template)

Cách dùng với project hiện tại của bạn:

1. Trong VS Code, bạn đã có project english_app được tạo bằng Spring Initializr.
2. Tải file english_app_code.zip từ ChatGPT, giải nén.
3. Copy toàn bộ thư mục:
   - src/main/java/com/example/english_app
   - src/main/resources/templates
   - src/main/resources/static
   - src/main/resources/application.properties
   vào đúng vị trí tương ứng trong project english_app trên máy bạn (ghi đè nếu được hỏi).
4. Chạy lại:
   mvnw.cmd spring-boot:run
5. Mở trình duyệt:
   - http://localhost:8080/           (trang landing giống Mochi)
   - http://localhost:8080/lessons    (danh sách bài học)
   - http://localhost:8080/lessons/1  (bài học example)
   - http://localhost:8080/dashboard  (dashboard)
   - http://localhost:8080/login      (form login)
   - http://localhost:8080/register   (form đăng ký)

Tích hợp Python LLM:

- File Java: src/main/java/com/example/english_app/api/ChatApiController.java
- Thuộc tính cấu hình: llm.server.url trong application.properties

Bạn cần tự chạy một Python server (Flask/FastAPI) ở port 5000 với endpoint /api/chat
trả về JSON dạng:
{
  "reply": "Câu trả lời của model..."
}

Sau đó ở trang bài học, phần "Chat with AI tutor" sẽ gọi sang Python.
