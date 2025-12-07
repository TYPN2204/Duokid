# 🤖 ChatBot Feature - Quick Start Guide

## ⚡ Khởi động nhanh (2 bước)

### Step 1: Backend Java (Terminal 1)
```bash
cd /workspaces/Duokid/backend-java
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
./mvnw spring-boot:run
```

### Step 2: Python Service (Terminal 2)
```bash
cd /workspaces/Duokid/python-service
pip install -r requirements.txt
python main.py
```

## 🌐 Truy cập

Mở trình duyệt vào: **http://localhost:8080/chatbot**

## 💬 Sử dụng

### Panel 1: Chat với AI
- Nhập câu hỏi bất kỳ
- Hoặc click nút nhanh: Phát âm | Ví dụ | Định nghĩa | Hướng dẫn

### Panel 2: Giải Thích Từ Vựng
- Nhập từ tiếng Anh (VD: "apple")
- Xem định nghĩa + ví dụ + phát âm
- Click "⭐ Thêm vào Sổ Tay" để lưu

## 📚 Từ vựng có sẵn

apple, book, cat, dog, happy, hello, school, friend, family, water

## ✅ Ready

Tất cả đã sẵn sàng để chạy! 🚀

---

Xem `DETAILED_CHANGES.md` để biết chi tiết các thay đổi.
