package com.docservice.docservice.service;

import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.docservice.docservice.controller.ConvertSmartController;

@Service
public class UniversalTextExtractService {

    private final ConvertSmartController pdfSmart;
    private final DocxExtractService docx;
    private final OcrExtractService ocr;

    public UniversalTextExtractService(ConvertSmartController pdfSmart,
                                       DocxExtractService docx,
                                       OcrExtractService ocr) {
        this.pdfSmart = pdfSmart;
        this.docx = docx;
        this.ocr = ocr;
    }

    // ==========================
    // MAIN ENTRY
    // ==========================
    public String extractToText(MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = Optional.ofNullable(file.getOriginalFilename())
                .orElse("")
                .toLowerCase();

        // ===== PDF =====
        if (filename.endsWith(".pdf")) {
            return pdfSmart.convertSmart(file);
        }

        // ===== DOCX =====
        if (filename.endsWith(".docx")) {
            try {
                return docx.extract(file);
            } catch (Exception e) {
                throw new RuntimeException("DOCX extraction failed: " + e.getMessage(), e);
            }
        }

        // ===== IMAGE =====
        if (filename.endsWith(".png")
                || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")) {
            return ocr.extract(file);
        }

        // ===== ZIP =====
        if (filename.endsWith(".zip")) {
            return extractZip(file);
        }

        throw new IllegalArgumentException("Unsupported file type: " + filename);
    }

    // ==========================
    // ZIP SUPPORT
    // ==========================
    private String extractZip(MultipartFile zipFile) throws Exception {

        StringBuilder all = new StringBuilder();

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                if (entry.isDirectory()) continue;

                String entryName = entry.getName().toLowerCase();
                byte[] bytes = zis.readAllBytes();

                all.append("\n\n=== FILE: ")
                   .append(entryName)
                   .append(" ===\n\n");

                try {

                    if (entryName.endsWith(".docx")) {
                        all.append(docx.extract(bytes));
                    }
                    else if (entryName.endsWith(".png")
                            || entryName.endsWith(".jpg")
                            || entryName.endsWith(".jpeg")) {
                        all.append(ocr.extract(bytes, entryName));
                    }
                    else if (entryName.endsWith(".txt")
                            || entryName.endsWith(".md")) {
                        all.append(new String(bytes));
                    }
                    else if (entryName.endsWith(".pdf")) {
                        all.append("[PDF inside ZIP skipped]");
                    }
                    else {
                        all.append("[Unsupported inside ZIP]");
                    }

                } catch (Exception e) {
                    all.append("[ERROR: ")
                       .append(e.getMessage())
                       .append("]");
                }
            }
        }

        return all.toString().trim();
    }
}