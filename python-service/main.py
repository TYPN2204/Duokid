from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from pydantic import BaseModel
from gtts import gTTS
import uuid
import os
import random
import re
import requests

# Hugging Face API Configuration - Load from environment variable
HF_API_TOKEN = os.getenv("HF_API_TOKEN", "")
# Using the new serverless inference API endpoint
HF_API_URL = "https://router.huggingface.co/models/distilgpt2"

app = FastAPI()

# Cấu hình CORS để cho phép browser phát audio
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Trong production nên chỉ định domain cụ thể
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

TTS_FOLDER = "tts_audio"
os.makedirs(TTS_FOLDER, exist_ok=True)

print("✅ Hugging Face API configured successfully!")
print(f"🤖 Using model: distilgpt2 (lightweight text generation)")
print(f"🔌 API Endpoint: https://api-inference.huggingface.co")




class SuggestRequest(BaseModel):
    vietnamese: str
    topic: str | None = None
    level: str | None = None


class SuggestResponse(BaseModel):
    sentence: str


class TTSRequest(BaseModel):
    text: str


class GradeRequest(BaseModel):
    question: str
    expected: str
    answer: str


class GradeResponse(BaseModel):
    score: int
    commentEn: str
    commentVi: str


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


TOPIC_LIBRARY = {
    "greetings": [
        "Hello, nice to meet you.",
        "Good morning, class!",
        "Hi, I am happy to see you."
    ],
    "colors": [
        "The flower is yellow.",
        "My backpack is blue.",
        "The sun is bright orange."
    ],
    "numbers": [
        "There are six apples on the table.",
        "I can count from one to ten.",
        "She has nine balloons."
    ],
    "animals": [
        "The brown dog runs fast.",
        "A little cat sleeps on the chair.",
        "The duck swims in the pond."
    ],
    "family": [
        "This is my little sister.",
        "I love my father and mother.",
        "My grandparents tell fun stories."
    ]
}

LEVEL_LIBRARY = {
    "grade1": [
        "I have a red pen.",
        "This is my friend Lan.",
        "We sing a song at school."
    ],
    "grade2": [
        "My brother likes to ride his bike.",
        "We eat lunch together at noon.",
        "I can describe five animals."
    ]
}


@app.post("/suggest", response_model=SuggestResponse)
def suggest(req: SuggestRequest):
    vi = (req.vietnamese or "").lower()

    # Một số rule nhỏ cho vui
    if "mèo" in vi:
        return SuggestResponse(sentence="I like cats.")
    if "chó" in vi:
        return SuggestResponse(sentence="I like dogs.")
    if "màu đỏ" in vi or "quả táo" in vi:
        return SuggestResponse(sentence="The apple is red.")
    if "số" in vi or "đếm" in vi:
        return SuggestResponse(sentence="I can count from one to ten.")

    topic_text = (req.topic or "").lower()
    level_text = (req.level or "").lower()

    for keyword, sentence in [
        ("chào", "Hello! Nice to meet you."),
        ("xin chào", "Hi there, friend!"),
        ("màu xanh", "The leaf is green."),
        ("gia đình", "My family loves me very much.")
    ]:
        if keyword in vi:
            return SuggestResponse(sentence=sentence)

    for key, sentences in TOPIC_LIBRARY.items():
        if key in topic_text:
            return SuggestResponse(sentence=random.choice(sentences))

    for key, sentences in LEVEL_LIBRARY.items():
        if key in level_text:
            return SuggestResponse(sentence=random.choice(sentences))

    templates = [
        "This is a simple English sentence.",
        "I am learning English every day.",
        "This sentence is easy for kids.",
        "English is fun and interesting.",
        "I like studying English with my friends."
    ]
    return SuggestResponse(sentence=random.choice(templates))


@app.post("/tts")
def generate_tts(req: TTSRequest):
    text = req.text.strip()
    if not text:
        return {"error": "Text is required"}
    file_id = str(uuid.uuid4())
    file_path = os.path.join(TTS_FOLDER, f"{file_id}.mp3")
    try:
        tts = gTTS(text=text, lang="en")
        tts.save(file_path)
    except Exception as exc:
        return {"error": f"Failed to synthesize audio: {exc}"}
    return {"audio_url": f"http://127.0.0.1:8000/tts/{file_id}"}


@app.get("/tts/{file_id}")
def serve_audio(file_id: str):
    file_path = os.path.join(TTS_FOLDER, f"{file_id}.mp3")
    if os.path.exists(file_path):
        return FileResponse(file_path, media_type="audio/mpeg")
    return {"error": "File not found"}


@app.get("/health")
def health_check():
    return {"status": "ok"}


def _compute_score(expected: str, answer: str) -> int:
    expected_words = set(re.findall(r"[a-z']+", expected.lower()))
    answer_words = set(re.findall(r"[a-z']+", answer.lower()))
    if not expected_words:
        return 0
    matched = len(expected_words & answer_words)
    score = int((matched / len(expected_words)) * 100)
    return max(0, min(100, score))


