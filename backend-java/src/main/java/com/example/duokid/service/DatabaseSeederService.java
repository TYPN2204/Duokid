package com.example.duokid.service;

import com.example.duokid.model.*;
import com.example.duokid.repo.*;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Service để seed dữ liệu mới vào database
 * Chỉ thêm dữ liệu mới, không xóa dữ liệu cũ
 */
@Service
public class DatabaseSeederService {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;

    public DatabaseSeederService(UserRepository userRepository,
                                LessonRepository lessonRepository,
                                QuizQuestionRepository quizQuestionRepository,
                                TestRepository testRepository,
                                TestQuestionRepository testQuestionRepository) {
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
    }

    /**
     * Tự động seed dữ liệu khi khởi động nếu database trống
     */
    @PostConstruct
    public void autoSeedIfEmpty() {
        try {
            // Chỉ seed nếu chưa có user nào (database mới)
            if (userRepository.count() == 0) {
                System.out.println("📦 Database trống, tự động seed dữ liệu mẫu...");
                seedDatabase();
            } else {
                System.out.println("ℹ️  Database đã có dữ liệu, bỏ qua auto-seed");
            }
        } catch (Exception e) {
            System.err.println("⚠️  Lỗi khi auto-seed: " + e.getMessage());
            // Không throw exception để không làm crash ứng dụng
        }
    }

    /**
     * Seed dữ liệu mới vào database
     * Chỉ thêm nếu chưa tồn tại
     */
    public void seedDatabase() {
        System.out.println("🌱 Bắt đầu seed dữ liệu mới...");
        
        seedUsers();
        seedLessons();
        seedTests();
        seedTestQuestions();
        seedQuizQuestions();
        
        System.out.println("✅ Hoàn thành seed dữ liệu!");
    }

    /**
     * Tạo các user mẫu
     */
    private void seedUsers() {
        // Tạo admin user nếu chưa có
        if (userRepository.findByEmail("admin@duokid.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@duokid.com");
            admin.setPassword(BCrypt.hashpw("admin123", BCrypt.gensalt()));
            admin.setDisplayName("Admin");
            admin.setIsAdmin(true);
            admin.setAvatar("/avatar1.svg");
            admin.setStreak(0);
            admin.setXp(0);
            admin.setGems(1000);
            admin.setHearts(5);
            admin.setGradeLevel("GRADE1");
            admin.setLastHeartRefillDate(LocalDate.now());
            userRepository.save(admin);
            System.out.println("  ✅ Đã tạo admin user: admin@duokid.com / admin123");
        }

        // Tạo test user nếu chưa có
        if (userRepository.findByEmail("test@duokid.com").isEmpty()) {
            User testUser = new User();
            testUser.setEmail("test@duokid.com");
            testUser.setPassword(BCrypt.hashpw("test123", BCrypt.gensalt()));
            testUser.setDisplayName("Test User");
            testUser.setIsAdmin(false);
            testUser.setAvatar("/avatar2.svg");
            testUser.setStreak(5);
            testUser.setXp(150);
            testUser.setGems(750);
            testUser.setHearts(5);
            testUser.setGradeLevel("GRADE2");
            testUser.setLastStudyDate(LocalDate.now());
            testUser.setLastHeartRefillDate(LocalDate.now());
            userRepository.save(testUser);
            System.out.println("  ✅ Đã tạo test user: test@duokid.com / test123");
        }

        // Tạo student user nếu chưa có
        if (userRepository.findByEmail("student@duokid.com").isEmpty()) {
            User student = new User();
            student.setEmail("student@duokid.com");
            student.setPassword(BCrypt.hashpw("student123", BCrypt.gensalt()));
            student.setDisplayName("Student");
            student.setIsAdmin(false);
            student.setAvatar("/avatar3.svg");
            student.setStreak(3);
            student.setXp(80);
            student.setGems(600);
            student.setHearts(4);
            student.setGradeLevel("GRADE3");
            student.setLastStudyDate(LocalDate.now().minusDays(1));
            student.setLastHeartRefillDate(LocalDate.now());
            userRepository.save(student);
            System.out.println("  ✅ Đã tạo student user: student@duokid.com / student123");
        }
    }

