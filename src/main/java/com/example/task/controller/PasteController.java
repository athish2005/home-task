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

@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:5173",
        "https://your-frontend-url.vercel.app"
})
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
                "url", baseUrl + "/p/" + paste.getId()));
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

        Integer remainingViews = paste.getMaxViews() == null
                ? null
                : paste.getMaxViews() - paste.getViewCount();

        String safeContent = paste.getContent()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        String viewsText = remainingViews == null
                ? "∞ Unlimited views"
                : remainingViews + " views remaining";

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Paste Viewer</title>
                    <style>
                        body {
                            margin: 0;
                            padding: 0;
                            background: #0f172a;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
                            color: #e5e7eb;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                        }

                        .container {
                            width: 90%%;
                            max-width: 900px;
                            background: #020617;
                            border-radius: 12px;
                            box-shadow: 0 20px 40px rgba(0,0,0,0.4);
                            overflow: hidden;
                        }

                        .header {
                            padding: 16px 20px;
                            background: linear-gradient(135deg, #2563eb, #06b6d4);
                            color: white;
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            font-size: 16px;
                            font-weight: 600;
                        }

                        .badge {
                            background: rgba(0,0,0,0.25);
                            padding: 6px 12px;
                            border-radius: 999px;
                            font-size: 13px;
                            font-weight: 500;
                        }

                        .content {
                            padding: 20px;
                        }

                        pre {
                            margin: 0;
                            padding: 16px;
                            background: #020617;
                            border: 1px solid #1e293b;
                            border-radius: 8px;
                            font-family: "JetBrains Mono", Consolas, monospace;
                            font-size: 14px;
                            line-height: 1.6;
                            color: #e5e7eb;
                            overflow-x: auto;
                            white-space: pre-wrap;
                            word-break: break-word;
                        }

                        .footer {
                            padding: 12px 20px;
                            font-size: 12px;
                            text-align: right;
                            color: #94a3b8;
                            border-top: 1px solid #1e293b;
                            background: #020617;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <span>📄 Shared Paste</span>
                            <span class="badge">%s</span>
                        </div>

                        <div class="content">
                            <pre>%s</pre>
                        </div>

                        <div class="footer">
                            Paste Service • Secure & Temporary
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(viewsText, safeContent);

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    @RequestMapping(value = "/api/pastes", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok().build();
    }

}
