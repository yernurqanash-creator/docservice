package com.docservice.docservice.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocxExtractService {

    public String extract(MultipartFile file) throws Exception {
        return extract(file.getBytes());
    }

    public String extract(byte[] bytes) throws Exception {
        try (InputStream in = new ByteArrayInputStream(bytes);
             XWPFDocument doc = new XWPFDocument(in)) {

            StringBuilder sb = new StringBuilder();

            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText();
                if (t != null && !t.isBlank()) sb.append(t).append("\n");
                else sb.append("\n");
            }

            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        sb.append(cell.getText()).append(" | ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }

            return sb.toString().trim();
        }
    }
}