package n7.facade;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiPocControllerTest {

    private final AiPocController controller = new AiPocController();

    @Test
    void validateAiPipeline_whenBadIsTrue_returnsBadRequestWithError() {
        ResponseEntity<Map<String, Object>> response = controller.validateAiPipeline(true);

        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("bad request", response.getBody().get("error"));
    }

    @Test
    void validateAiPipeline_whenBadIsFalse_returnsOkWithStatusAndTimestamp() {
        ResponseEntity<Map<String, Object>> response = controller.validateAiPipeline(false);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ok", body.get("status"));
        assertNotNull(body.get("timestamp"));

        // Validate timestamp is a valid ISO-8601 string
        String timestamp = (String) body.get("timestamp");
        Instant parsedInstant = Instant.parse(timestamp);
        assertNotNull(parsedInstant);
    }

    @Test
    void validateAiPipeline_whenBadIsNull_returnsOkWithStatusAndTimestamp() {
        ResponseEntity<Map<String, Object>> response = controller.validateAiPipeline(null);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ok", body.get("status"));
        assertNotNull(body.get("timestamp"));

        // Validate timestamp is a valid ISO-8601 string
        String timestamp = (String) body.get("timestamp");
        Instant parsedInstant = Instant.parse(timestamp);
        assertNotNull(parsedInstant);
    }

    @Test
    void validateAiPipeline_whenNoParam_returnsOkWithStatusAndTimestamp() {
        // Call method with no param (simulate null)
        ResponseEntity<Map<String, Object>> response = controller.validateAiPipeline(null);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ok", body.get("status"));
        assertNotNull(body.get("timestamp"));

        String timestamp = (String) body.get("timestamp");
        Instant parsedInstant = Instant.parse(timestamp);
        assertNotNull(parsedInstant);
    }
}