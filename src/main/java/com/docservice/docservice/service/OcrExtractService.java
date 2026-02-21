package com.docservice.docservice.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrExtractService {

    private final String tesseractPath = "tesseract";

    public String extract(MultipartFile file) throws Exception {
        return extract(
                file.getBytes(),
                Optional.ofNullable(file.getOriginalFilename()).orElse("image.png")
        );
    }

    public String extract(byte[] bytes, String nameHint) throws Exception {

        // 🔥 EXTENSION FIX
        String ext = ".png";
        if (nameHint.contains(".")) {
            ext = nameHint.substring(nameHint.lastIndexOf("."));
        }

        Path img = Files.createTempFile("ocr-", ext);
        Files.write(img, bytes);

        Path outBase = Files.createTempFile("ocr-out-", "");
        String outBaseStr = outBase.toAbsolutePath().toString();

        List<String> cmd = List.of(
                tesseractPath,
                img.toAbsolutePath().toString(),
                outBaseStr,
                "-l", "kaz+rus+eng"
        );

        try {
            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            String log = new String(p.getInputStream().readAllBytes());
            int code = p.waitFor();

            if (code != 0) {
                throw new RuntimeException("Tesseract failed:\n" + log);
            }

            Path txt = Path.of(outBaseStr + ".txt");
            String text = Files.exists(txt) ? Files.readString(txt) : "";

            return text.trim();

        } finally {
            Files.deleteIfExists(img);
            Files.deleteIfExists(Path.of(outBaseStr + ".txt"));
        }
    }
}