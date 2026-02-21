package com.docservice.docservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docservice.docservice.service.PdfOcrCliService;

@RestController
public class PdfOcrCliController {

    private final PdfOcrCliService service;

    public PdfOcrCliController(PdfOcrCliService service) {
        this.service = service;
    }

    @PostMapping("/convert-ocr2")
    public String convertOcr2(@RequestParam("file") MultipartFile file) throws Exception {
        String text = service.ocrPdf(file.getInputStream());

        if (text == null || text.isBlank()) {
            return "# OCR FAILED\n\nNo text recognized.";
        }

        return text;
    }
}
