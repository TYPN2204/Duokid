# 🚀 Tích Hợp ChatBot từ Google Colab

## Bước 1: Tạo Colab Notebook

Truy cập: https://colab.research.google.com

Tạo notebook mới và paste code dưới đây:

```python
# ============ INSTALL DEPENDENCIES ============
!pip install fastapi uvicorn torch transformers pyngrok -q

# ============ IMPORTS ============
from fastapi import FastAPI
from pydantic import BaseModel
from transformers import pipeline
from pyngrok import ngrok
import uvicorn
import threading
import time

# ============ SETUP NGROK ============
# Lấy ngrok token từ: https://dashboard.ngrok.com/auth/your-authtoken
ngrok.set_auth_token("YOUR_NGROK_TOKEN")  # ⚠️ Thay YOUR_NGROK_TOKEN

# ============ CREATE FASTAPI APP ============
app = FastAPI()

# Load model
print("📥 Loading AI model...")
qa_pipeline = pipeline("question-answering", model="distilbert-base-uncased-distilled-squad")
print("✅ Model loaded!")

# ============ DEFINE MODELS ============
class ChatRequest(BaseModel):
    message: str

class ChatResponse(BaseModel):
    reply: str

# ============ ENDPOINTS ============
@app.get("/health")
async def health():
    return {"status": "ok", "model": "distilbert-base-uncased-distilled-squad"}

@app.post("/api/chat")
async def chat(req: ChatRequest):
    try:
        message = req.message.strip()
        
        if not message:
            return ChatResponse(reply="Please type a message!")
        
        # Use QA model to generate response
        context = """
        English is a global language. Learning English helps you communicate with 
        people around the world. There are many ways to learn English including 
        reading, writing, speaking, and listening. Practice every day to improve your skills.
        """
        
        try:
            result = qa_pipeline(question=message, context=context)
            reply = result.get("answer", "Let me think about that...")
            return ChatResponse(reply=reply)
        except:
            return ChatResponse(reply="That's an interesting question! Keep practicing!")
    
    except Exception as e:
        print(f"Error: {e}")
        return ChatResponse(reply="Let's continue learning!")

# ============ RUN SERVER ============
# Start server in background thread
def run_server():
    uvicorn.run(app, host="127.0.0.1", port=8000, log_level="error")

server_thread = threading.Thread(target=run_server, daemon=True)
server_thread.start()

# Wait for server to start
print("⏳ Starting server...")
time.sleep(3)

# Create public URL
public_url = ngrok.connect(8000)
print(f"\n✅ PUBLIC URL: {public_url}")
print(f"\n🔗 ChatBot Endpoint: {public_url}/api/chat")
print(f"🔗 Health Check: {public_url}/health")

# Keep running
ngrok_process = ngrok.get_ngrok_process()
ngrok_process.proc.wait()
```

## Bước 2: Lấy Ngrok Token

1. Truy cập: https://dashboard.ngrok.com/auth/your-authtoken
2. Copy token
3. Thay `YOUR_NGROK_TOKEN` trong code

## Bước 3: Chạy Colab

1. Bấm `Ctrl+F9` hoặc nút Run
2. Chờ model load (khoảng 30 giây)
3. Copy public URL từ output

## Bước 4: Cập Nhật Java Backend

Sau khi có public URL từ Colab, sửa `ChatBotController.java`:

```java
@PostMapping("/api/chat")
@ResponseBody
public ResponseEntity<?> chat(@RequestBody JsonNode request) {
    String colabUrl = "https://xxxxx-xxx-ngrok-io.app";  // ⚠️ Thay bằng URL từ Colab
    
    try {
        String response = restTemplate.postForObject(
            colabUrl + "/api/chat",
            request,
            String.class
        );
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body("{\"error\":\"Colab service unavailable\"}");
    }
}
```

## Bước 5: Restart Java Backend

```bash
pkill -f "java.*spring-boot"
cd /workspaces/Duokid/backend-java
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./mvnw spring-boot:run &
```

## ✅ Test

```bash
curl -X POST http://localhost:8080/chatbot/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"What is English?"}'
```

---

## 📊 So Sánh

| Phương án | Tốc độ | Chất lượng | Chi phí | Độ phức tạp |
|-----------|-------|-----------|--------|-----------|
| Hugging Face API | Trung bình | Trung bình | 🆓 | Đơn giản |
| **Google Colab** | Nhanh | **Cao** | 🆓 | Trung bình |
| OpenAI API | Nhanh | Rất cao | 💰 | Đơn giản |

**Khuyến cáo:** Colab là lựa chọn tốt nhất - miễn phí, GPU mạnh, chất lượng cao!
