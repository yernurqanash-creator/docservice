package com.docservice.docservice.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

@Service
public class PdfOcrCliService {

    public String ocrPdf(InputStream pdfStream) throws Exception {

        File tempDir = Files.createTempDirectory("pdfocr_").toFile();
        List<File> images = new ArrayList<>();
        
try (PDDocument doc = PDDocument.load(pdfStream)) {

            PDFRenderer renderer = new PDFRenderer(doc);

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 300, ImageType.RGB);
                File out = new File(tempDir, "page_" + (i + 1) + ".png");
                ImageIO.write(img, "png", out);
                images.add(out);
            }
        }

        StringBuilder allText = new StringBuilder();

        for (File img : images) {
            allText.append(runTesseract(img)).append("\n\n");
        }

        return allText.toString().trim();
    }

    private String runTesseract(File imageFile) throws Exception {

        // tesseract <img> stdout -l rus+eng --psm 1
        ProcessBuilder pb = new ProcessBuilder(
                "tesseract",
                imageFile.getAbsolutePath(),
                "stdout",
                "-l", "rus+eng",
                "--psm", "1"
        );

        pb.redirectErrorStream(true);
        Process p = pb.start();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = p.getInputStream()) {
            is.transferTo(baos);
        }

        int code = p.waitFor();
        String out = baos.toString(StandardCharsets.UTF_8);

        if (code != 0) {
            throw new RuntimeException("Tesseract failed: " + out);
        }

        return out;
    }
}
