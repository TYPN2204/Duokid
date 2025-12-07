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
from functools import lru_cache

# Hugging Face API Configuration - Load from environment variable (free tier, but token recommended)
HF_API_TOKEN = os.getenv("HF_API_TOKEN", "")
# Prefer a stronger free instruct model on HF
HF_MODEL = "meta-llama/Meta-Llama-3-8B-Instruct"
# New HF router endpoint (OpenAI-compatible)
HF_API_URL = "https://router.huggingface.co/v1/chat/completions"

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
print(f"🤖 Using model: {HF_MODEL}")
print(f"🔌 API Endpoint: {HF_API_URL}")




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


# Simple translation helper (MyMemory) to get Vietnamese meaning
@lru_cache(maxsize=256)
def translate_to_vi(text: str) -> str:
    text = (text or "").strip()
    if not text:
        return ""
    try:
        resp = requests.get(
            "https://api.mymemory.translated.net/get",
            params={"q": text, "langpair": "en|vi"},
            timeout=6,
        )
        if resp.status_code == 200:
            data = resp.json()
            trans = data.get("responseData", {}).get("translatedText")
            if trans:
                return trans
    except Exception:
        return text
    return text

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
    
    prompt = (
        "Bạn là trợ lý dạy tiếng Anh cho thiếu nhi, trả lời NGẮN gọn bằng tiếng Việt (ưu tiên)."
        " Nếu cần ví dụ tiếng Anh thì chỉ 1 câu ngắn kèm dịch Việt."
        " Nếu người dùng hỏi định nghĩa, hãy giải thích ngắn + dịch nghĩa Việt."
        " Nếu người dùng hỏi phát âm, hãy nêu IPA và hướng dẫn khẩu hình ngắn."
        f"\nUser: {message}\nAssistant:"
    )

    # Trả lời nhanh các câu hỏi dạng "X là gì" hoặc "what is X"
    lower_msg = message.lower()
    def maybe_define_from_vocab() -> str | None:
        # Heuristic extraction of target word/phrase
        target = None
        if " la gi" in lower_msg or " là gì" in lower_msg:
            target = message.replace("là gì", "").replace("la gi", "").strip(" ?!.")
        elif lower_msg.startswith("what is "):
            target = message[8:].strip(" ?!.")
        elif "meaning of" in lower_msg:
            target = lower_msg.split("meaning of", 1)[-1].strip(" ?!.")
        elif "define" in lower_msg:
            target = lower_msg.split("define", 1)[-1].strip(" ?!.")
        if not target:
            return None
        # Call local vocabulary logic
        try:
            vreq = VocabularyRequest(word=target)
            vresp = lookup_vocabulary(vreq)
            meaning_vi = vresp.meaning
            if meaning_vi:
                return f"{target}: {meaning_vi}"
        except Exception:
            return None
        return None

    def maybe_pronounce() -> str | None:
        intent_words = ["phat am", "phát âm", "pronounce", "pronunciation", "ipa"]
        if not any(w in lower_msg for w in intent_words):
            return None
        target = None
        for sep in ["phát âm", "phat am", "pronounce", "pronunciation", "ipa"]:
            if sep in lower_msg:
                target = lower_msg.split(sep, 1)[-1].strip(" ?!.,\"'")
                break
        if not target:
            parts = lower_msg.split()
            target = parts[-1].strip("\"'") if parts else None
        if not target:
            return None
        try:
            vreq = VocabularyRequest(word=target)
            vresp = lookup_vocabulary(vreq)
            ipa = vresp.phonetic or "(chưa rõ IPA)"
            meaning_vi = vresp.meaning or ""
            return f"Phát âm '{target}': {ipa}. Nghĩa: {meaning_vi}"
        except Exception:
            return None

    pron = maybe_pronounce()
    if pron:
        return ChatResponse(reply=pron)

    quick_def = maybe_define_from_vocab()
    if quick_def:
        return ChatResponse(reply=quick_def)

    try:
        headers = {"Content-Type": "application/json"}
        if HF_API_TOKEN:
            headers["Authorization"] = f"Bearer {HF_API_TOKEN}"

        payload = {
            "model": HF_MODEL,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "Bạn là trợ lý dạy tiếng Anh cho thiếu nhi. Luôn trả lời NGẮN gọn (2-3 câu tối đa) bằng tiếng Việt, rõ ràng và vui vẻ. "
                        "Khi cần ví dụ tiếng Anh, chỉ đưa 1 câu ngắn và kèm dịch Việt. "
                        "Nếu hỏi định nghĩa: trả lời 1-2 câu tiếng Việt + 1 ví dụ ngắn (Anh + dịch). "
                        "Nếu hỏi phát âm: đưa IPA, nhấn trọng âm và gợi ý khẩu hình 1 câu. "
                        "Nếu hỏi ví dụ: cho 2 câu tiếng Anh khác nhau, mỗi câu kèm dịch Việt. "
                        "Nếu hỏi hướng dẫn học: đưa 3 bước cụ thể, ngắn gọn dễ làm theo. "
                        "Nếu câu hỏi về ngữ pháp: giải thích cách dùng rồi cho 1-2 ví dụ. "
                        "Nếu câu hỏi chung chung hoặc mơ hồ, hãy hỏi lại để hiểu rõ hơn."
                    ),
                },
                {"role": "user", "content": message}
            ],
            "max_tokens": 200,
            "temperature": 0.75,
            "top_p": 0.9,
        }

        response = requests.post(HF_API_URL, headers=headers, json=payload, timeout=25)

        if response.status_code == 503:
            print(f"⚠️ HF Model loading: {response.text[:200]}")
            return ChatResponse(reply="🤖 Model đang khởi động trên HuggingFace, thử lại sau vài giây nhé!")

        if response.status_code == 401:
            print(f"❌ HF Auth failed: {response.text[:200]}")
            return ChatResponse(reply="Cần thiết lập biến môi trường HF_API_TOKEN (miễn phí trên HuggingFace) để dùng AI thông minh.")

        if response.status_code != 200:
            print(f"⚠️ HF Error {response.status_code}: {response.text[:300]}")
            return ChatResponse(reply="Hiện đang gặp sự cố với AI. Thử lại sau ít phút nhé!")

        data = response.json()
        generated = ""
        if isinstance(data, dict):
            choices = data.get("choices") or []
            if choices:
                generated = choices[0].get("message", {}).get("content", "")

        bot_reply = (generated or "Let me think about that...").strip()
        bot_reply = bot_reply.replace("\n", " ")
        if len(bot_reply) > 200:
            bot_reply = bot_reply[:200].rsplit(" ", 1)[0] + "..."

        if not bot_reply:
            bot_reply = "Great topic! Let's keep talking."

        return ChatResponse(reply=bot_reply)

    except requests.exceptions.Timeout:
        pass
    except Exception:
        pass

    # Offline fallback
    offline_templates = [
        "I'm here and ready to chat!",
        "Great question! Let's practice more.",
        "Keep it up! What else would you like to learn?",
    ]
    return ChatResponse(reply=random.choice(offline_templates))



