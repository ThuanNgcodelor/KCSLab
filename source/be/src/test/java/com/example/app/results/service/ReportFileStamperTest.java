package com.example.app.results.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;


class ReportFileStamperTest {
    private final ReportFileStamper stamper;

    ReportFileStamperTest() throws Exception { stamper = new ReportFileStamper("http://192.168.1.132:5173/tracuu/"); }

    private static byte[] template(boolean withPlaceholder) throws Exception {
        var out = new ByteArrayOutputStream();
        var document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("PHIẾU KẾT QUẢ THỬ NGHIỆM"));
        document.add(new Paragraph("Số/ No.: SR092026-001"));
        PdfContentByte canvas = writer.getDirectContent();
        if (withPlaceholder) {
            var base = com.lowagie.text.pdf.BaseFont.createFont(com.lowagie.text.pdf.BaseFont.HELVETICA, "Cp1252", false);
            canvas.beginText(); canvas.setFontAndSize(base, 7);
            canvas.showTextAligned(com.lowagie.text.Element.ALIGN_CENTER, "Dau/anh ma QR", 500, 770, 0);
            canvas.showTextAligned(com.lowagie.text.Element.ALIGN_CENTER, "khi phat hanh phieu", 500, 761, 0);
            canvas.endText();
        }
        document.close();
        return out.toByteArray();
    }

    @Test
    void readsTheNumberAndStampsScannableQrWithoutPrintingLookupCode() throws Exception {
        byte[] original = template(false);
        var inspection = stamper.inspect(original);
        assertThat(inspection.documentNumber()).isEqualTo("SR092026-001");
        assertThat(inspection.pages()).isEqualTo(1);
        assertThat(inspection.slot().placeholderFound()).isFalse();

        byte[] stamped = stamper.stamp(original, "ABCDEFGHJKLMNPQR", false);
        assertThat(stamped).isNotEqualTo(original);
        var reader = new com.lowagie.text.pdf.PdfReader(stamped);
        assertThat(reader.getNumberOfPages()).isEqualTo(2);
        // The first page is left intact; the QR is on a separate page.
        String text = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text).contains("SR092026-001");
        String lookupPage = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(2);
        assertThat(lookupPage).doesNotContain("Mã tra cứu", "ABCD-EFGH-JKLM-NPQR", "ABCDEFGHJKLMNPQR");
        reader.close();

        try (var pdf = Loader.loadPDF(stamped)) {
            var resources = pdf.getPage(1).getResources();
            BufferedImage qr = null;
            for (var name : resources.getXObjectNames()) {
                if (resources.getXObject(name) instanceof PDImageXObject image) qr = image.getImage();
            }
            assertThat(qr).isNotNull();
            int[] pixels = qr.getRGB(0, 0, qr.getWidth(), qr.getHeight(), null, 0, qr.getWidth());
            var bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(qr.getWidth(), qr.getHeight(), pixels)));
            assertThat(new MultiFormatReader().decode(bitmap).getText())
                    .isEqualTo("http://192.168.1.132:5173/tracuu/?code=ABCDEFGHJKLMNPQR");
        }
    }

    @Test
    void usesTheTemplatePlaceholderWhenPresentAndMarksPreviews() throws Exception {
        byte[] original = template(true);
        var slot = stamper.inspect(original).slot();
        assertThat(slot.placeholderFound()).isTrue();
        assertThat(slot.centerX()).isBetween(470f, 530f);
        byte[] preview = stamper.stamp(original, null, true);
        String text = new com.lowagie.text.pdf.parser.PdfTextExtractor(new com.lowagie.text.pdf.PdfReader(preview)).getTextFromPage(1);
        assertThat(text).contains("in khi phát hành");
    }

    @Test
    void mentioningQrInContentIsNotAStampingPlaceholder() throws Exception {
        var out = new ByteArrayOutputStream();
        var document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("Ket qua: ma QR duoc ghi nhan tai day"));
        document.close();

        var original = out.toByteArray();
        assertThat(stamper.inspect(original).slot().placeholderFound()).isFalse();
        var stamped = new com.lowagie.text.pdf.PdfReader(stamper.stamp(original, "ABCDEFGHJKLMNPQR", false));
        assertThat(stamped.getNumberOfPages()).isEqualTo(2);
        assertThat(new com.lowagie.text.pdf.parser.PdfTextExtractor(stamped).getTextFromPage(1))
                .contains("Ket qua: ma QR duoc ghi nhan tai day");
    }

    @Test
    void rejectsGarbage() {
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> stamper.inspect("%PDF-1.4 nope".getBytes()));
    }

    /** Optional: run against the real converted LAB template when its path is given (never committed). */
    @Test
    void realTemplateIfProvided() throws Exception {
        String path = System.getenv("KQKN_REAL_PDF");
        org.junit.jupiter.api.Assumptions.assumeTrue(path != null && Files.exists(Path.of(path)));
        byte[] pdf = Files.readAllBytes(Path.of(path));
        var inspection = stamper.inspect(pdf);
        System.out.println("REAL number=" + inspection.documentNumber() + " slot=" + inspection.slot());
        assertThat(inspection.documentNumber()).isEqualTo("SR092026-001");
        assertThat(inspection.slot().placeholderFound()).isTrue();
        Files.write(Path.of(path + ".stamped.pdf"), stamper.stamp(pdf, "ABCDEFGHJKLMNPQR", false));
    }
}
