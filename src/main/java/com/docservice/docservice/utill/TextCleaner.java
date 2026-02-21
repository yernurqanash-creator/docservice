package com.docservice.docservice.utill;

public class TextCleaner {

    public static String clean(String text) {
        if (text == null || text.isBlank()) return "";

        String t = text;

        // 1) Unicode normalize
        t = t.replace("“", "\"")
             .replace("”", "\"")
             .replace("‘", "'")
             .replace("’", "'")
             .replace("–", "-")
             .replace("—", "-");

        // 2) OCR mixups (Kaz/Rus friendly)
        // І / I / | / ! / 1 шатасуы: жеке тұрған кезде ғана ауыстырамыз
        t = t.replaceAll("(?<=\\s)[|!](?=\\s)", "І");
        t = t.replaceAll("\\b1\\b", "І");

        // Kazakh combining diacritics artifacts
        t = t.replace("қ̧", "қ");
        t = t.replace("ғ̧", "ғ");
        t = t.replace("н̧", "ң");

        // 3) extra spaces
        t = t.replaceAll("[ \\t]{2,}", " ");
        t = t.replace("\u00A0", " "); // nbsp

        // 4) extra newlines
        t = t.replaceAll("\\n{3,}", "\n\n");

        // 5) paragraph split: newline before capital => new paragraph
        t = t.replaceAll("(?<!\\n)\\n([А-ЯӘӨҰҚҒІA-Z])", "\n\n$1");

        // 6) missing space after punctuation
        t = t.replaceAll("([.!?])([А-ЯӘӨҰҚҒІA-Z])", "$1 $2");

        return t.trim();
    }
}