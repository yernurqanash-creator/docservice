package com.docservice.docservice.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.docservice.docservice.utill.TextCleaner;

@Service
public class PostProcessService {

    // 1) “таза” мәтін (header/footer, артық бос жол, linewrap жөндеу)
    public String cleanText(String raw) {
        if (raw == null) return "";

        // normalize line endings
        String s = raw.replace("\r\n", "\n").replace("\r", "\n");

        // ===== OCR / PDF GARBAGE CLEAN =====

        // drop noisy OCR lines
        s = s.replaceAll("(?m)^Estimating resolution as \\d+\\s*$", "");
        s = s.replaceAll("(?m)^Page\\s+\\d+\\s*$", "");

        // remove hh.kz / broken OCR urls
        s = s.replaceAll("(?mi)^\\s*https?://\\S*hh\\.[^\\s]+\\s*$", "");
        s = s.replaceAll("(?mi)^\\s*һарв://\\S+\\s*$", "");

        // remove page markers like "1/4", "?print=true 2/4"
        s = s.replaceAll("(?mi)^\\s*\\?print=true\\s*\\d+\\s*/\\s*\\d+\\s*$", "");
        s = s.replaceAll("(?mi)^\\s*\\d+\\s*/\\s*\\d+\\s*$", "");

        // remove repeated CV header (OCR repeats it every page)
        s = s.replaceAll("(?m)^\\s*13\\.02\\.2026,\\s*12:02\\s*Түйіндеме.*$", "");

        // remove generic Page X
        s = s.replaceAll("(?m)^\\s*Page\\s+\\d+\\s*(/\\s*\\d+)?\\s*$", "");

        // remove any leftover pure URLs
        s = s.replaceAll("(?m)^\\s*https?://\\S+\\s*$", "");

        // collapse 3+ blank lines into 2
        s = s.replaceAll("\\n{3,}", "\n\n");

        // ✅ FINAL: Quick-fix cleaner (Kaz/Rus/Eng)
        s = TextCleaner.clean(s);

        return s.trim();
    }

    // 2) Мәтінді markdown сияқты ету (өте жеңіл: bullet-терді нормалау)
    public String toMarkdown(String cleaned) {
        if (cleaned == null || cleaned.isBlank()) return "# EMPTY_OUTPUT\n";

        String s = cleaned;

        // normalize bullets: —, –, • -> -
        s = s.replaceAll("(?m)^\\s*[•●▪–—]\\s+", "- ");

        return s.trim() + "\n";
    }

    // 3) chunking: LLM үшін 1200-1600 символ арасы жақсы
    public List<String> chunkByChars(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String[] paras = text.split("\\n\\n+");
        StringBuilder cur = new StringBuilder();

        for (String p : paras) {
            String para = p.trim();
            if (para.isEmpty()) continue;

            // if a single paragraph is huge, hard-split
            if (para.length() > maxChars) {
                flush(cur, chunks);
                for (int i = 0; i < para.length(); i += maxChars) {
                    chunks.add(para.substring(i, Math.min(i + maxChars, para.length())));
                }
                continue;
            }

            if (cur.length() == 0) {
                cur.append(para);
            } else if (cur.length() + 2 + para.length() <= maxChars) {
                cur.append("\n\n").append(para);
            } else {
                flush(cur, chunks);
                cur.append(para);
            }
        }
        flush(cur, chunks);
        return chunks;
    }

    private void flush(StringBuilder cur, List<String> chunks) {
        if (cur.length() > 0) {
            chunks.add(cur.toString());
            cur.setLength(0);
        }
    }
}