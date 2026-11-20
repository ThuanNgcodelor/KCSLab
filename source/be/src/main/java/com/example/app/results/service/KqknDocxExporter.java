package com.example.app.results.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Fills the LAB's KQKN-DV Word template ({@code templates/kqkn-dv.docx}, made from KQKN-SR-CT001-092026.docx).
 * The template holds {@code {{key}}} placeholders, each inside one text run:
 * <ul>
 *   <li>{@code {{opt:key}}} — the whole paragraph is dropped when the value is empty (e.g. no mooc number);</li>
 *   <li>{@code {{row.*}}} — the table row that contains them is repeated once per result row.</li>
 * </ul>
 */
@Component
public class KqknDocxExporter {
    static final String TEMPLATE = "templates/kqkn-dv.docx";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] render(ReportData data) {
        try (InputStream input = new ClassPathResource(TEMPLATE).getInputStream();
             ZipInputStream zip = new ZipInputStream(input);
             ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream out = new ZipOutputStream(bytes)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                byte[] content = zip.readAllBytes();
                if ("word/document.xml".equals(entry.getName())) {
                    content = fill(new String(content, StandardCharsets.UTF_8), data).getBytes(StandardCharsets.UTF_8);
                }
                out.putNextEntry(new ZipEntry(entry.getName()));
                out.write(content);
                out.closeEntry();
            }
            out.finish();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo file Word phiếu kết quả", exception);
        }
    }

    String fill(String xml, ReportData data) {
        xml = repeatRows(xml, data);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("resultNumber", data.resultNumber() != null ? data.resultNumber() : "(cấp khi phát hành)");
        values.put("reportDate", date(data.reportDate()));
        values.put("customerName", data.customerName());
        values.put("customerAddress", data.customerAddress());
        values.put("sampleName", data.sampleName());
        values.put("orchardCode", data.orchardCode());
        values.put("packingFacility", data.packingFacility());
        values.put("truckNumber", data.truckNumber());
        values.put("moocNumber", data.moocNumber());
        values.put("containerNumber", data.containerNumber());
        values.put("sealNumber", data.sealNumber());
        values.put("sampleCode", data.sampleCode());
        values.put("sampleCollector", data.sampleCollector());
        values.put("samplingDate", date(data.samplingDate()));
        values.put("sampleCondition", data.sampleCondition());
        values.put("sampleQuantity", data.sampleQuantity());
        values.put("dateReceived", date(data.dateReceived()));
        values.put("testingDate", date(data.testingDate()));
        values.put("qualityHeadName", data.qualityHeadName());
        values.put("directorName", data.directorName());
        for (var value : values.entrySet()) {
            String optional = "{{opt:" + value.getKey() + "}}";
            if (blank(value.getValue())) xml = dropParagraphs(xml, optional);
            xml = xml.replace(optional, escape(value.getValue())).replace("{{" + value.getKey() + "}}", escape(value.getValue()));
        }
        return xml;
    }

    private String repeatRows(String xml, ReportData data) {
        int marker = xml.indexOf("{{row.no}}");
        if (marker < 0) return xml;
        int start = Math.max(xml.lastIndexOf("<w:tr>", marker), xml.lastIndexOf("<w:tr ", marker));
        int end = xml.indexOf("</w:tr>", marker) + "</w:tr>".length();
        String template = xml.substring(start, end);
        StringBuilder rows = new StringBuilder();
        int number = 1;
        for (ReportData.Row row : data.rows()) {
            rows.append(template.replace("{{row.no}}", String.valueOf(number++))
                    .replace("{{row.parameter}}", escape(row.parameter()))
                    .replace("{{row.unit}}", escape(row.unit()))
                    .replace("{{row.method}}", escape(row.method()))
                    .replace("{{row.result}}", escape(row.result()))
                    .replace("{{row.note}}", escape(row.note())));
        }
        return xml.substring(0, start) + rows + xml.substring(end);
    }

    private static String dropParagraphs(String xml, String placeholder) {
        int marker;
        while ((marker = xml.indexOf(placeholder)) >= 0) {
            int start = Math.max(xml.lastIndexOf("<w:p>", marker), xml.lastIndexOf("<w:p ", marker));
            int end = xml.indexOf("</w:p>", marker) + "</w:p>".length();
            xml = xml.substring(0, start) + xml.substring(end);
        }
        return xml;
    }

    private static String date(LocalDate value) { return value == null ? "" : value.format(DATE); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\r", "").replace("\n", " ").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
