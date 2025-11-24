# TỔNG QUAN CHỨC NĂNG WEB VÀ GIAO DIỆN WEB - KID NANI ENGLISH

## 1. TỔNG QUAN HỆ THỐNG

**Kid Nani English** là một ứng dụng web học tiếng Anh dành cho học sinh tiểu học (lớp 1-5), được xây dựng bằng Spring Boot (Java) với giao diện Thymeleaf. Hệ thống tích hợp AI Python service để hỗ trợ học tập.

---

## 2. KIẾN TRÚC GIAO DIỆN

### 2.1. Layout chung
- **Sidebar bên trái**: Menu điều hướng cố định với logo "Kid Nani"
- **Main content**: Nội dung chính của từng trang
- **Responsive design**: Giao diện thân thiện, dễ sử dụng

### 2.2. Menu điều hướng
- Trang chính (Dashboard)
- Lộ trình học (Lớp 1-5)
- Tất cả bài học
- Sổ tay từ vựng
- Ôn tập
- Mini game
- Bảng xếp hạng
- Cửa hàng tim
- Đăng xuất

---

## 3. CÁC CHỨC NĂNG CHI TIẾT

### 3.1. XÁC THỰC NGƯỜI DÙNG

#### 3.1.1. Đăng ký (`/register`)
- **Form đăng ký**:
  - Họ tên
  - Email
  - Mật khẩu
  - Lớp hiện tại (Lớp 1-5)
- **Xử lý**: Tạo tài khoản mới, khởi tạo thông tin người dùng

#### 3.1.2. Đăng nhập (`/login`)
- **Form đăng nhập**:
  - Email
  - Mật khẩu
- **Xử lý**: Xác thực và tạo session

---

### 3.2. TRANG CHÍNH (DASHBOARD) (`/dashboard`)

#### 3.2.1. Thông tin người dùng
- **Profile mini**:
  - Avatar (SVG hoặc hình tròn mặc định)
  - Tên hiển thị
  - Thông điệp chào mừng
- **Thống kê**:
  - 🔥 Streak (số ngày học liên tiếp)
  - ⚡ XP (điểm kinh nghiệm)
  - ❤️ Hearts (tim - năng lượng để làm bài)

#### 3.2.2. Tiếp tục học
- **Bài học cho học sinh tiểu học**: Link đến danh sách bài học
- **Ôn tập hôm nay**: Link đến trang ôn tập
- **Bảng xếp hạng**: Link đến bảng xếp hạng

#### 3.2.3. Nhiệm vụ hôm nay (Daily Goals)
- Hoàn thành 1 bài học ✅/⏳
- Hoàn thành 1 mini test ✅/⏳
- Giữ streak ≥ 3 ngày ✅/⏳

#### 3.2.4. Huy hiệu đã nhận (Achievements)
- Danh sách huy hiệu đã đạt được
- Tên huy hiệu, ngày nhận, mô tả

---

### 3.3. LỘ TRÌNH HỌC (`/grade/{GRADE1-5}`)

- Hiển thị danh sách bài học theo từng lớp
- Mỗi bài học hiển thị:
  - Tiêu đề
  - Mô tả
  - Nút "Học bài" để vào chi tiết

---

### 3.4. DANH SÁCH BÀI HỌC (`/lessons`)

- **Grid layout** hiển thị tất cả bài học
- Mỗi bài học card hiển thị:
  - Tiêu đề
  - Mô tả
  - XP thưởng
  - Nút "Học bài"

---

### 3.5. CHI TIẾT BÀI HỌC (`/lessons/{id}`)

#### 3.5.1. Thông tin bài học
- Tiêu đề bài học
- Badge hiển thị XP thưởng
- Mô tả
- Nội dung HTML của bài học

#### 3.5.2. Mini test
- Nút "Làm mini test để nhận XP"
- Link đến trang quiz của bài học

#### 3.5.3. AI gợi ý câu tiếng Anh
- **Chức năng**: Nhập câu tiếng Việt → AI gợi ý câu tiếng Anh
- **Form**:
  - Textarea nhập câu tiếng Việt
  - Nút "Gợi ý câu tiếng Anh"
