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
            Map<String, byte[]> entries = new LinkedHashMap<>();
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) entries.put(entry.getName(), zip.readAllBytes());
            String document = fill(text(entries, "word/document.xml"), data);
            if (!data.photos().isEmpty()) {
                document = appendPhotos(document, data.photos());
                StringBuilder rels = new StringBuilder(text(entries, "word/_rels/document.xml.rels"));
                int close = rels.lastIndexOf("</Relationships>");
                for (int index = 0; index < data.photos().size(); index++) {
                    entries.put("word/media/cfc-photo-" + (index + 1) + ".jpeg", data.photos().get(index).jpeg());
                    rels.insert(close, "<Relationship Id=\"rIdCfcPhoto" + (index + 1) + "\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/cfc-photo-" + (index + 1) + ".jpeg\"/>");
                    close = rels.lastIndexOf("</Relationships>");
                }
                entries.put("word/_rels/document.xml.rels", rels.toString().getBytes(StandardCharsets.UTF_8));
                String types = text(entries, "[Content_Types].xml");
                if (!types.contains("Extension=\"jpeg\"")) {
                    types = types.replace("<Default Extension=\"xml\"", "<Default Extension=\"jpeg\" ContentType=\"image/jpeg\"/><Default Extension=\"xml\"");
                    entries.put("[Content_Types].xml", types.getBytes(StandardCharsets.UTF_8));
                }
            }
            entries.put("word/document.xml", document.getBytes(StandardCharsets.UTF_8));
            for (var item : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(item.getKey()));
                out.write(item.getValue());
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

    private static String text(Map<String, byte[]> entries, String name) {
        byte[] content = entries.get(name);
        if (content == null) throw new IllegalStateException("Mẫu KQKN thiếu " + name);
        return new String(content, StandardCharsets.UTF_8);
    }

    private static final long MAX_WIDTH_EMU = 5_760_000L;  // 16 cm
    private static final long MAX_HEIGHT_EMU = 3_600_000L; // 10 cm: two photos fit on one page

    /** Adds a "Hình ảnh mẫu" page after the report (new page, same header/footer), one photo per paragraph. */
    private String appendPhotos(String xml, java.util.List<ReportData.Photo> photos) {
        int body = xml.lastIndexOf("</w:body>");
        int sect = xml.lastIndexOf("<w:sectPr", body);
        int at = sect >= 0 ? sect : body;
        StringBuilder page = new StringBuilder();
        page.append("<w:p><w:pPr><w:pageBreakBefore/><w:spacing w:after=\"60\"/></w:pPr>")
                .append(run("HÌNH ẢNH MẪU", true, false, 26, "00A859"))
                .append(run("  / Sample photos", false, true, 22, "00A859")).append("</w:p>");
        page.append("<w:p><w:pPr><w:spacing w:after=\"160\"/></w:pPr>")
                .append(run("Ảnh chụp khi tiếp nhận mẫu/ Photos taken when the sample was received", false, true, 18, "555555"))
                .append("</w:p>");
        for (int index = 0; index < photos.size(); index++) {
            ReportData.Photo photo = photos.get(index);
            double scale = Math.min((double) MAX_WIDTH_EMU / photo.width(), (double) MAX_HEIGHT_EMU / photo.height());
            long cx = Math.round(photo.width() * scale), cy = Math.round(photo.height() * scale);
            int id = 9000 + index;
            page.append("<w:p><w:pPr><w:keepNext/><w:spacing w:before=\"120\" w:after=\"40\"/><w:jc w:val=\"center\"/></w:pPr><w:r><w:drawing>")
                    .append("<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\"><wp:extent cx=\"").append(cx).append("\" cy=\"").append(cy).append("\"/>")
                    .append("<wp:docPr id=\"").append(id).append("\" name=\"Ảnh mẫu ").append(index + 1).append("\"/>")
                    .append("<wp:cNvGraphicFramePr><a:graphicFrameLocks xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" noChangeAspect=\"1\"/></wp:cNvGraphicFramePr>")
                    .append("<a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\"><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">")
                    .append("<pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\"><pic:nvPicPr><pic:cNvPr id=\"").append(id)
                    .append("\" name=\"cfc-photo-").append(index + 1).append(".jpeg\"/><pic:cNvPicPr/></pic:nvPicPr>")
                    .append("<pic:blipFill><a:blip r:embed=\"rIdCfcPhoto").append(index + 1).append("\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>")
                    .append("<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"").append(cx).append("\" cy=\"").append(cy)
                    .append("\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>")
                    .append("</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>");
            page.append("<w:p><w:pPr><w:spacing w:after=\"160\"/><w:jc w:val=\"center\"/></w:pPr>")
                    .append(run("Hình " + (index + 1) + ". " + photo.caption(), false, true, 18, "333333")).append("</w:p>");
        }
        return xml.substring(0, at) + page + xml.substring(at);
    }

    private static String run(String text, boolean bold, boolean italic, int halfPoints, String color) {
        return "<w:r><w:rPr><w:rFonts w:ascii=\"Times New Roman\" w:hAnsi=\"Times New Roman\" w:cs=\"Times New Roman\"/>"
                + (bold ? "<w:b/>" : "") + (italic ? "<w:i/>" : "") + "<w:color w:val=\"" + color + "\"/><w:sz w:val=\"" + halfPoints
                + "\"/></w:rPr><w:t xml:space=\"preserve\">" + escape(text) + "</w:t></w:r>";
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
