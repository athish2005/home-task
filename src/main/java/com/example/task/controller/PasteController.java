package com.example.task.controller;



import com.example.task.service.PasteService;
import com.example.task.entity.Paste;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class PasteController {

    private final PasteService pasteService;

    @Value("${TEST_MODE:0}")
    private String testMode;

    @Value("${APP_BASE_URL:http://localhost:8080}")
    private String baseUrl;

    // HEALTH CHECK
    @GetMapping("/api/healthz")
    public ResponseEntity<Map<String, Boolean>> health() {
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // CREATE PASTE
    @PostMapping("/api/pastes")
    public ResponseEntity<?> createPaste(@RequestBody Map<String, Object> body) {

        String content = (String) body.get("content");
        Integer ttl = body.get("ttl_seconds") != null
                ? ((Number) body.get("ttl_seconds")).intValue()
                : null;
        Integer maxViews = body.get("max_views") != null
                ? ((Number) body.get("max_views")).intValue()
                : null;

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "content is required"));
        }

        if (ttl != null && ttl < 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "ttl_seconds must be >= 1"));
        }

        if (maxViews != null && maxViews < 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "max_views must be >= 1"));
        }

        com.example.task.entity.Paste paste = pasteService.createPaste(content, ttl, maxViews);

        return ResponseEntity.ok(Map.of(
                "id", paste.getId(),
                "url", baseUrl + "/p/" + paste.getId()
        ));
    }

    // FETCH PASTE (API)
   @GetMapping("/api/pastes/{id}")
public ResponseEntity<?> getPaste(
        @PathVariable String id,
        @RequestHeader(value = "x-test-now-ms", required = false) Long testNowMs) {

    Instant now;

    if ("1".equals(testMode) && testNowMs != null) {
        now = Instant.ofEpochMilli(testNowMs);
    } else {
        now = Instant.now();
    }

    Optional<Paste> optional = pasteService.fetchPaste(id, now);

    if (optional.isEmpty()) {
        return ResponseEntity.status(404)
                .body(Map.of("error", "Paste not found"));
    }

    Paste paste = optional.get();

    Integer remainingViews = paste.getMaxViews() == null
            ? null
            : Math.max(0, paste.getMaxViews() - paste.getViewCount());

    Map<String, Object> response = new java.util.HashMap<>();
    response.put("content", paste.getContent());
    response.put("remaining_views", remainingViews);
    response.put("expires_at", paste.getExpiresAt());

    return ResponseEntity.ok(response);
}

@GetMapping("/p/{id}")
public ResponseEntity<String> viewPaste(@PathVariable String id) {

    Optional<Paste> optional = pasteService.fetchPaste(id, Instant.now());

    if (optional.isEmpty()) {
        return ResponseEntity.status(404).body("Paste not found");
    }

    Paste paste = optional.get();

    // VERY IMPORTANT: escape HTML to avoid script execution
    String safeContent = paste.getContent()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");

    String html = """
        <html>
        <head><title>Paste</title></head>
        <body>
            <h3>Your Paste</h3>
            <pre>%s</pre>
        </body>
        </html>
        """.formatted(safeContent);

    return ResponseEntity.ok()
            .header("Content-Type", "text/html")
            .body(html);
}


}
