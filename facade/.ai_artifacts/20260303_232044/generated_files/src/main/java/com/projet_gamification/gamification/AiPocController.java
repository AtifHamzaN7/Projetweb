package com.projet_gamification.gamification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiPocController {

    @GetMapping("/poc")
    public ResponseEntity<Map<String, Object>> validateAiPipeline() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