@app.post("/grade", response_model=GradeResponse)
def grade(req: GradeRequest):
    expected = req.expected.strip()
    answer = req.answer.strip()
    if not answer:
        return GradeResponse(
            score=0,
            commentEn="Please type your English sentence so I can grade it.",
            commentVi="Hãy nhập câu tiếng Anh của em để cô giáo AI chấm điểm nhé!",
        )

    score = _compute_score(expected, answer)

    if score >= 80:
        comment_en = "Great job! Your sentence matches the model answer."
        comment_vi = "Rất tốt! Câu của em gần giống câu mẫu rồi."
    elif score >= 50:
        comment_en = "Nice try! There are a few differences. Review the model sentence."
        comment_vi = "Cố lên! Có vài chỗ khác câu mẫu, hãy xem lại câu gợi ý nhé."
    else:
        comment_en = "Let's practice again. Try to follow the model sentence more closely."
        comment_vi = "Mình luyện lại nhé. Hãy cố bám sát câu mẫu hơn."

    return GradeResponse(
        score=score,
        commentEn=comment_en,
        commentVi=comment_vi,
    )


# ChatBot API
VOCABULARY_LIBRARY = {
    "apple": {
        "phonetic": "/ˈæp.əl/",
        "meaning": "Quả táo",
        "example": "I eat an apple every day."
    },
    "book": {
        "phonetic": "/bʊk/",
        "meaning": "Cuốn sách",
        "example": "This book is very interesting."
    },
    "cat": {
        "phonetic": "/kæt/",
        "meaning": "Con mèo",
        "example": "My cat is very cute."
    },
    "dog": {
        "phonetic": "/dɔɡ/",
        "meaning": "Con chó",
        "example": "The dog runs in the park."
    },
    "happy": {
        "phonetic": "/ˈhæp.i/",
        "meaning": "Vui vẻ, hạnh phúc",
        "example": "She is very happy today."
    },
    "hello": {
        "phonetic": "/həˈloʊ/",
        "meaning": "Xin chào",
        "example": "Hello, how are you?"
    },
    "school": {
        "phonetic": "/skuːl/",
        "meaning": "Trường học",
        "example": "I go to school every day."
    },
    "friend": {
        "phonetic": "/frend/",
        "meaning": "Bạn bè",
        "example": "He is my best friend."
    },
    "family": {
        "phonetic": "/ˈfæm.əl.i/",
        "meaning": "Gia đình",
        "example": "My family loves me very much."
    },
    "water": {
        "phonetic": "/ˈwɔː.tɚ/",
        "meaning": "Nước",
        "example": "Drink water every day for your health."
    }
}


@app.post("/api/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    """
    AI ChatBot sử dụng Hugging Face Text Generation
    """
    message = req.message.strip()
    
    if not message:
        return ChatResponse(reply="Please type a message to chat with me!")
    
    try:
        # Call Hugging Face Inference API
        headers = {"Authorization": f"Bearer {HF_API_TOKEN}"}
        
        payload = {
            "inputs": message[:80],
            "parameters": {
                "max_new_tokens": 60,
                "temperature": 0.7
            }
        }
        
        response = requests.post(
            "https://router.huggingface.co/models/distilgpt2",
            headers=headers,
            json=payload,
            timeout=15
        )
        
        # Handle different response codes
        if response.status_code == 200:
            try:
                result = response.json()
            except:
                return ChatResponse(reply="That's interesting! Can you tell me more?")
            
            if isinstance(result, list) and len(result) > 0:
                generated = result[0].get("generated_text", "")
                
                # Extract only the generated part
                if generated.startswith(message):
                    bot_reply = generated[len(message):].strip()
                else:
                    bot_reply = generated.strip()
                
                # Clean response
                bot_reply = bot_reply.replace("\n", " ").strip()
                if not bot_reply:
                    bot_reply = "Great topic! Let's continue."
                if len(bot_reply) > 150:
                    bot_reply = bot_reply[:150].rsplit(' ', 1)[0] + "..."
                
                return ChatResponse(reply=bot_reply)
        
        elif response.status_code == 503:
            return ChatResponse(reply="🤖 Model is loading... Try again in a moment!")
        
        else:
            return ChatResponse(reply="Let me think about that...")
    
    except requests.exceptions.Timeout:
        return ChatResponse(reply="Request timed out. Try a shorter message!")
    except:
        return ChatResponse(reply="That's a great question! Keep learning!")



@app.post("/api/vocabulary", response_model=VocabularyResponse)
def lookup_vocabulary(req: VocabularyRequest):
    """
    Look up vocabulary with definition, pronunciation, and examples
    """
    word = req.word.lower().strip()
    
    # Check if word exists in library
    if word in VOCABULARY_LIBRARY:
        vocab = VOCABULARY_LIBRARY[word]
        return VocabularyResponse(
            word=word,
            phonetic=vocab["phonetic"],
            meaning=vocab["meaning"],
            example=vocab["example"]
        )
    
    # If not found, return a generic response
    return VocabularyResponse(
        word=word,
        phonetic="/word/",
        meaning=f"Sorry, I don't have this word in my vocabulary database yet. Try another word!",
        example="Keep learning new words!"
    )


# Run the server
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
