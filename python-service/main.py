from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from pydantic import BaseModel
from gtts import gTTS
import uuid
import os
import random
import re

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
