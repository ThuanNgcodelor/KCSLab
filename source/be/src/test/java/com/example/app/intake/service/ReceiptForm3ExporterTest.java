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

    @Test
    void ticksTheCheckboxesMatchingTheStoredValuesAndFillsTheBlanks() throws Exception {
        User receivedBy = new User("receptionist@test.local", "Người nhận", "unused", Role.RECEPTIONIST);
        Receipt receipt = new Receipt(
                "TN-20261119-TICK", new Customer("Công ty A", "0900000001", null, null), receivedBy,
                "Người giao", Instant.parse("2026-11-19T02:05:00Z"),
                "Sầu riêng", "2 kg", "Hộp/thùng", "SEAL-9", false,
                "Nguyên vẹn", "Mô tả", "Xe tải", 4.0,
                "Lạnh", "Cadimi (Cd), Dư lượng thuốc BVTV", LocalDate.of(2026, 11, 25),
                "Không", AcceptanceDecision.ACCEPTED_WITH_CONDITION, "Thiếu ảnh");
        receipt.setRequestNumber("CFC26.0001");
        receipt.setSealStatus(SealStatus.OPENED);
        receipt.setStorageAfterReceipt("Bảo quản 4 °C");
        receipt.addSample(new Sample(
                "CT001-SR-19112026-001", "CT001", "SR", LocalDate.of(2026, 11, 19), 1,
                "Sầu riêng Ri6", "2 kg", "Hộp/thùng", "Rò rỉ; Dấu hiệu bất thường: mốc",
                "Cadimi (Cd), Auramine O (Vàng ô), Khác: Dư lượng thuốc BVTV"));

        String xml = new String(readDocumentXml(exporter.export(receipt)), StandardCharsets.UTF_8);
        java.util.List<String> checked = new java.util.ArrayList<>();
        var matcher = java.util.regex.Pattern.compile(
                "<w14:checked w14:val=\"1\" />.*?</w:sdt><w:r><w:t xml:space=\"preserve\">([^<]*)</w:t>",
                java.util.regex.Pattern.DOTALL).matcher(xml);
        while (matcher.find()) checked.add(matcher.group(1).trim());
        assertThat(checked).containsExactlyInAnyOrder("Hộp/ thùng", "Không có/ đã mở", "Rò rỉ", "Dấu hiệu bất thường:",
                "Lạnh", "Cadimi (Cd)", "Auramin O (Vàng Ô)", "Khác:", "Chấp nhận có điều kiện");

        String text = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).getDocumentElement().getTextContent();
        assertThat(text).contains("CFC26.0001", "TN-20261119-TEST".replace("TEST", "TICK"), "CT001-SR-19112026-001",
                "Sầu riêng Ri6", "2 kg", "mốc", "Dư lượng thuốc BVTV", "Bảo quản 4 °C", "4 °C", "09 giờ 05 phút",
                "ngày 25 / 11 / 2026", "ngày 19 / 11 / 2026");
        assertThat(text).doesNotContain("{{", "INTACT", "OPENED", "Chưa có phê duyệt");
        assertThat(text.split("Xe tải", -1)).hasSize(2); // transport text appears once, not also in the heading
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
