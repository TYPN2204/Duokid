package com.example.duokid.service;

import com.example.duokid.model.Lesson;
import com.example.duokid.model.QuizQuestion;
import com.example.duokid.repo.LessonRepository;
import com.example.duokid.repo.QuizQuestionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LessonService {

    private final LessonRepository lessonRepo;
    private final QuizQuestionRepository quizRepo;

    public LessonService(LessonRepository lessonRepo, QuizQuestionRepository quizRepo) {
        this.lessonRepo = lessonRepo;
        this.quizRepo = quizRepo;
    }

    @PostConstruct
    public void initDemoData() {
        try {
            if (lessonRepo.count() > 0) return;
        } catch (Exception e) {
            // Database might not be ready yet, skip initialization
            System.err.println("Warning: Could not initialize demo data: " + e.getMessage());
            return;
        }
        
        try {

        Lesson greetings1 = new Lesson();
        greetings1.setTitle("Greetings 1");
        greetings1.setDescription("Chào hỏi đơn giản cho học sinh lớp 1");
        greetings1.setXpReward(10);
        greetings1.setLevel("GRADE1");
        greetings1.setOrderIndex(0);
        greetings1.setPartName("PHẦN 1, CỬA 1");
        greetings1.setParentId(null);
        greetings1.setContentHtml(
            "<h3>Từ vựng</h3>" +
            "<ul>" +
            "<li><b>hello</b> – xin chào</li>" +
            "<li><b>hi</b> – chào</li>" +
            "<li><b>good morning</b> – chào buổi sáng</li>" +
            "<li><b>good afternoon</b> – chào buổi chiều</li>" +
            "<li><b>good evening</b> – chào buổi tối</li>" +
            "<li><b>goodbye</b> – tạm biệt</li>" +
            "</ul>" +
            "<h3>Câu mẫu</h3>" +
            "<ul>" +
            "<li>Hello, I am Nam.</li>" +
            "<li>Good morning, teacher.</li>" +
            "<li>Goodbye, see you tomorrow.</li>" +
            "</ul>"
        );

        Lesson colors = new Lesson();
        colors.setTitle("Colors");
        colors.setDescription("Màu sắc cơ bản");
        colors.setXpReward(10);
        colors.setLevel("GRADE1");
        colors.setOrderIndex(1);
        colors.setPartName("PHẦN 1, CỬA 2");
        colors.setParentId(greetings1.getId());
        colors.setContentHtml(
            "<h3>Từ vựng</h3>" +
            "<ul>" +
            "<li><b>red</b> – màu đỏ</li>" +
            "<li><b>blue</b> – màu xanh dương</li>" +
            "<li><b>yellow</b> – màu vàng</li>" +
            "<li><b>green</b> – màu xanh lá</li>" +
            "<li><b>black</b> – màu đen</li>" +
            "<li><b>white</b> – màu trắng</li>" +
            "</ul>" +
            "<h3>Câu mẫu</h3>" +
            "<ul>" +
            "<li>The apple is red.</li>" +
            "<li>The sky is blue.</li>" +
            "<li>My bike is green.</li>" +
            "</ul>"
        );

        Lesson numbers = new Lesson();
        numbers.setTitle("Numbers 1–10");
        numbers.setDescription("Đếm số từ 1 đến 10");
        numbers.setXpReward(10);
        numbers.setLevel("GRADE1");
        numbers.setOrderIndex(2);
        numbers.setPartName("PHẦN 1, CỬA 3");
        numbers.setParentId(colors.getId());
        numbers.setContentHtml(
            "<h3>Từ vựng</h3>" +
            "<ul>" +
            "<li><b>one</b> – một</li>" +
            "<li><b>two</b> – hai</li>" +
            "<li><b>three</b> – ba</li>" +
            "<li><b>four</b> – bốn</li>" +
            "<li><b>five</b> – năm</li>" +
            "<li><b>six</b> – sáu</li>" +
            "<li><b>seven</b> – bảy</li>" +
            "<li><b>eight</b> – tám</li>" +
            "<li><b>nine</b> – chín</li>" +
            "<li><b>ten</b> – mười</li>" +
            "</ul>" +
            "<h3>Câu mẫu</h3>" +
            "<ul>" +
            "<li>I have three pens.</li>" +
            "<li>She has five cats.</li>" +
            "<li>There are ten students.</li>" +
            "</ul>"
        );

        Lesson animals = new Lesson();
        animals.setTitle("Animals");
        animals.setDescription("Động vật quen thuộc");
        animals.setXpReward(10);
        animals.setLevel("GRADE2");
        animals.setOrderIndex(3);
        animals.setPartName("PHẦN 2, CỬA 1");
        animals.setParentId(numbers.getId());
        animals.setContentHtml(
            "<h3>Từ vựng</h3>" +
            "<ul>" +
            "<li><b>cat</b> – con mèo</li>" +
            "<li><b>dog</b> – con chó</li>" +
            "<li><b>bird</b> – con chim</li>" +
            "<li><b>fish</b> – con cá</li>" +
            "<li><b>duck</b> – con vịt</li>" +
            "<li><b>cow</b> – con bò</li>" +
            "</ul>" +
            "<h3>Câu mẫu</h3>" +
            "<ul>" +
            "<li>This is a cat.</li>" +
            "<li>I like dogs.</li>" +
            "<li>The cow is big.</li>" +
            "</ul>"
        );

        Lesson family = new Lesson();
        family.setTitle("My family");
        family.setDescription("Từ vựng về gia đình");
        family.setXpReward(10);
        family.setLevel("GRADE2");
        family.setOrderIndex(4);
        family.setPartName("PHẦN 2, CỬA 2");
        family.setParentId(animals.getId());
        family.setContentHtml(
            "<h3>Từ vựng</h3>" +
            "<ul>" +
            "<li><b>father</b> – bố</li>" +
            "<li><b>mother</b> – mẹ</li>" +
            "<li><b>brother</b> – anh/em trai</li>" +
            "<li><b>sister</b> – chị/em gái</li>" +
            "<li><b>grandfather</b> – ông</li>" +
            "<li><b>grandmother</b> – bà</li>" +
            "</ul>" +
            "<h3>Câu mẫu</h3>" +
            "<ul>" +
            "<li>This is my father.</li>" +
            "<li>My mother is a doctor.</li>" +
            "<li>I have one brother and one sister.</li>" +
            "</ul>"
        );

        // Save in order to get IDs
        greetings1 = lessonRepo.save(greetings1);
        colors.setParentId(greetings1.getId());
        colors = lessonRepo.save(colors);
        numbers.setParentId(colors.getId());
        numbers = lessonRepo.save(numbers);
        animals.setParentId(numbers.getId());
        animals = lessonRepo.save(animals);
        family.setParentId(animals.getId());
        family = lessonRepo.save(family);

        if (quizRepo.count() == 0) {
            quizRepo.saveAll(List.of(
                    question(greetings1,
                            "Bạn sẽ nói gì để chào cô giáo vào buổi sáng?",
                            "Good evening, teacher.",
                            "Good morning, teacher.",
                            "Goodbye, teacher.",
                            "Good night, teacher.",
                            "B",
                            "Vào buổi sáng chúng ta dùng cụm \"Good morning\"."),
                    question(greetings1,
                            "\"Xin chào, tớ là Nam\" được viết như thế nào?",
                            "\"Hello, I am Nam.\"",
                            "\"Goodbye, I am Nam.\"",
                            "\"Good evening, I am Nam.\"",
                            "\"Hi, Nam am I.\"",
                            "A",
                            "\"Hello\" là cách chào phổ biến và cấu trúc câu là \"I am ...\"."),
                    question(colors,
                            "Quả táo trong bài là màu gì?",
                            "Blue",
                            "Green",
                            "Red",
                            "Yellow",
                            "C",
                            "\"The apple is red.\" nên đáp án là đỏ."),
                    question(colors,
                            "\"The sky is blue\" có nghĩa là gì?",
                            "Bầu trời màu xanh dương",
                            "Bầu trời màu đỏ",
                            "Bầu trời màu vàng",
                            "Bầu trời màu trắng",
                            "A",
                            "\"Sky\" là bầu trời và \"blue\" là xanh dương."),
                    question(numbers,
                            "Từ nào có nghĩa là số 5?",
                            "five",
                            "seven",
                            "ten",
                            "two",
                            "A",
                            "\"five\" nghĩa là số 5."),
                    question(numbers,
                            "Câu \"There are ten students.\" muốn nói điều gì?",
                            "Có 3 học sinh",
                            "Có 5 học sinh",
                            "Có 8 học sinh",
                            "Có 10 học sinh",
                            "D",
                            "\"Ten\" nghĩa là số 10."),
                    question(animals,
                            "Từ nào nghĩa là \"con mèo\"?",
                            "dog",
                            "cat",
                            "fish",
                            "duck",
                            "B",
                            "\"Cat\" là con mèo."),
                    question(animals,
                            "\"I like dogs.\" có nghĩa là gì?",
                            "Tôi ghét chó",
                            "Tôi sợ chó",
                            "Tôi thích chó",
                            "Tôi có một con chó",
                            "C",
                            "\"Like\" nghĩa là thích."),
                    question(family,
                            "Từ \"mother\" nghĩa là gì?",
                            "Bố",
                            "Mẹ",
                            "Ông",
                            "Anh trai",
                            "B",
                            "\"Mother\" là mẹ."),
                    question(family,
                            "Câu nào mô tả đúng \"I have one brother and one sister\"?",
                            "Tôi có hai anh trai",
                            "Tôi có một anh trai và một chị gái",
                            "Tôi có một anh trai và một em gái",
                            "Tôi có một anh/em trai và một chị/em gái",
                            "D",
                            "\"Brother\" là anh/em trai, \"sister\" là chị/em gái.")
            ));
        }
        } catch (Exception e) {
            System.err.println("Warning: Could not save quiz questions: " + e.getMessage());
        }
    }

    public List<Lesson> findAll() {
        return lessonRepo.findAll();
    }

    public Lesson findById(Long id) {
        return lessonRepo.findById(id).orElse(null);
    }

    public java.util.List<QuizQuestion> getQuestionsByLesson(Lesson lesson) {
        return quizRepo.findByLesson(lesson);
    }

    private QuizQuestion question(
            Lesson lesson,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String correct,
            String explanation
    ) {
        QuizQuestion q = new QuizQuestion();
        q.setLesson(lesson);
        q.setQuestion(questionText);
        q.setOptionA(optionA);
        q.setOptionB(optionB);
        q.setOptionC(optionC);
        q.setOptionD(optionD);
        q.setCorrectOption(correct);
        q.setExplanation(explanation);
        return q;
    }
}
