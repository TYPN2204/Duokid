# 🤖 Tích Hợp AI Model từ Google Colab

Hiện tại ChatBot sử dụng **Pattern Matching** (rule-based). Để nâng cao chất lượng, bạn có thể tích hợp AI model từ Google Colab hoặc Hugging Face.

## Phương án 1: Dùng Hugging Face Inference API (Dễ nhất ✅)

### Bước 1: Tạo tài khoản Hugging Face
- Truy cập: https://huggingface.co/join
- Đăng ký tài khoản miễn phí
- Tạo API token: https://huggingface.co/settings/tokens

### Bước 2: Cài đặt dependencies
```bash
cd /workspaces/Duokid/python-service
pip install requests
```

### Bước 3: Sửa `main.py` để dùng Hugging Face API

Thay thế hàm `chat()` bằng:

```python
import requests

HF_API_URL = "https://api-inference.huggingface.co/models/microsoft/DialoGPT-small"
HF_API_TOKEN = "YOUR_HUGGING_FACE_API_TOKEN"  # Lấy từ https://huggingface.co/settings/tokens

@app.post("/api/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    """
    AI Chat sử dụng Hugging Face DialoGPT
    """
    message = req.message.strip()
    
    headers = {"Authorization": f"Bearer {HF_API_TOKEN}"}
    
    try:
        response = requests.post(
            HF_API_URL,
            headers=headers,
            json={"inputs": message},
            timeout=10
        )
        
        if response.status_code == 200:
            result = response.json()
            # DialoGPT trả về dạng: [{"generated_text": "..."}]
            ai_reply = result[0]["generated_text"] if result else message
            return ChatResponse(reply=ai_reply)
        else:
            return ChatResponse(reply="Sorry, AI service is temporarily unavailable. Please try again.")
    
    except Exception as e:
        print(f"Hugging Face API Error: {e}")
        # Fallback to pattern matching
        return ChatResponse(reply="Let's practice vocabulary or check the examples!")
```

### Bước 4: Khởi động lại Python service
```bash
pkill -f "python.*main.py"
sleep 2
cd /workspaces/Duokid/python-service && python main.py &
```

---

## Phương án 2: Chạy Model từ Google Colab (Nâng cao)

### Bước 1: Tạo Colab Notebook
1. Truy cập: https://colab.research.google.com
2. Tạo notebook mới
3. Paste code dưới đây:

```python
# Chạy AI Model từ Colab
!pip install fastapi uvicorn torch transformers

from fastapi import FastAPI
from pydantic import BaseModel
import uvicorn
from transformers import AutoTokenizer, AutoModelForCausalLM
import torch

app = FastAPI()

# Load model (DialoGPT or GPT-2)
tokenizer = AutoTokenizer.from_pretrained("microsoft/DialoGPT-small")
model = AutoModelForCausalLM.from_pretrained("microsoft/DialoGPT-small")

class ChatRequest(BaseModel):
    message: str
    context: str = None

@app.post("/api/chat")
async def chat(req: ChatRequest):
    input_ids = tokenizer.encode(req.message + tokenizer.eos_token, return_tensors='pt')
    chat_history_ids = model.generate(input_ids, max_length=100, pad_token_id=tokenizer.eos_token_id)
    reply = tokenizer.decode(chat_history_ids[:, input_ids.shape[-1]:][0], skip_special_tokens=True)
    return {"reply": reply}

@app.get("/health")
async def health():
    return {"status": "ok"}

if __name__ == "__main__":
    # Sử dụng ngrok hoặc cloudflare tunnel để expose
    !pip install pyngrok
    from pyngrok import ngrok
    
    public_url = ngrok.connect(8000)
    print(f"🌐 Public URL: {public_url}")
    
    uvicorn.run(app, host="127.0.0.1", port=8000)
```

### Bước 2: Expose Colab Server
```python
# Tạo tunnel với Cloudflare (miễn phí, không cần API key)
!pip install cloudflare-tunnel
# Hoặc dùng ngrok với free tier
```

### Bước 3: Sửa Java Backend
Sửa `ChatBotController.java`:
```java
private final String PYTHON_SERVICE_URL = "YOUR_COLAB_PUBLIC_URL"; // https://xxx.ngrok.io
```

---

## Phương án 3: Dùng OpenAI API

### Bước 1: Lấy OpenAI API Key
- Tạo tài khoản: https://openai.com/api/
- Lấy API key: https://platform.openai.com/api-keys

### Bước 2: Cài đặt
```bash
pip install openai
```

### Bước 3: Sửa `main.py`

```python
import openai

openai.api_key = "YOUR_OPENAI_API_KEY"

@app.post("/api/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    try:
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": "You are a helpful English learning assistant for Vietnamese students."},
                {"role": "user", "content": req.message}
            ],
            temperature=0.7,
            max_tokens=100
        )
        return ChatResponse(reply=response.choices[0].message.content)
    except Exception as e:
        print(f"OpenAI Error: {e}")
        return ChatResponse(reply="Sorry, AI service is temporarily unavailable.")
```

---

## 🚀 Cách khởi động nhanh

```bash
# Dừng services cũ
pkill -f "java.*spring-boot"
pkill -f "python.*main.py"

# Khởi động lại
/workspaces/Duokid/start-services.sh
```

---

## 📊 So sánh các phương án

| Phương án | Chi phí | Tốc độ | Chất lượng | Dễ sử dụng |
|-----------|--------|-------|-----------|-----------|
| Pattern Matching | 🆓 | ⚡⚡⚡ | ⭐⭐ | ⭐⭐⭐ |
| Hugging Face | 🆓 | ⚡⚡ | ⭐⭐⭐ | ⭐⭐⭐ |
| Colab + ngrok | 🆓 | ⚡ | ⭐⭐⭐⭐ | ⭐⭐ |
| OpenAI API | 💰 | ⚡⚡ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

---

## 🔍 Kiểm tra kết nối

```bash
# Test Java API
curl -X POST http://localhost:8080/chatbot/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"hello"}'

# Test vocabulary
curl -X POST http://localhost:8080/chatbot/api/vocabulary \
  -H "Content-Type: application/json" \
  -d '{"word":"apple"}'
```

**Hãy chọn phương án phù hợp và cho tôi biết để tích hợp!** 🎉
