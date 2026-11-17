package com.example.app.results.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Works on a KQKN report delivered as a file (DOCX converted to PDF, or a PDF exported from Word).
 * <ul>
 *   <li>{@link #inspect} reads the "Số/ No." printed on page 1 and finds the template's QR placeholder
 *       ("Dấu/ảnh mã QR khi phát hành phiếu").</li>
 *   <li>{@link #stamp} covers that placeholder with the lookup QR (PUBLIC_LOOKUP_URL + ?code=...) and the lookup code.
 *       Without a placeholder a separate page is added, so report content is not covered.
 *       Previews get a "BẢN XEM TRƯỚC" watermark.</li>
 * </ul>
 * The original file is never modified; publication stores a stamped copy.
 */
@Component
public class ReportFileStamper {
    private static final Pattern NUMBER = Pattern.compile("S[ốo]\\s*/\\s*No\\.?\\s*:?\\s*([A-Za-z0-9][A-Za-z0-9./-]{2,58})");
    private static final float HALF_WIDTH = 40f, HALF_HEIGHT = 34f;

    private final String lookupBaseUrl;
    private final byte[] font;

    public ReportFileStamper(@Value("${app.public.lookup-url:}") String lookupBaseUrl) throws IOException {
        this.lookupBaseUrl = lookupBaseUrl == null ? "" : lookupBaseUrl.trim();
        try (InputStream in = new ClassPathResource("report/fonts/LiberationSans-Regular.ttf").getInputStream()) {
            this.font = in.readAllBytes();
        }
    }

    /** Where the QR goes, in PDF user space of page 1 (origin bottom-left). */
    public record Slot(float centerX, float centerY, boolean placeholderFound) {}
    public record Inspection(String documentNumber, int pages, Slot slot) {}

    public Inspection inspect(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            if (document.isEncrypted()) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "PDF có mật khẩu/mã hóa nên không dùng làm phiếu kết quả được");
            }
            if (document.getNumberOfPages() == 0) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "PDF không có trang nào");
            }
            Chunks chunks = new Chunks();
            chunks.setStartPage(1);
            chunks.setEndPage(1);
            chunks.setSortByPosition(true);
            String text = Normalizer.normalize(chunks.getText(document), Normalizer.Form.NFC);
            Matcher matcher = NUMBER.matcher(text);
            String number = matcher.find() ? matcher.group(1).replaceAll("[./-]+$", "") : null;
            PDRectangle box = document.getPage(0).getMediaBox();
            return new Inspection(number, document.getNumberOfPages(), locate(chunks.found, box));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Không đọc được nội dung PDF của phiếu", exception);
        }
    }

    /** Stamps the QR + lookup code (or, for a preview, a placeholder and a watermark) on a copy of the PDF. */
    public byte[] stamp(byte[] pdf, String lookupCode, boolean preview) {
        Slot slot = inspect(pdf).slot();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFont regular = PDType0Font.load(document, new java.io.ByteArrayInputStream(font), true);
            PDPage first;
            if (slot.placeholderFound()) {
                first = document.getPage(0);
            } else {
                // An arbitrary report may contain important text or a signature at any corner.
                // Do not paint over the source document when there is no explicit placeholder.
                first = new PDPage(PDRectangle.A4);
                document.addPage(first);
                slot = new Slot(PDRectangle.A4.getWidth() / 2, PDRectangle.A4.getHeight() / 2, false);
            }
            float left = slot.centerX() - HALF_WIDTH, bottom = slot.centerY() - HALF_HEIGHT;
            try (PDPageContentStream content = new PDPageContentStream(document, first, PDPageContentStream.AppendMode.APPEND, true, true)) {
                // Cover the dashed placeholder (or give the QR a clean background in the corner).
                content.setNonStrokingColor(Color.WHITE);
                content.addRect(left, bottom, HALF_WIDTH * 2, HALF_HEIGHT * 2);
                content.fill();
                if (preview || lookupCode == null) {
                    content.setStrokingColor(new Color(0x9a, 0xa5, 0x9e));
                    content.setLineDashPattern(new float[]{3, 2}, 0);
                    content.addRect(left + 2, bottom + 2, HALF_WIDTH * 2 - 4, HALF_HEIGHT * 2 - 4);
                    content.stroke();
                    centered(content, regular, 6.5f, "QR + mã tra cứu", slot.centerX(), slot.centerY() + 3);
                    centered(content, regular, 6.5f, "in khi phát hành", slot.centerX(), slot.centerY() - 6);
                } else {
                    float qrSize = HALF_HEIGHT * 2 - 14;
                    BufferedImage qr = qr(lookupCode);
                    if (qr != null) {
                        var image = LosslessFactory.createFromImage(document, qr);
                        content.drawImage(image, slot.centerX() - qrSize / 2, bottom + 11, qrSize, qrSize);
                        centered(content, regular, 5.2f, "Mã tra cứu: " + groups(lookupCode), slot.centerX(), bottom + 4);
                    } else {
                        centered(content, regular, 6.5f, "Mã tra cứu", slot.centerX(), slot.centerY() + 4);
                        centered(content, regular, 6.5f, groups(lookupCode), slot.centerX(), slot.centerY() - 6);
                    }
                }
            }
            if (preview) {
                for (PDPage page : document.getPages()) watermark(document, page, regular);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đóng dấu QR lên phiếu kết quả", exception);
        }
    }

    private static Slot locate(List<Chunk> chunks, PDRectangle page) {
        Chunk marker = null;
        for (Chunk chunk : chunks) {
            String text = Normalizer.normalize(chunk.text, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
            // Only the explicit placeholder is safe to cover; an ordinary mention of
            // “mã QR” in a report must not be interpreted as a blank stamping area.
            if (text.contains("dau/anh ma qr") || text.contains("dan/in ma qr")) { marker = chunk; break; }
        }
        if (marker == null) {
            // Position is not used for stamping: a dedicated page is added instead.
            return new Slot(page.getUpperRightX() - 36 - HALF_WIDTH, page.getUpperRightY() - 36 - HALF_HEIGHT, false);
        }
        float minX = marker.minX, maxX = marker.maxX, top = marker.top, bottom = marker.bottom;
        for (Chunk chunk : chunks) {
            // The second line of the placeholder ("khi phát hành phiếu") sits right below the first.
            boolean below = chunk.top >= marker.bottom - 1 && chunk.top <= marker.bottom + 16;
            boolean aligned = chunk.maxX >= marker.minX - 30 && chunk.minX <= marker.maxX + 30;
            if (chunk != marker && below && aligned) {
                minX = Math.min(minX, chunk.minX); maxX = Math.max(maxX, chunk.maxX); bottom = Math.max(bottom, chunk.bottom);
            }
        }
        float centerX = (minX + maxX) / 2;
        float centerYFromTop = (top + bottom) / 2;
        float centerY = page.getUpperRightY() - centerYFromTop;
        centerX = Math.max(HALF_WIDTH + 4, Math.min(page.getUpperRightX() - HALF_WIDTH - 4, centerX));
        centerY = Math.max(HALF_HEIGHT + 4, Math.min(page.getUpperRightY() - HALF_HEIGHT - 4, centerY));
        return new Slot(centerX, centerY, true);
    }

    private BufferedImage qr(String lookupCode) {
        if (lookupBaseUrl.isBlank()) return null;
        try {
            String url = lookupBaseUrl + (lookupBaseUrl.contains("?") ? "&" : "?") + "code=" + lookupCode;
            var matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 240, 240, Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
            return image;
        } catch (Exception exception) {
            return null; // the lookup code text is still printed
        }
    }

    private static void watermark(PDDocument document, PDPage page, PDFont font) throws IOException {
        PDRectangle box = page.getMediaBox();
        try (PDPageContentStream content = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            PDExtendedGraphicsState state = new PDExtendedGraphicsState();
            state.setNonStrokingAlphaConstant(0.16f);
            content.setGraphicsStateParameters(state);
            content.setNonStrokingColor(new Color(0xc0, 0x39, 0x2b));
            content.beginText();
            content.setFont(font, 54);
            content.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(35), box.getWidth() * 0.16f, box.getHeight() * 0.28f));
            content.showText("BẢN XEM TRƯỚC");
            content.endText();
        }
    }

    private static void centered(PDPageContentStream content, PDFont font, float size, String text, float centerX, float baseline)
            throws IOException {
        float width = font.getStringWidth(text) / 1000 * size;
        content.setNonStrokingColor(Color.BLACK);
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(centerX - width / 2, baseline);
        content.showText(text);
        content.endText();
    }

    static String groups(String code) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < code.length(); i += 4) builder.append(i > 0 ? "-" : "").append(code, i, Math.min(code.length(), i + 4));
        return builder.toString();
    }

    private record Chunk(String text, float minX, float maxX, float top, float bottom) {}

    /** Collects every text run of page 1 with its bounding box (y measured from the top of the page). */
    private static final class Chunks extends PDFTextStripper {
        private final List<Chunk> found = new ArrayList<>();

        @Override
        protected void writeString(String text, List<TextPosition> positions) throws IOException {
            if (!positions.isEmpty()) {
                float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, top = Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
                for (TextPosition position : positions) {
                    minX = Math.min(minX, position.getXDirAdj());
                    maxX = Math.max(maxX, position.getXDirAdj() + position.getWidthDirAdj());
                    top = Math.min(top, position.getYDirAdj() - position.getHeightDir());
                    bottom = Math.max(bottom, position.getYDirAdj());
                }
                found.add(new Chunk(Normalizer.normalize(text, Normalizer.Form.NFC), minX, maxX, top, bottom));
            }
            super.writeString(text, positions);
        }
    }
}