- **Kết quả**:
  - Hiển thị câu gợi ý
  - Nút 🔊 "Nghe" để phát âm (TTS)
- **Tích hợp**: Python AI service

#### 3.5.4. Thêm từ vựng vào sổ tay
- **Form thêm từ**:
  - Từ tiếng Anh * (bắt buộc)
  - Nghĩa tiếng Việt * (bắt buộc)
  - Phiên âm (IPA) (tùy chọn)
  - Câu ví dụ (tùy chọn)
- **Tính năng đặc biệt**:
  - Click vào từ vựng trong nội dung bài học → Mở modal
  - Modal hiển thị từ, nghĩa, nút nghe phát âm
  - Tự động điền vào form thêm từ

#### 3.5.5. Modal từ vựng
- Hiển thị khi click vào từ trong bài học
- Thông tin: Từ tiếng Anh, nghĩa tiếng Việt
- Nút 🔊 "Nghe" để phát âm
- Tự động điền form thêm từ

---

### 3.6. MINI TEST / QUIZ (`/quiz/{lessonId}`)

#### 3.6.1. Trang làm bài
- Tiêu đề: "Mini test: [Tên bài học]"
- Hướng dẫn: "Chọn đáp án đúng. Đúng từ 70% trở lên sẽ được nhận XP."
- **Form quiz**:
  - Mỗi câu hỏi có 4 đáp án (A, B, C, D)
  - Radio buttons để chọn
  - Nút "Nộp bài"

#### 3.6.2. Trang kết quả (`/quiz/{lessonId}` - POST)
- **Thông tin kết quả**:
  - Số câu đúng / Tổng số câu
  - Số câu sai (nếu có) → Trừ tim tương ứng
  - Tim còn lại
- **Thông báo**:
  - 🎉 Đạt ≥70%: Nhận XP, cập nhật streak
  - 😢 Chưa đạt 70%: Yêu cầu học lại
  - ⚠️ Hết tim: Hướng dẫn mua tim
- **Các nút hành động**:
  - Về trang chính
  - Xem lại bài học
  - Vào cửa hàng tim

---

### 3.7. SỔ TAY TỪ VỰNG (`/mywords`)

#### 3.7.1. Thêm từ mới
- **Form**:
  - Từ tiếng Anh * (max 60 ký tự)
  - Nghĩa tiếng Việt * (max 120 ký tự)
  - Phiên âm IPA (max 80 ký tự)
  - Câu ví dụ (max 180 ký tự)
- Nút "Lưu vào sổ tay"

#### 3.7.2. Từ đã lưu
- **Grid layout** hiển thị các từ đã lưu
- **Mỗi card từ vựng**:
  - Từ tiếng Anh (in đậm)
  - Phiên âm IPA (nếu có)
  - Nghĩa tiếng Việt
  - Câu ví dụ (nếu có)
  - Nút "Xóa" để xóa từ
- **Thông báo**: Nếu chưa có từ nào

---

### 3.8. ÔN TẬP (`/practice`)

#### 3.8.1. Nội dung ôn tập
- Hiển thị lại một phần bài học đã học
- Tiêu đề bài học
- Nội dung HTML của bài
- Nút "Làm mini test"

#### 3.8.2. AI chấm điểm câu trả lời
- **Đề bài**: Câu hỏi tiếng Anh
- **Câu mẫu**: Câu trả lời mẫu
- **Form chấm điểm**:
  - Textarea nhập câu trả lời của học sinh
  - Nút "Chấm điểm"
- **Kết quả chấm**:
  - Điểm số /100
  - Feedback (tiếng Anh)
  - Nhận xét (tiếng Việt)
  - Câu mẫu
- **Tích hợp**: Python AI service để chấm điểm tự động

---

### 3.9. MINI GAME (`/minigame`)

#### 3.9.1. Mô tả
- Trả lời 5 câu hỏi nhanh
- Mỗi câu đúng: +10 điểm, +5 XP

#### 3.9.2. Form chơi game
- 5 câu hỏi trắc nghiệm
- Radio buttons để chọn đáp án
- Nút "Nộp kết quả"

#### 3.9.3. Kết quả vòng chơi
- Điểm vòng này
- Số câu đúng / Tổng số câu
- XP nhận được
- Kỷ lục của bạn (best score)