    /**
     * Tạo các lesson mẫu nếu chưa có
     */
    private void seedLessons() {
        long existingLessons = lessonRepository.count();
        if (existingLessons > 0) {
            System.out.println("  ℹ️  Đã có " + existingLessons + " bài học, bỏ qua tạo lesson mẫu");
            return;
        }

        // Lesson 1: Greetings
        Lesson lesson1 = new Lesson();
        lesson1.setTitle("Greetings - Chào hỏi");
        lesson1.setDescription("Học cách chào hỏi trong tiếng Anh");
        lesson1.setXpReward(10);
        lesson1.setLevel("GRADE1");
        lesson1.setOrderIndex(0);
        lesson1.setPartName("PHẦN 1, CỬA 1");
        lesson1.setLessonType("VOCABULARY");
        lesson1.setContentHtml("<h3>Chào hỏi cơ bản</h3>" +
                "<ul>" +
                "<li><b>Hello</b> - Xin chào</li>" +
                "<li><b>Hi</b> - Chào</li>" +
                "<li><b>Good morning</b> - Chào buổi sáng</li>" +
                "<li><b>Good afternoon</b> - Chào buổi chiều</li>" +
                "<li><b>Good evening</b> - Chào buổi tối</li>" +
                "<li><b>Goodbye</b> - Tạm biệt</li>" +
                "<li><b>See you later</b> - Hẹn gặp lại</li>" +
                "</ul>");
        lessonRepository.save(lesson1);

        // Lesson 2: Numbers
        Lesson lesson2 = new Lesson();
        lesson2.setTitle("Numbers - Số đếm");
        lesson2.setDescription("Học số đếm từ 1 đến 10");
        lesson2.setXpReward(10);
        lesson2.setLevel("GRADE1");
        lesson2.setOrderIndex(1);
        lesson2.setPartName("PHẦN 1, CỬA 2");
        lesson2.setLessonType("VOCABULARY");
        lesson2.setContentHtml("<h3>Số đếm từ 1 đến 10</h3>" +
                "<ul>" +
                "<li><b>One</b> - Một (1)</li>" +
                "<li><b>Two</b> - Hai (2)</li>" +
                "<li><b>Three</b> - Ba (3)</li>" +
                "<li><b>Four</b> - Bốn (4)</li>" +
                "<li><b>Five</b> - Năm (5)</li>" +
                "<li><b>Six</b> - Sáu (6)</li>" +
                "<li><b>Seven</b> - Bảy (7)</li>" +
                "<li><b>Eight</b> - Tám (8)</li>" +
                "<li><b>Nine</b> - Chín (9)</li>" +
                "<li><b>Ten</b> - Mười (10)</li>" +
                "</ul>");
        lessonRepository.save(lesson2);

        // Lesson 3: Colors
        Lesson lesson3 = new Lesson();
        lesson3.setTitle("Colors - Màu sắc");
        lesson3.setDescription("Học tên các màu sắc cơ bản");
        lesson3.setXpReward(10);
        lesson3.setLevel("GRADE1");
        lesson3.setOrderIndex(2);
        lesson3.setPartName("PHẦN 1, CỬA 3");
        lesson3.setLessonType("VOCABULARY");
        lesson3.setContentHtml("<h3>Màu sắc cơ bản</h3>" +
                "<ul>" +
                "<li><b>Red</b> - Đỏ</li>" +
                "<li><b>Blue</b> - Xanh dương</li>" +
                "<li><b>Green</b> - Xanh lá</li>" +
                "<li><b>Yellow</b> - Vàng</li>" +
                "<li><b>Orange</b> - Cam</li>" +
                "<li><b>Purple</b> - Tím</li>" +
                "<li><b>Pink</b> - Hồng</li>" +
                "<li><b>Black</b> - Đen</li>" +
                "<li><b>White</b> - Trắng</li>" +
                "</ul>");
        lessonRepository.save(lesson3);

        // Lesson 4: Animals
        Lesson lesson4 = new Lesson();
        lesson4.setTitle("Animals - Động vật");
        lesson4.setDescription("Học tên các con vật");
        lesson4.setXpReward(10);
        lesson4.setLevel("GRADE2");
        lesson4.setOrderIndex(0);
        lesson4.setPartName("PHẦN 2, CỬA 1");
        lesson4.setLessonType("VOCABULARY");
        lesson4.setContentHtml("<h3>Động vật</h3>" +
                "<ul>" +
                "<li><b>Cat</b> - Con mèo</li>" +
                "<li><b>Dog</b> - Con chó</li>" +
                "<li><b>Bird</b> - Con chim</li>" +
                "<li><b>Fish</b> - Con cá</li>" +
                "<li><b>Duck</b> - Con vịt</li>" +
                "<li><b>Cow</b> - Con bò</li>" +
                "<li><b>Horse</b> - Con ngựa</li>" +
                "<li><b>Pig</b> - Con lợn</li>" +
                "</ul>");
        lessonRepository.save(lesson4);

        // Lesson 5: Food
        Lesson lesson5 = new Lesson();
        lesson5.setTitle("Food - Đồ ăn");
        lesson5.setDescription("Học tên các loại đồ ăn");
        lesson5.setXpReward(10);
        lesson5.setLevel("GRADE2");
        lesson5.setOrderIndex(1);
        lesson5.setPartName("PHẦN 2, CỬA 2");
        lesson5.setLessonType("VOCABULARY");
        lesson5.setContentHtml("<h3>Đồ ăn</h3>" +
                "<ul>" +
                "<li><b>Apple</b> - Quả táo</li>" +
                "<li><b>Banana</b> - Quả chuối</li>" +
                "<li><b>Bread</b> - Bánh mì</li>" +
                "<li><b>Milk</b> - Sữa</li>" +
                "<li><b>Rice</b> - Cơm</li>" +
                "<li><b>Chicken</b> - Thịt gà</li>" +
                "<li><b>Egg</b> - Trứng</li>" +
                "</ul>");
        lessonRepository.save(lesson5);

        System.out.println("  ✅ Đã tạo 5 bài học mẫu");
    }

