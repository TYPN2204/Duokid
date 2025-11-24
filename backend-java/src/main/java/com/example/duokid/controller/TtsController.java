package com.example.duokid.controller;

import com.example.duokid.service.AiPythonClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TtsController {

    private final AiPythonClient aiPythonClient;

    public TtsController(AiPythonClient aiPythonClient) {
        this.aiPythonClient = aiPythonClient;
    }

    @GetMapping("/tts")
    public ResponseEntity<Map<String, String>> tts(@RequestParam String text) {
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
        }
        String audioUrl = aiPythonClient.getTtsAudioUrl(text);
        if (audioUrl == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Không tạo được audio lúc này."));
        }
        return ResponseEntity.ok(Map.of("audioUrl", audioUrl));
    }
}

