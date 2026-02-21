package com.docservice.docservice.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class SmartExtractService {

    private final ApplicationContext ctx;

    public SmartExtractService(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public String extractSmart(byte[] pdfBytes) {
        // 1) Plain text extractor (Tika plain) -> 2) CLI OCR -> 3) Tika OCR
        String t1 = tryPlainExtract(pdfBytes);
        if (isGood(t1)) return t1;

        String t2 = tryCliOcr(pdfBytes);
        if (isGood(t2)) return t2;

        String t3 = tryOcrExtract(pdfBytes);
        if (isGood(t3)) return t3;

        return "# EMPTY_OUTPUT\n\nNo text recognized by any method.";
    }

    // --------- Heuristics ---------

    private String tryPlainExtract(byte[] bytes) {
        // method: extractText(InputStream)
        List<Object> candidates = findBeansHavingMethod("extractText", InputStream.class);

        // prioritize beans NOT containing "Ocr" in class name (plain extract)
        candidates.sort(Comparator.comparingInt(o -> scorePlain(o.getClass().getSimpleName())));

        for (Object bean : candidates) {
            String out = invokeExtractText(bean, bytes);
            if (isGood(out)) return out;
        }
        return null;
    }

    private String tryOcrExtract(byte[] bytes) {
        List<Object> candidates = findBeansHavingMethod("extractText", InputStream.class);

        // prioritize beans containing "Ocr" in class name
        candidates.sort(Comparator.comparingInt(o -> scoreOcr(o.getClass().getSimpleName())));

        for (Object bean : candidates) {
            String out = invokeExtractText(bean, bytes);
            if (isGood(out)) return out;
        }
        return null;
    }

    private String tryCliOcr(byte[] bytes) {
        // method: ocrPdf(InputStream)
        List<Object> candidates = findBeansHavingMethod("ocrPdf", InputStream.class);

        // prioritize PdfOcrCliService etc
        candidates.sort(Comparator.comparingInt(o -> scoreCli(o.getClass().getSimpleName())));

        for (Object bean : candidates) {
            String out = invokeOcrPdf(bean, bytes);
            if (isGood(out)) return out;
        }
        return null;
    }

    // --------- Reflection helpers ---------

    private List<Object> findBeansHavingMethod(String methodName, Class<?>... params) {
        List<Object> out = new ArrayList<>();
        String[] names = ctx.getBeanDefinitionNames();

        for (String n : names) {
            Object bean;
            try {
                bean = ctx.getBean(n);
            } catch (BeansException e) {
                continue;
            }

            Class<?> c = bean.getClass();
            if (hasMethod(c, methodName, params)) out.add(bean);
        }
        return out;
    }

    private boolean hasMethod(Class<?> c, String name, Class<?>... params) {
        try {
            c.getMethod(name, params);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private String invokeExtractText(Object bean, byte[] bytes) {
        try {
            Method m = bean.getClass().getMethod("extractText", InputStream.class);
            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                Object res = m.invoke(bean, in);
                return res == null ? null : res.toString();
            }
        } catch (IOException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            return null;
        }
    }

    private String invokeOcrPdf(Object bean, byte[] bytes) {
        try {
            Method m = bean.getClass().getMethod("ocrPdf", InputStream.class);
            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                Object res = m.invoke(bean, in);
                return res == null ? null : res.toString();
            }
        } catch (IOException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            return null;
        }
    }

    private boolean isGood(String s) {
        if (s == null) return false;
        String t = s.trim();
        // "# EMPTY_OUTPUT" келсе — good емес
        if (t.startsWith("# EMPTY_OUTPUT")) return false;
        return t.length() >= 30;
    }

    // lower score = higher priority
    private int scorePlain(String name) {
        String n = name.toLowerCase();
        int score = 100;
        if (n.contains("convert")) score -= 40;
        if (n.contains("tika")) score -= 20;
        if (n.contains("extract")) score -= 10;
        if (n.contains("ocr")) score += 50; // plain үшін ocr-ды артқа итереміз
        return score;
    }

    private int scoreOcr(String name) {
        String n = name.toLowerCase();
        int score = 100;
        if (n.contains("ocr")) score -= 50;
        if (n.contains("tika")) score -= 10;
        if (n.contains("convert")) score += 10; // OCR үшін convert-ті сәл артқа
        return score;
    }

    private int scoreCli(String name) {
        String n = name.toLowerCase();
        int score = 100;
        if (n.contains("pdf")) score -= 30;
        if (n.contains("cli")) score -= 30;
        if (n.contains("tesser")) score -= 20;
        if (n.contains("ocr")) score -= 10;
        return score;
    }
}
