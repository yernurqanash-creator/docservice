package com.docservice.docservice;
import java.nio.charset.StandardCharsets;

import org.apache.tika.Tika;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ConvertController {

    private final Tika tika = new Tika();

    @PostMapping("/convert")
    public ResponseEntity<byte[]> convert(@RequestParam("file") MultipartFile file) throws Exception {
        String text = tika.parseToString(file.getInputStream());

        // Егер бос болса — диагноз үшін мета-ақпарат қайтар
        if (text == null || text.trim().isEmpty()) {
            String info =
                    """
                    # EMPTY_OUTPUT
                    filename: """ + file.getOriginalFilename() + "\n" +
                    "contentType: " + file.getContentType() + "\n" +
                    "sizeBytes: " + file.getSize() + "\n" +
                    "\n" +
                    "Tika returned empty text. Likely scanned PDF (image-only) or protected/encrypted PDF.\n";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"result.md\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(info.getBytes(StandardCharsets.UTF_8));
        }

        byte[] output = text.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"result.md\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(output);
    }
}