#### 3.9.4. Bảng xếp hạng mini game
- **Bảng hiển thị**:
  - Hạng
  - Học sinh
  - Điểm tích lũy
  - Best vòng (điểm cao nhất 1 vòng)
- Highlight dòng của người dùng hiện tại

---

### 3.10. BẢNG XẾP HẠNG (`/leaderboard`)

- **Mô tả**: Top 10 học sinh có XP cao nhất
- **Bảng hiển thị**:
  - Hạng
  - Học sinh (tên hiển thị)
  - XP
  - Streak (số ngày học liên tiếp)
- Highlight dòng của người dùng hiện tại với text "(bạn)"

---

### 3.11. CỬA HÀNG TIM (`/shop`)

#### 3.11.1. Thông tin hiện tại
- XP hiện tại
- Tim hiện tại ❤️
- Thông báo nếu hết tim (khi redirect từ quiz)

#### 3.11.2. Mua tim bằng XP
- **Form mua tim**:
  - Giá: Mỗi tim = [xpPerHeart] XP (mặc định 10 XP)
  - Input số lượng tim muốn mua
  - Nút "Mua tim"
- **Thông báo**: Success/Error messages

#### 3.11.3. Lịch sử giao dịch
- **Bảng lịch sử**:
  - Thời gian
  - Loại (BUY, ...)
  - Tim (số lượng thay đổi: +1, -1, ...)
  - XP (số lượng thay đổi: -10, ...)
  - Ghi chú
- Hiển thị "Chưa có giao dịch nào" nếu rỗng

---

## 4. HỆ THỐNG ĐIỂM VÀ PHẦN THƯỞNG

### 4.1. XP (Experience Points)
- **Nhận XP khi**:
  - Hoàn thành bài học
  - Làm mini test đạt ≥70%
  - Chơi mini game (mỗi câu đúng +5 XP)
- **Sử dụng XP**:
  - Mua tim (10 XP/tim)

### 4.2. Hearts (Tim)
- **Chức năng**: Năng lượng để làm bài kiểm tra
- **Mất tim khi**:
  - Trả lời sai trong quiz (mỗi câu sai -1 tim)
- **Nhận tim**:
  - Mua bằng XP
  - Tự động nạp lại hàng ngày (nếu có cơ chế)

### 4.3. Streak
- Số ngày học liên tiếp
- Cập nhật khi hoàn thành nhiệm vụ hôm nay
- Hiển thị trên dashboard và leaderboard

### 4.4. Achievements (Huy hiệu)
- Nhận huy hiệu khi đạt các thành tích
- Hiển thị trên dashboard với tên, ngày nhận, mô tả

---

## 5. TÍCH HỢP AI PYTHON SERVICE

### 5.1. AI gợi ý câu tiếng Anh
- **Endpoint**: `/lessons/{id}/suggest`
- **Input**: Câu tiếng Việt
- **Output**: Câu tiếng Anh gợi ý + Audio TTS

### 5.2. AI chấm điểm
- **Endpoint**: `/practice/grade`
- **Input**: Câu hỏi, câu mẫu, câu trả lời của học sinh
- **Output**: Điểm số, feedback (EN), nhận xét (VI)

### 5.3. Text-to-Speech (TTS)
- **Endpoint**: `/tts?text={text}`
- **Chức năng**: Chuyển đổi text thành audio
- **Sử dụng**: Nút 🔊 "Nghe" ở nhiều nơi

---

## 6. CƠ CHẾ HOẠT ĐỘNG

### 6.1. Session Management
- Sử dụng HttpSession để lưu thông tin user
- Redirect về `/login` nếu chưa đăng nhập

### 6.2. Daily Goals
- Theo dõi nhiệm vụ hôm nay
- Cập nhật khi hoàn thành bài học, quiz
- Kiểm tra streak

### 6.3. Heart System
- Kiểm tra tim trước khi làm quiz
- Trừ tim khi trả lời sai
- Redirect đến shop nếu hết tim

---

## 7. GIAO DIỆN VÀ UX

