package com.example.app.intake.service;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.entity.SealStatus;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Fills the supplied Word template (one form per sample: the form carries a unique sample code) and ticks the
 * checkbox content controls that match the stored values, preserving all other template parts.
 */
@Component
public class ReceiptForm3Exporter {
    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT)
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern CHECKBOX = Pattern.compile(
            "(<w:sdt>(?:(?!</w:sdt>).)*?</w:sdt>)(<w:r><w:t xml:space=\"preserve\">)([^<]*)(</w:t></w:r>)", Pattern.DOTALL);
    private static final String UNCHECKED = "<w14:checked w14:val=\"0\" />";
    private static final String CHECKED = "<w14:checked w14:val=\"1\" />";

    public byte[] export(Receipt receipt) { return export(receipt, null); }

    /** @param sample the sample this form is for; the first sample of the receipt when null. */
    public byte[] export(Receipt receipt, Sample sample) {
        Sample chosen = sample != null ? sample : receipt.getSamples().isEmpty() ? null : receipt.getSamples().get(0);
        try (InputStream input = new ClassPathResource("templates/phieu-tiep-nhan-ban-giao-mau.docx").getInputStream();
             ZipInputStream zip = new ZipInputStream(input);
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream result = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                result.putNextEntry(new ZipEntry(entry.getName()));
                byte[] content = zip.readAllBytes();
                if ("word/document.xml".equals(entry.getName())) content = fillDocument(content, receipt, chosen);
                result.write(content);
                result.closeEntry();
            }
            result.finish();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể xuất Phiếu tiếp nhận và bàn giao mẫu", exception);
        }
    }

    private byte[] fillDocument(byte[] source, Receipt receipt, Sample sample) {
        String xml = new String(source, StandardCharsets.UTF_8);
        String packaging = sample == null ? receipt.getPackaging() : sample.getPackaging();
        String condition = sample == null ? receipt.getSampleCondition() : sample.getCondition();
        String parameters = sample == null ? receipt.getRequestedParameters() : sample.getRequestedParameters();

        // Tick the checkbox content controls first (anchors are template labels), then fill the text placeholders.
        Choice pack = packagingChoice(packaging);
        xml = tick(xml, "Packaging", pack.labels());
        xml = tick(xml, "Seal No.", List.of(receipt.getSealStatus() == SealStatus.INTACT ? "cònnguyên" : "khôngcó/đãmở"));
        Condition cond = conditionChoice(condition);
        xml = tick(xml, "Nguyên vẹn", cond.labels());
        Choice temp = modeChoice(receipt.getStorageCondition(), List.of("thường", "lạnh"));
        xml = tick(xml, "Temperature", temp.labels());
        Parameters params = parameterChoice(parameters);
        xml = tick(xml, "Parameters", params.labels());
        AcceptanceDecision decision = receipt.getAcceptanceDecision();
        xml = tick(xml, "Chấp nhận có điều kiện", List.of(decision == AcceptanceDecision.REJECTED ? "từchối"
                : decision == AcceptanceDecision.ACCEPTED_WITH_CONDITION ? "chấpnhậncóđiềukiện" : "chấpnhận"));

        ZonedDateTime received = receipt.getReceivedAt().atZone(ZONE);
        LocalDate expected = receipt.getExpectedDate();
        xml = xml.replace("{{receiptCode}}", xmlValue(receipt.getReceiptCode()))
                .replace("{{requestNumber}}", xmlValue(receipt.getRequestNumber()))
                .replace("{{sampleCode}}", sample == null ? "" : xmlValue(sample.getSampleCode()))
                .replace("{{receivedDay}}", two(received.getDayOfMonth()))
                .replace("{{receivedMonth}}", two(received.getMonthValue()))
                .replace("{{receivedYear}}", String.valueOf(received.getYear()))
                .replace("{{receivedHour}}", two(received.getHour()))
                .replace("{{receivedMinute}}", two(received.getMinute()))
                .replace("{{deliveredBy}}", xmlValue(receipt.getDeliveredBy()))
                .replace("{{receivedBy}}", xmlValue(receipt.getReceivedBy().getDisplayName()))
                .replace("{{sampleName}}", sample == null ? xmlValue(receipt.getSampleName()) : xmlValue(sample.getName()))
                .replace("{{quantity}}", xmlValue(sample == null ? receipt.getQuantity() : sample.getQuantity()))
                .replace("{{packagingOther}}", xmlValue(pack.other()))
                .replace("{{sealNumber}}", xmlValue(receipt.getSealNumber()))
                .replace("{{conditionOther}}", xmlValue(cond.other()))
                .replace("{{conditionDetail}}", xmlValue(receipt.getConditionDetail()))
                .replace("{{transportInfo}}", xmlValue(receipt.getTransportInfo()))
                .replace("{{temperature}}", xmlValue(temperature(receipt.getReceivedTemperature())))
                .replace("{{temperatureOther}}", xmlValue(temp.other()))
                .replace("{{storageAfterReceipt}}", xmlValue(receipt.getStorageAfterReceipt()))
                .replace("{{parameterOther}}", xmlValue(params.other()))
                .replace("{{expectedDay}}", expected == null ? "" : two(expected.getDayOfMonth()))
                .replace("{{expectedMonth}}", expected == null ? "" : two(expected.getMonthValue()))
                .replace("{{expectedYear}}", expected == null ? "" : String.valueOf(expected.getYear()))
                .replace("{{specialRequest}}", xmlValue(receipt.getSpecialRequest()))
                .replace("{{acceptanceReason}}", xmlValue(receipt.getAcceptanceReason()));
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    /** Ticks, inside the paragraph that contains {@code anchor}, every checkbox whose label (normalized) is selected. */
    private String tick(String xml, String anchor, List<String> selectedLabels) {
        int at = xml.indexOf(anchor);
        if (at < 0 || selectedLabels.isEmpty()) return xml;
        int start = Math.max(xml.lastIndexOf("<w:p>", at), xml.lastIndexOf("<w:p ", at));
        int end = xml.indexOf("</w:p>", at) + "</w:p>".length();
        String paragraph = xml.substring(start, end);
        Predicate<String> chosen = label -> selectedLabels.contains(label);
        Matcher matcher = CHECKBOX.matcher(paragraph);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String box = matcher.group(1);
            if (chosen.test(normalize(matcher.group(3)))) box = box.replace(UNCHECKED, CHECKED).replace("☐", "☒");
            matcher.appendReplacement(out, Matcher.quoteReplacement(box + matcher.group(2) + matcher.group(3) + matcher.group(4)));
        }
        matcher.appendTail(out);
        return xml.substring(0, start) + out + xml.substring(end);
    }

    private static String normalize(String label) {
        return java.text.Normalizer.normalize(label, java.text.Normalizer.Form.NFC).replaceAll("\\s+", "").replaceAll(":$", "").toLowerCase(Locale.ROOT);
    }

    private record Choice(List<String> labels, String other) {}
    private record Condition(List<String> labels, String other) {}
    private record Parameters(List<String> labels, String other) {}

    private Choice packagingChoice(String value) {
        if (value == null || value.isBlank()) return new Choice(List.of(), "");
        String normalized = normalize(value);
        if (List.of("túipe", "hộp/thùng", "chai/lọ").contains(normalized)) return new Choice(List.of(normalized), "");
        return new Choice(List.of("khác"), afterColon(value));
    }

    private Choice modeChoice(String value, List<String> known) {
        if (value == null || value.isBlank()) return new Choice(List.of(), "");
        String normalized = normalize(value);
        if (known.contains(normalized)) return new Choice(List.of(normalized), "");
        return new Choice(List.of("khác"), afterColon(value));
    }

    /** Condition checklist is stored as "Rò rỉ; Dập nát; Dấu hiệu bất thường: ..."; legacy free text counts as abnormal. */
    private Condition conditionChoice(String value) {
        List<String> labels = new ArrayList<>();
        String other = "";
        if (value != null) {
            for (String token : value.split(";")) {
                String item = token.trim();
                if (item.isEmpty()) continue;
                String normalized = normalize(item);
                if (List.of("nguyênvẹn", "ròrỉ", "dậpnát", "thiếulượng").contains(normalized)) labels.add(normalized);
                else { if (!labels.contains("dấuhiệubấtthường")) labels.add("dấuhiệubấtthường"); other = afterColon(item); }
            }
        }
        return new Condition(labels, other);
    }

    /** Known parameters tick their box; anything else (e.g. residue, free text) goes into "Khác". */
    private Parameters parameterChoice(String value) {
        List<String> labels = new ArrayList<>();
        List<String> others = new ArrayList<>();
        if (value != null && !value.isBlank()) {
            String rest = value;
            int other = value.indexOf("Khác:");
            if (other >= 0) { rest = value.substring(0, other); others.add(value.substring(other + "Khác:".length()).trim()); }
            for (String token : rest.split(",")) {
                String item = token.trim();
                if (item.isEmpty()) continue;
                String lower = item.toLowerCase(Locale.ROOT);
                if (lower.contains("cadimi") || lower.contains("cadmi")) { if (!labels.contains("cadimi(cd)")) labels.add("cadimi(cd)"); }
                else if (lower.contains("auramin")) { if (!labels.contains("auramino(vàngô)")) labels.add("auramino(vàngô)"); }
                else others.add(item);
            }
        }
        others.removeIf(String::isBlank);
        if (!others.isEmpty()) labels.add("khác");
        return new Parameters(labels, String.join("; ", others));
    }

    private static String afterColon(String value) {
        int colon = value.indexOf(':');
        return (colon >= 0 ? value.substring(colon + 1) : value.equalsIgnoreCase("Khác") ? "" : value).trim();
    }

    private static String temperature(Double value) {
        return value == null ? "" : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String two(int number) { return String.format(Locale.ROOT, "%02d", number); }

    private String xmlValue(Object value) {
        return value(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String value(Object value) { return value == null ? "" : value.toString(); }
}
