package com.example.app.intake.service;

import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.Sample;
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
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Fills the supplied Word template while preserving all other template parts. */
@Component
public class ReceiptForm3Exporter {
    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT)
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public byte[] export(Receipt receipt) {
        try (InputStream input = new ClassPathResource("templates/phieu-tiep-nhan-ban-giao-mau.docx").getInputStream();
             ZipInputStream zip = new ZipInputStream(input);
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream result = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                result.putNextEntry(new ZipEntry(entry.getName()));
                byte[] content = zip.readAllBytes();
                if ("word/document.xml".equals(entry.getName())) content = fillDocument(content, receipt);
                result.write(content);
                result.closeEntry();
            }
            result.finish();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể xuất Phiếu tiếp nhận và bàn giao mẫu", exception);
        }
    }

    private byte[] fillDocument(byte[] source, Receipt receipt) throws Exception {
        Sample sample = receipt.getSamples().isEmpty() ? null : receipt.getSamples().get(0);
        String xml = new String(source, StandardCharsets.UTF_8);
        xml = xml.replace("{{receiptCode}}", value(receipt.getReceiptCode()))
                .replace("{{requestNumber}}", value(receipt.getRequestNumber()))
                .replace("{{sampleCode}}", sample == null ? "" : value(sample.getSampleCode()))
                .replace("{{receivedAt}}", DATE_TIME.format(receipt.getReceivedAt()))
                .replace("{{deliveredBy}}", value(receipt.getDeliveredBy()))
                .replace("{{receivedBy}}", value(receipt.getReceivedBy().getDisplayName()))
                .replace("{{sampleName}}", sample == null ? value(receipt.getSampleName()) : value(sample.getName()))
                .replace("{{sealNumber}}", value(receipt.getSealNumber()))
                .replace("{{sealStatus}}", value(receipt.getSealStatus()))
                .replace("{{conditionDetail}}", value(receipt.getConditionDetail()))
                .replace("{{transportInfo}}", value(receipt.getTransportInfo()))
                .replace("{{temperature}}", receipt.getReceivedTemperature() == null ? "" : receipt.getReceivedTemperature() + " °C")
                .replace("{{storageCondition}}", value(receipt.getStorageCondition()))
                .replace("{{requestedParameters}}", sample == null ? value(receipt.getRequestedParameters()) : value(sample.getRequestedParameters()))
                .replace("{{specialRequest}}", value(receipt.getSpecialRequest()))
                .replace("{{acceptanceReason}}", value(receipt.getAcceptanceReason()))
                .replace("{{approvedBy}}", "Chưa có phê duyệt độc lập");
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private String value(Object value) { return value == null ? "" : value.toString(); }
}
