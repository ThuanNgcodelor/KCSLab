package com.example.app.intake.service;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Customer;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.entity.SealStatus;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptForm3ExporterTest {
    private final ReceiptForm3Exporter exporter = new ReceiptForm3Exporter();

    @Test
    void exportsDocxWithWellFormedDocumentXmlAndEscapedValues() throws Exception {
        User receivedBy = new User("receptionist@test.local", "Người nhận & KCS", "unused", Role.RECEPTIONIST);
        Receipt receipt = new Receipt(
                "TN-20261116-TEST", new Customer("Công ty A & B", "0900000000", null, null), receivedBy,
                "Đơn vị giao <mẫu>", Instant.parse("2026-11-16T03:30:00Z"),
                "Mẫu & <đặc biệt>", "1 kg", "Túi PE", "SEAL-1", true,
                "Nguyên vẹn", "Không có & bất thường", "Xe lạnh <01>", 18.5,
                "Lạnh", "Cadimi & Auramin O", LocalDate.of(2026, 11, 20),
                "Ghi chú <khẩn>", AcceptanceDecision.ACCEPTED, null);
        receipt.setRequestNumber("REQ-001");
        receipt.setSealStatus(SealStatus.INTACT);
        receipt.addSample(new Sample(
                "CT001-SR-16112026-001", "CT001", "SR", LocalDate.of(2026, 11, 16), 1,
                "Mẫu & <đặc biệt>", "1 kg", "Túi PE", "Nguyên vẹn", "Cadimi & Auramin O"));

        byte[] documentXml = readDocumentXml(exporter.export(receipt));
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(documentXml));
        String text = document.getDocumentElement().getTextContent();

        assertThat(text).contains("TN-20261116-TEST", "REQ-001", "Đơn vị giao <mẫu>", "Người nhận & KCS");
        assertThat(new String(documentXml, StandardCharsets.UTF_8)).doesNotContain("{{", "d5p1:", "d8p1:");
    }

    private byte[] readDocumentXml(byte[] docx) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            var entry = zip.getNextEntry();
            while (entry != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    zip.transferTo(output);
                    return output.toByteArray();
                }
                entry = zip.getNextEntry();
            }
        }
        throw new AssertionError("Không tìm thấy word/document.xml trong file DOCX");
    }
}