### 7.1. Màu sắc và Style
- Giao diện thân thiện, phù hợp trẻ em
- Sử dụng emoji để tăng tính trực quan (🔥, ⚡, ❤️, 🎉, 😢, ⚠️, 🏅, 🔊)
- Card-based layout
- Button styles: `btn-main`, `btn-main big`

### 7.2. Responsive Design
- Sidebar cố định bên trái
- Main content linh hoạt
- Grid layout cho danh sách

### 7.3. Thông báo
- **Success messages**: Màu xanh
- **Error messages**: Màu đỏ
- **Info messages**: Màu xám/xanh nhạt

---

## 8. CÁC TRANG VÀ ROUTE

| Route | Mô tả | Yêu cầu Auth |
|-------|-------|--------------|
| `/` | Trang chủ (redirect đến dashboard) | ✅ |
| `/dashboard` | Trang chính | ✅ |
| `/login` | Đăng nhập | ❌ |
| `/register` | Đăng ký | ❌ |
| `/logout` | Đăng xuất | ✅ |
| `/lessons` | Danh sách bài học | ✅ |
| `/lessons/{id}` | Chi tiết bài học | ✅ |
| `/lessons/{id}/suggest` | AI gợi ý câu | ✅ |
| `/lessons/{id}/mywords` | Thêm từ vào sổ tay | ✅ |
| `/quiz/{lessonId}` | Mini test | ✅ |
| `/mywords` | Sổ tay từ vựng | ✅ |
| `/mywords/{id}/delete` | Xóa từ | ✅ |
| `/practice` | Ôn tập | ✅ |
| `/practice/grade` | AI chấm điểm | ✅ |
| `/minigame` | Mini game | ✅ |
| `/minigame/submit` | Nộp kết quả game | ✅ |
| `/leaderboard` | Bảng xếp hạng | ✅ |
| `/shop` | Cửa hàng tim | ✅ |
| `/shop/buy-hearts` | Mua tim | ✅ |
| `/grade/{GRADE1-5}` | Lộ trình theo lớp | ✅ |
| `/tts` | Text-to-Speech | ✅ |

---

## 9. DATABASE MODELS

### 9.1. User
- Thông tin người dùng: email, password, displayName, gradeLevel
- Thống kê: xp, hearts, streak
- Avatar

### 9.2. Lesson
- Tiêu đề, mô tả, nội dung HTML
- XP thưởng
- Grade level

### 9.3. QuizQuestion
- Câu hỏi, 4 đáp án (A, B, C, D), đáp án đúng
- Liên kết với Lesson

### 9.4. MyWord
- Từ tiếng Anh, nghĩa tiếng Việt
- IPA, câu ví dụ
- Liên kết với User

### 9.5. DailyGoalProgress
- Theo dõi nhiệm vụ hôm nay
- lessonCompleted, quizCompleted

### 9.6. Achievement
- Tên, mô tả, ngày nhận
- Liên kết với User

### 9.7. VocabGameScore
- Điểm tích lũy, best round score
- Liên kết với User

### 9.8. ShopTransaction
- Lịch sử giao dịch
- Loại, tim, XP thay đổi, ghi chú

---

## 10. TÍNH NĂNG NỔI BẬT

1. **Gamification**: XP, hearts, streak, achievements
2. **AI Integration**: Gợi ý câu, chấm điểm tự động
3. **TTS**: Phát âm từ vựng và câu
4. **Personal Vocabulary**: Sổ tay từ vựng cá nhân
5. **Daily Goals**: Nhiệm vụ hằng ngày
6. **Leaderboard**: Bảng xếp hạng kích thích học tập
7. **Mini Games**: Học qua chơi
8. **Grade-based Learning**: Lộ trình theo lớp học

---

## 11. KẾT LUẬN

**Kid Nani English** là một hệ thống học tiếng Anh toàn diện với:
- Giao diện thân thiện, dễ sử dụng
- Nhiều tính năng gamification
- Tích hợp AI để hỗ trợ học tập
- Hệ thống điểm thưởng và phần thưởng
- Theo dõi tiến độ học tập chi tiết

Hệ thống phù hợp cho học sinh tiểu học với mục tiêu học tiếng Anh một cách vui vẻ và hiệu quả.