@app.post("/api/vocabulary", response_model=VocabularyResponse)
def lookup_vocabulary(req: VocabularyRequest):
    """
    Look up vocabulary with definition, pronunciation, and examples
    """
    word = req.word.lower().strip()
    
    # Check local library first
    if word in VOCABULARY_LIBRARY:
        vocab = VOCABULARY_LIBRARY[word]
        return VocabularyResponse(
            word=word,
            phonetic=vocab["phonetic"],
            meaning=vocab["meaning"],
            example=vocab["example"],
        )

    # Try free dictionary API for broader coverage
    try:
        resp = requests.get(
            f"https://api.dictionaryapi.dev/api/v2/entries/en/{word}", timeout=8
        )
        if resp.status_code == 200:
            data = resp.json()
            if isinstance(data, list) and data:
                entry = data[0]
                phonetics = entry.get("phonetics", [])
                meanings = entry.get("meanings", [])

                phon = ""
                for ph in phonetics:
                    if ph.get("text"):
                        phon = ph.get("text")
                        break

                # pick a friendly definition (avoid gym exercise noise)
                def pick_def():
                    if not meanings:
                        return None, None
                    # first pass: avoid definitions mentioning exercise/barbell/waist
                    bad_keywords = ["exercise", "barbell", "waist"]
                    prefer_pos = {"interjection", "exclamation", "phrase", "idiom"}
                    for m in meanings:
                        defs = m.get("definitions", [])
                        pos = (m.get("partOfSpeech") or "").lower()
                        for d in defs:
                            definition = d.get("definition", "")
                            if not definition:
                                continue
                            low = definition.lower()
                            if any(bk in low for bk in bad_keywords):
                                continue
                            if pos in prefer_pos or "greet" in low or "good morning" in low:
                                return definition, d.get("example", "") or ""
                    # second pass: first available definition
                    for m in meanings:
                        defs = m.get("definitions", [])
                        if defs:
                            d = defs[0]
                            return d.get("definition", ""), d.get("example", "") or ""
                    return None, None

                meaning_text, example_text = pick_def()

                if meaning_text:
                    vi_meaning = translate_to_vi(meaning_text)
                    return VocabularyResponse(
                        word=word,
                        phonetic=phon or "/word/",
                        meaning=vi_meaning,
                        example=example_text or f"Example: {word}",
                    )
    except requests.exceptions.Timeout:
        pass
    except Exception:
        pass

    return VocabularyResponse(
        word=word,
        phonetic="/word/",
        meaning="Xin lỗi, từ này chưa có trong từ điển online lúc này. Thử lại sau nhé!",
        example="Keep learning new words!",
    )


# Run the server
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
