package com.example.app.results.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class KqknDocxExporterTest {
    private final KqknDocxExporter exporter = new KqknDocxExporter();

    @Test
    void fillsSampleBlockAndRepeatsOneRowPerParameter() throws Exception {
        ReportData data = new ReportData(null, null, LocalDate.of(2026, 11, 20), "Công ty A & B", "Cần Thơ",
                "Sầu riêng tươi", "VN-DTOR-0530", "VN-LDPH-027", "37H06084", null, "WHLU8737418", "025978",
                "SR-CT001-20112026-001", "Nguyễn Văn A", LocalDate.of(2026, 11, 19), "Mẫu nguyên vẹn", "10 trái",
                LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 20),
                List.of(new ReportData.Row("Auramine O (Vỏ)", "µg/kg", "CFC-01:2026 (1)(2)(3)", "KPH", "LOQ = 1,5 µg/kg"),
                        new ReportData.Row("Auramine O (Ruột)", "µg/kg", "CFC-01:2026 (1)(2)(3)", "KPH", "LOQ = 1,5 µg/kg"),
                        new ReportData.Row("Cadimi (Cd)", "µg/kg", "CFC-02:2026 (1)(2)(3)", "< LOQ", "LOQ = 30 µg/kg")),
                "Người ký 1", "Người ký 2", true);

        byte[] docx = exporter.render(data);
        String xml = documentXml(docx);

        assertThat(xml).doesNotContain("{{");
        assertThat(xml).contains("Công ty A &amp; B", "SR-CT001-20112026-001", "19/11/2026", "20/11/2026",
                "Auramine O (Ruột)", "&lt; LOQ", "LOQ = 30 µg/kg", "(cấp khi phát hành)", "Người ký 2");
        assertThat(xml).doesNotContain("Mooc number", "An Thịnh Phát", "Phan Trường Tiền");
        assertThat(xml.split("Auramine O \\(Vỏ\\)", -1)).hasSize(2);
        String out = System.getProperty("kqkn.out");
        if (out != null) Files.write(Path.of(out), docx);
    }

    private static String documentXml(byte[] docx) throws Exception {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().equals("word/document.xml")) return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("document.xml missing");
    }
}
