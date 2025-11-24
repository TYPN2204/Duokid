package com.example.duokid.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
public class AiPythonClient {

    private final WebClient webClient;

    public AiPythonClient(
            @Value("${python.service.base-url:http://localhost:8000}")
            String baseUrl
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    private record SuggestRequest(String vietnamese, String topic, String level) {}
    private record SuggestResponse(String sentence) {}
    private record TtsRequest(String text) {}
    private record TtsResponse(String audio_url) {}
    private record GradeRequest(String question, String expected, String answer) {}
    private record GradeResponse(Integer score, String commentEn, String commentVi) {}

    public record GradeResult(int score, String commentEn, String commentVi) {}

    public String suggestSentence(String vietnamese, String topic, String level) {
        try {
            SuggestRequest req = new SuggestRequest(vietnamese, topic, level);
            SuggestResponse res = webClient.post()
                    .uri("/suggest")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(SuggestResponse.class)
                    .block(Duration.ofSeconds(5));
            if (res == null || res.sentence() == null) {
                return "AI hiện chưa đưa ra gợi ý. Hãy thử lại sau.";
            }
            return res.sentence();
        } catch (Exception e) {
            return "Không kết nối được tới AI service. Kiểm tra lại Python server.";
        }
    }

    public String getTtsAudioUrl(String text) {
        try {
            TtsRequest req = new TtsRequest(text);
            TtsResponse res = webClient.post()
                    .uri("/tts")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(TtsResponse.class)
                    .block(Duration.ofSeconds(10));
            if (res == null || res.audio_url() == null) {
                return null;
            }
            return res.audio_url();
        } catch (Exception e) {
            return null;
        }
    }

    public GradeResult gradeAnswer(String question, String expected, String answer) {
        try {
            GradeRequest req = new GradeRequest(question, expected, answer);
            GradeResponse res = webClient.post()
                    .uri("/grade")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(GradeResponse.class)
                    .block(Duration.ofSeconds(5));
            if (res == null || res.score() == null) {
                return new GradeResult(0, "AI chưa chấm được câu này.", "AI chưa chấm được câu này.");
            }
            return new GradeResult(
                    res.score(),
                    res.commentEn() != null ? res.commentEn() : "",
                    res.commentVi() != null ? res.commentVi() : ""
            );
        } catch (Exception e) {
            return new GradeResult(
                    0,
                    "Cannot reach AI grader right now.",
                    "Không kết nối được với AI chấm điểm."
            );
        }
    }
}
