package com.docservice.docservice.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiDocsController {


    @GetMapping("/health")
public Map<String, Object> health() {
    return Map.of(
            "status", "UP",
            "service", "docservice",
            "timestamp", Instant.now().toString()
    );
}


    @GetMapping("/api")
    public Map<String, Object> Api() {
        return Map.of(
                "service", "DocService",
                "version", "1.0",
                "description", "AI-ready document to Markdown/JSON chunks converter",
                "timestamp", Instant.now().toString(),

                "endpoints", List.of(
                        Map.of(
                                "method", "POST",
                                "path", "/convert-smart-json",
                                "consumes", "multipart/form-data",
                                "returns", "JSON (chunks)",
                                "description", "PDF / DOCX / PNG / JPG / ZIP → JSON chunks"
                        ),
                        Map.of(
                                "method", "POST",
                                "path", "/convert-smart-md",
                                "consumes", "multipart/form-data",
                                "returns", "text/markdown",
                                "description", "PDF / DOCX / PNG / JPG / ZIP → Markdown"
                        ),
                        Map.of(
                                "method", "GET",
                                "path", "/health",
                                "returns", "200 OK",
                                "description", "Health check"
                        )
                )
        );
    }

    @GetMapping("/docs")
    public String docs() {
        return """
                <html>
                <head>
                  <title>DocService API</title>
                  <style>
                    body { font-family: sans-serif; background:#0f172a; color:#e2e8f0; padding:40px; }
                    h1 { color:#a78bfa; }
                    code { background:#1e293b; padding:4px 8px; border-radius:6px; }
                    .card { background:#1e293b; padding:20px; margin:15px 0; border-radius:12px; }
                  </style>
                </head>
                <body>
                  <h1>DocService API</h1>

                  <div class="card">
                    <h3>POST /convert-smart-json</h3>
                    <p>Upload file → get JSON chunks</p>
                    <code>multipart/form-data (file)</code>
                  </div>

                  <div class="card">
                    <h3>POST /convert-smart-md</h3>
                    <p>Upload file → get Markdown</p>
                    <code>multipart/form-data (file)</code>
                  </div>

                  <div class="card">
                    <h3>GET /api</h3>
                    <p>Machine-readable API info</p>
                  </div>

                </body>
                </html>
                """;
    }
}