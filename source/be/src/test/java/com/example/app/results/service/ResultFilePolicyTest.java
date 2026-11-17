package com.example.app.results.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultFilePolicyTest {
    static final String PDF = "application/pdf";
    static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    static byte[] pdf(String body) {
        return ("%PDF-1.4\n" + body + "\ntrailer\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1);
    }

    static byte[] zip(String... namesAndContents) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                zip.putNextEntry(new ZipEntry(namesAndContents[i]));
                zip.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    public static byte[] docx() throws Exception {
        return zip("[Content_Types].xml", "<Types/>", "word/document.xml", "<w:document/>");
    }

    private static int status(Runnable action) {
        try { action.run(); return 200; }
        catch (ResponseStatusException e) { return e.getStatusCode().value(); }
    }

    private static int check(String name, String type, byte[] content) {
        return status(() -> ResultFilePolicy.validate(
                new MockMultipartFile("files", name, type, content)));
    }

    @Test void acceptsPlainPdfDocxAndXlsx() throws Exception {
        assertThat(check("ket-qua.pdf", PDF, pdf("1 0 obj <<>> endobj"))).isEqualTo(200);
        assertThat(check("ket-qua.docx", DOCX, docx())).isEqualTo(200);
        assertThat(check("bang.xlsx", XLSX, zip("[Content_Types].xml", "<Types/>", "xl/workbook.xml", "<w/>"))).isEqualTo(200);
    }

    @Test void rejectsDisallowedExtensionAndMismatchedMime() throws Exception {
        assertThat(check("tool.exe", "application/octet-stream", new byte[]{'M', 'Z', 1, 2})).isEqualTo(415);
        assertThat(check("ket-qua.pdf", "image/png", pdf("x"))).isEqualTo(415);
        assertThat(check("ket-qua.docm", DOCX, docx())).isEqualTo(415);
        assertThat(check("noext", PDF, pdf("x"))).isEqualTo(415);
    }

    @Test void rejectsSpoofedContent() throws Exception {
        assertThat(check("fake.pdf", PDF, "MZ-not-a-pdf".getBytes(StandardCharsets.UTF_8))).isEqualTo(415);
        assertThat(check("fake.docx", DOCX, pdf("x"))).isEqualTo(415);
        assertThat(check("truncated.pdf", PDF, "%PDF-1.4 truncated".getBytes(StandardCharsets.UTF_8))).isEqualTo(415);
    }

    @Test void rejectsActivePdfAndDangerousOoxml() throws Exception {
        assertThat(check("js.pdf", PDF, pdf("<< /S /JavaScript /JS (app.alert(1)) >>"))).isEqualTo(415);
        assertThat(check("macro.docx", DOCX, zip("[Content_Types].xml", "<Types/>", "word/document.xml", "<d/>",
                "word/vbaProject.bin", "x"))).isEqualTo(415);
        assertThat(check("macro2.docx", DOCX, zip("[Content_Types].xml", "<Default ContentType=\"application/vnd.ms-word.document.macroEnabled.main+xml\"/>",
                "word/document.xml", "<d/>"))).isEqualTo(415);
        assertThat(check("ole.docx", DOCX, zip("[Content_Types].xml", "<Types/>", "word/document.xml", "<d/>",
                "word/embeddings/oleObject1.bin", "x"))).isEqualTo(415);
        assertThat(check("tpl.docx", DOCX, zip("[Content_Types].xml", "<Types/>", "word/document.xml", "<d/>",
                "word/_rels/settings.xml.rels", "<Relationship Type=\".../attachedTemplate\" TargetMode=\"External\"/>"))).isEqualTo(415);
        assertThat(check("slip.docx", DOCX, zip("[Content_Types].xml", "<Types/>", "word/document.xml", "<d/>",
                "../evil.txt", "x"))).isEqualTo(415);
        assertThat(check("nomain.docx", DOCX, zip("[Content_Types].xml", "<Types/>"))).isEqualTo(415);
    }

    @Test void rejectsOversizedAndEmptyAndBadNames() throws Exception {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        assertThat(check("big.pdf", PDF, big)).isEqualTo(413);
        assertThat(check("empty.pdf", PDF, new byte[0])).isEqualTo(400);
        assertThat(check("..\\..\\x..pdf", PDF, pdf("x"))).isEqualTo(400);
    }
}