    /**
     * Tạo các test mẫu nếu chưa có
     */
    private void seedTests() {
        long existingTests = testRepository.count();
        if (existingTests > 0) {
            System.out.println("  ℹ️  Đã có " + existingTests + " bài test, bỏ qua tạo test mẫu");
            return;
        }

        // Test 1: Sau 3 bài học
        Test test1 = new Test();
        test1.setTitle("Test 1 - Kiểm tra cơ bản");
        test1.setDescription("Kiểm tra kiến thức cơ bản sau 3 bài học");
        test1.setLevel("GRADE1");
        test1.setAfterLessons(3);
        test1.setPassingScore(70);
        test1.setHeartsLostOnFail(1);
        test1.setXpReward(20);
        test1.setGemsReward(10);
        test1.setInstructions("Hoàn thành bài test này để nhận phần thưởng!");
        testRepository.save(test1);

        // Test 2: Sau 5 bài học
        Test test2 = new Test();
        test2.setTitle("Test 2 - Kiểm tra nâng cao");
        test2.setDescription("Kiểm tra kiến thức nâng cao sau 5 bài học");
        test2.setLevel("GRADE2");
        test2.setAfterLessons(5);
        test2.setPassingScore(75);
        test2.setHeartsLostOnFail(1);
        test2.setXpReward(30);
        test2.setGemsReward(15);
        test2.setInstructions("Hoàn thành bài test này để nhận phần thưởng lớn hơn!");
        testRepository.save(test2);

        System.out.println("  ✅ Đã tạo 2 bài test mẫu");
    }

