package com.docservice.docservice.service;

import java.io.File;
import java.nio.file.Files;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrService {

    private static final String LANGS = "kaz+rus+eng";

    public String ocrPdf(MultipartFile file) throws Exception {

        File pdf = File.createTempFile("ocr-", ".pdf");
        file.transferTo(pdf);

        File outDir = Files.createTempDirectory("ocr-pages-").toFile();
        String prefix = new File(outDir, "page").getAbsolutePath();

        // 1) PDF -> PNG pages
        // pdftoppm -png -r 300 input.pdf outPrefix
        ProcessBuilder pb1 = new ProcessBuilder(
                "pdftoppm",
                "-png",
                "-r", "300",
                pdf.getAbsolutePath(),
                prefix
        );
        pb1.redirectErrorStream(true);
        Process p1 = pb1.start();
        int code1 = p1.waitFor();

        if (code1 != 0) {
            cleanup(pdf, outDir);
            return "";
        }

        // 2) OCR each page image
        File[] pages = outDir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
        if (pages == null || pages.length == 0) {
            cleanup(pdf, outDir);
            return "";
        }

        // sort pages by name (page-1.png, page-2.png...)
        java.util.Arrays.sort(pages);

        StringBuilder all = new StringBuilder();

        for (File img : pages) {
            File outBase = File.createTempFile("tess-", "");
            outBase.delete();

            ProcessBuilder pb2 = new ProcessBuilder(
                    "tesseract",
                    img.getAbsolutePath(),
                    outBase.getAbsolutePath(),
                    "-l", LANGS
            );

            pb2.redirectErrorStream(true);
            Process p2 = pb2.start();
            p2.waitFor();

            File txt = new File(outBase.getAbsolutePath() + ".txt");
            if (txt.exists()) {
                all.append(Files.readString(txt.toPath())).append("\n\n");
                txt.delete();
            }
        }

        cleanup(pdf, outDir);

        return all.toString().trim();
    }

    private void cleanup(File pdf, File outDir) {
        try { if (pdf != null) pdf.delete(); } catch (Exception ignored) {}
        try {
            if (outDir != null && outDir.exists()) {
                File[] fs = outDir.listFiles();
                if (fs != null) for (File f : fs) f.delete();
                outDir.delete();
            }
        } catch (Exception ignored) {}
    }
}

