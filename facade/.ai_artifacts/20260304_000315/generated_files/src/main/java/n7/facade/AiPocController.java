package n7.facade;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AiPocController {

    @GetMapping("/ai/poc")
    public ResponseEntity<Map<String, Object>> validateAiPipeline(@RequestParam(required = false) Boolean bad) {
        if (Boolean.TRUE.equals(bad)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "bad request");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