    /**
     * Tạo các test questions mẫu nếu chưa có
     */
    private void seedTestQuestions() {
        List<Test> tests = testRepository.findAll();
        if (tests.isEmpty()) {
            System.out.println("  ⚠️  Không có test nào, bỏ qua tạo test questions");
            return;
        }

        Test firstTest = tests.get(0);
        
        // Kiểm tra xem đã có test questions cho test này chưa
        List<TestQuestion> existing = testQuestionRepository.findByTest(firstTest);
        if (!existing.isEmpty()) {
            System.out.println("  ℹ️  Đã có " + existing.size() + " câu hỏi test, bỏ qua tạo test questions mẫu");
            return;
        }

        // Test Question 1
        TestQuestion tq1 = new TestQuestion();
        tq1.setTest(firstTest);
        tq1.setQuestion("What is the English word for 'Xin chào'?");
        tq1.setOptionA("Goodbye");
        tq1.setOptionB("Hello");
        tq1.setOptionC("Thank you");
        tq1.setOptionD("Sorry");
        tq1.setCorrectOption("B");
        tq1.setExplanation("Hello có nghĩa là xin chào trong tiếng Anh");
        testQuestionRepository.save(tq1);

        // Test Question 2
        TestQuestion tq2 = new TestQuestion();
        tq2.setTest(firstTest);
        tq2.setQuestion("How do you say 'Tạm biệt' in English?");
        tq2.setOptionA("Hello");
        tq2.setOptionB("Hi");
        tq2.setOptionC("Goodbye");
        tq2.setOptionD("Good morning");
        tq2.setCorrectOption("C");
        tq2.setExplanation("Goodbye có nghĩa là tạm biệt trong tiếng Anh");
        testQuestionRepository.save(tq2);

        // Test Question 3
        TestQuestion tq3 = new TestQuestion();
        tq3.setTest(firstTest);
        tq3.setQuestion("What does 'Good morning' mean?");
        tq3.setOptionA("Chào buổi tối");
        tq3.setOptionB("Chào buổi chiều");
        tq3.setOptionC("Chào buổi sáng");
        tq3.setOptionD("Tạm biệt");
        tq3.setCorrectOption("C");
        tq3.setExplanation("Good morning có nghĩa là chào buổi sáng");
        testQuestionRepository.save(tq3);

        System.out.println("  ✅ Đã tạo 3 câu hỏi test mẫu");
    }

