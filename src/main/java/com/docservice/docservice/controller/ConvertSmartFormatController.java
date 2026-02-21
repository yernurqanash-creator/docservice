package com.docservice.docservice.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.docservice.docservice.service.PostProcessService;
import com.docservice.docservice.service.UniversalTextExtractService;

import java.util.*;

@RestController
public class ConvertSmartFormatController {

    private final PostProcessService pp;
    private final UniversalTextExtractService universal;

    public ConvertSmartFormatController(PostProcessService pp,
                                        UniversalTextExtractService universal) {
        this.pp = pp;
        this.universal = universal;
    }

    // ==========================
    // MARKDOWN ENDPOINT
    // ==========================
   @PostMapping(
        value = "/api/v1/convert-smart-md",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "text/markdown; charset=utf-8"
    )
    public String toMarkdown(@RequestParam("file") MultipartFile file) throws Exception {

        String raw = universal.extractToText(file);
        return pp.toMarkdown(pp.cleanText(raw));
    }

    // ==========================
    // JSON ENDPOINT
    // ==========================
   @PostMapping(
        value = "/api/v1/convert-smart-json",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Object toJson(@RequestParam("file") MultipartFile file) throws Exception {

        String raw = universal.extractToText(file);
        String cleaned = pp.cleanText(raw);
        String md = pp.toMarkdown(cleaned);

        var chunksText = pp.chunkByChars(md, 1500);
        var chunks = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < chunksText.size(); i++) {
            String t = chunksText.get(i);
            chunks.add(Map.of(
                    "index", i,
                    "chars", t.length(),
                    "text", t
            ));
        }

        return Map.of(
                "format", "markdown",
                "meta", Map.of(
                        "filename", file.getOriginalFilename(),
                        "contentType", file.getContentType(),
                        "sizeBytes", file.getSize(),
                        "languageHint", "kaz+rus+eng",
                        "createdAt", java.time.Instant.now().toString()
                ),
                "chunksCount", chunks.size(),
                "chunks", chunks
        );
    }
}