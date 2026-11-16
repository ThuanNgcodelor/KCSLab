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
        xml = xml.replace("{{receiptCode}}", xmlValue(receipt.getReceiptCode()))
                .replace("{{requestNumber}}", xmlValue(receipt.getRequestNumber()))
                .replace("{{sampleCode}}", sample == null ? "" : xmlValue(sample.getSampleCode()))
                .replace("{{receivedAt}}", xmlValue(DATE_TIME.format(receipt.getReceivedAt())))
                .replace("{{deliveredBy}}", xmlValue(receipt.getDeliveredBy()))
                .replace("{{receivedBy}}", xmlValue(receipt.getReceivedBy().getDisplayName()))
                .replace("{{sampleName}}", sample == null ? xmlValue(receipt.getSampleName()) : xmlValue(sample.getName()))
                .replace("{{sealNumber}}", xmlValue(receipt.getSealNumber()))
                .replace("{{sealStatus}}", xmlValue(receipt.getSealStatus()))
                .replace("{{conditionDetail}}", xmlValue(receipt.getConditionDetail()))
                .replace("{{transportInfo}}", xmlValue(receipt.getTransportInfo()))
                .replace("{{temperature}}", xmlValue(receipt.getReceivedTemperature() == null ? "" : receipt.getReceivedTemperature() + " °C"))
                .replace("{{storageCondition}}", xmlValue(receipt.getStorageCondition()))
                .replace("{{requestedParameters}}", sample == null ? xmlValue(receipt.getRequestedParameters()) : xmlValue(sample.getRequestedParameters()))
                .replace("{{specialRequest}}", xmlValue(receipt.getSpecialRequest()))
                .replace("{{acceptanceReason}}", xmlValue(receipt.getAcceptanceReason()))
                .replace("{{approvedBy}}", xmlValue("Chưa có phê duyệt độc lập"));
        return xml.getBytes(StandardCharsets.UTF_8);
    }

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