    /**
     * Tạo các quiz questions mẫu cho tất cả lessons nếu chưa có
     */
    private void seedQuizQuestions() {
        List<Lesson> lessons = lessonRepository.findAll();
        if (lessons.isEmpty()) {
            System.out.println("  ⚠️  Không có lesson nào, bỏ qua tạo quiz questions");
            return;
        }

        int totalCreated = 0;
        
        // Tạo quiz questions cho mỗi lesson
        for (Lesson lesson : lessons) {
            // Kiểm tra xem đã có quiz questions cho lesson này chưa
            List<QuizQuestion> existing = quizQuestionRepository.findByLesson(lesson);
            if (!existing.isEmpty()) {
                continue; // Đã có quiz, bỏ qua
            }

            // Tạo quiz questions dựa trên nội dung lesson
            String lessonTitle = lesson.getTitle() != null ? lesson.getTitle().toLowerCase() : "";
            int questionsCreated = 0;

            // Quiz questions cho Greetings
            if (lessonTitle.contains("greeting") || lessonTitle.contains("chào")) {
                QuizQuestion q1 = new QuizQuestion();
                q1.setLesson(lesson);
                q1.setQuestion("How do you greet someone in the morning?");
                q1.setOptionA("Good night");
                q1.setOptionB("Good morning");
                q1.setOptionC("Goodbye");
                q1.setOptionD("See you");
                q1.setCorrectOption("B");
                q1.setExplanation("Good morning là cách chào buổi sáng trong tiếng Anh");
                quizQuestionRepository.save(q1);
                questionsCreated++;

                QuizQuestion q2 = new QuizQuestion();
                q2.setLesson(lesson);
                q2.setQuestion("What does 'Hello' mean?");
                q2.setOptionA("Tạm biệt");
                q2.setOptionB("Xin chào");
                q2.setOptionC("Cảm ơn");
                q2.setOptionD("Xin lỗi");
                q2.setCorrectOption("B");
                q2.setExplanation("Hello có nghĩa là xin chào");
                quizQuestionRepository.save(q2);
                questionsCreated++;

                QuizQuestion q3 = new QuizQuestion();
                q3.setLesson(lesson);
                q3.setQuestion("How do you say goodbye?");
                q3.setOptionA("Hello");
                q3.setOptionB("Hi");
                q3.setOptionC("Goodbye");
                q3.setOptionD("Good morning");
                q3.setCorrectOption("C");
                q3.setExplanation("Goodbye có nghĩa là tạm biệt");
                quizQuestionRepository.save(q3);
                questionsCreated++;
            }
            // Quiz questions cho Numbers
            else if (lessonTitle.contains("number") || lessonTitle.contains("số")) {
                QuizQuestion q1 = new QuizQuestion();
                q1.setLesson(lesson);
                q1.setQuestion("What is the English word for 'Một'?");
                q1.setOptionA("Two");
                q1.setOptionB("One");
                q1.setOptionC("Three");
                q1.setOptionD("Four");
                q1.setCorrectOption("B");
                q1.setExplanation("One có nghĩa là một");
                quizQuestionRepository.save(q1);
                questionsCreated++;

                QuizQuestion q2 = new QuizQuestion();
                q2.setLesson(lesson);
                q2.setQuestion("What is the English word for 'Hai'?");
                q2.setOptionA("One");
                q2.setOptionB("Two");
                q2.setOptionC("Three");
                q2.setOptionD("Four");
                q2.setCorrectOption("B");
                q2.setExplanation("Two có nghĩa là hai");
                quizQuestionRepository.save(q2);
                questionsCreated++;

                QuizQuestion q3 = new QuizQuestion();
                q3.setLesson(lesson);
                q3.setQuestion("What is the English word for 'Ba'?");
                q3.setOptionA("Two");
                q3.setOptionB("One");
                q3.setOptionC("Three");
                q3.setOptionD("Four");
                q3.setCorrectOption("C");
                q3.setExplanation("Three có nghĩa là ba");
                quizQuestionRepository.save(q3);
                questionsCreated++;
            }
            // Quiz questions cho Colors
            else if (lessonTitle.contains("color") || lessonTitle.contains("màu")) {
                QuizQuestion q1 = new QuizQuestion();
                q1.setLesson(lesson);
                q1.setQuestion("What is the English word for 'Đỏ'?");
                q1.setOptionA("Blue");
                q1.setOptionB("Red");
                q1.setOptionC("Green");
                q1.setOptionD("Yellow");
                q1.setCorrectOption("B");
                q1.setExplanation("Red có nghĩa là đỏ");
                quizQuestionRepository.save(q1);
                questionsCreated++;

                QuizQuestion q2 = new QuizQuestion();
                q2.setLesson(lesson);
                q2.setQuestion("What is the English word for 'Xanh dương'?");
                q2.setOptionA("Red");
                q2.setOptionB("Blue");
                q2.setOptionC("Green");
                q2.setOptionD("Yellow");
                q2.setCorrectOption("B");
                q2.setExplanation("Blue có nghĩa là xanh dương");
                quizQuestionRepository.save(q2);
                questionsCreated++;

                QuizQuestion q3 = new QuizQuestion();
                q3.setLesson(lesson);
                q3.setQuestion("What is the English word for 'Vàng'?");
                q3.setOptionA("Red");
                q3.setOptionB("Blue");
                q3.setOptionC("Green");
                q3.setOptionD("Yellow");
                q3.setCorrectOption("D");
                q3.setExplanation("Yellow có nghĩa là vàng");
                quizQuestionRepository.save(q3);
                questionsCreated++;
            }
            // Quiz questions cho Animals
            else if (lessonTitle.contains("animal") || lessonTitle.contains("động vật")) {
                QuizQuestion q1 = new QuizQuestion();
                q1.setLesson(lesson);
                q1.setQuestion("What is the English word for 'Con mèo'?");
                q1.setOptionA("Dog");
                q1.setOptionB("Cat");
                q1.setOptionC("Bird");
                q1.setOptionD("Fish");
                q1.setCorrectOption("B");
                q1.setExplanation("Cat có nghĩa là con mèo");
                quizQuestionRepository.save(q1);
                questionsCreated++;

                QuizQuestion q2 = new QuizQuestion();
                q2.setLesson(lesson);
                q2.setQuestion("What is the English word for 'Con chó'?");
                q2.setOptionA("Cat");
                q2.setOptionB("Dog");
                q2.setOptionC("Bird");
                q2.setOptionD("Fish");
                q2.setCorrectOption("B");
                q2.setExplanation("Dog có nghĩa là con chó");
                quizQuestionRepository.save(q2);
                questionsCreated++;

                QuizQuestion q3 = new QuizQuestion();
                q3.setLesson(lesson);
                q3.setQuestion("What is the English word for 'Con chim'?");
                q3.setOptionA("Cat");
                q3.setOptionB("Dog");
                q3.setOptionC("Bird");
                q3.setOptionD("Fish");
                q3.setCorrectOption("C");
                q3.setExplanation("Bird có nghĩa là con chim");
                quizQuestionRepository.save(q3);
                questionsCreated++;
            }
            // Quiz questions cho Food
            else if (lessonTitle.contains("food") || lessonTitle.contains("đồ ăn")) {
                QuizQuestion q1 = new QuizQuestion();
                q1.setLesson(lesson);
                q1.setQuestion("What is the English word for 'Quả táo'?");
                q1.setOptionA("Banana");
                q1.setOptionB("Apple");
                q1.setOptionC("Bread");
                q1.setOptionD("Milk");
                q1.setCorrectOption("B");
                q1.setExplanation("Apple có nghĩa là quả táo");
                quizQuestionRepository.save(q1);
                questionsCreated++;

                QuizQuestion q2 = new QuizQuestion();
                q2.setLesson(lesson);
                q2.setQuestion("What is the English word for 'Quả chuối'?");
                q2.setOptionA("Apple");
                q2.setOptionB("Banana");
                q2.setOptionC("Bread");
                q2.setOptionD("Milk");
                q2.setCorrectOption("B");
                q2.setExplanation("Banana có nghĩa là quả chuối");
                quizQuestionRepository.save(q2);
                questionsCreated++;

                QuizQuestion q3 = new QuizQuestion();
                q3.setLesson(lesson);
                q3.setQuestion("What is the English word for 'Bánh mì'?");
                q3.setOptionA("Apple");
                q3.setOptionB("Banana");
                q3.setOptionC("Bread");
                q3.setOptionD("Milk");
                q3.setCorrectOption("C");
                q3.setExplanation("Bread có nghĩa là bánh mì");
                quizQuestionRepository.save(q3);
                questionsCreated++;
            }
            // Quiz questions mặc định cho các lesson khác
            else {
                QuizQuestion q1 = new QuizQuestion();
                q1.setLesson(lesson);
                q1.setQuestion("What did you learn in this lesson?");
                q1.setOptionA("Nothing");
                q1.setOptionB("New vocabulary");
                q1.setOptionC("Grammar rules");
                q1.setOptionD("Both B and C");
                q1.setCorrectOption("D");
                q1.setExplanation("Bạn đã học từ vựng và ngữ pháp mới trong bài học này");
                quizQuestionRepository.save(q1);
                questionsCreated++;

                QuizQuestion q2 = new QuizQuestion();
                q2.setLesson(lesson);
                q2.setQuestion("Did you understand the lesson?");
                q2.setOptionA("No");
                q2.setOptionB("Yes");
                q2.setOptionC("Maybe");
                q2.setOptionD("Not sure");
                q2.setCorrectOption("B");
                q2.setExplanation("Nếu bạn đã học kỹ, bạn sẽ hiểu bài học này");
                quizQuestionRepository.save(q2);
                questionsCreated++;
            }

            if (questionsCreated > 0) {
                totalCreated += questionsCreated;
                System.out.println("  ✅ Đã tạo " + questionsCreated + " câu hỏi quiz cho bài học: " + lesson.getTitle());
            }
        }

        if (totalCreated > 0) {
            System.out.println("  ✅ Tổng cộng đã tạo " + totalCreated + " câu hỏi quiz cho các bài học");
        } else {
            System.out.println("  ℹ️  Tất cả bài học đã có quiz questions");
        }
    }
}

