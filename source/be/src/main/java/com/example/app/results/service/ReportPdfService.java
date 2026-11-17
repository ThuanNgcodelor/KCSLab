package com.example.app.results.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Renders the bilingual "PHIẾU KẾT QUẢ THỬ NGHIỆM / TEST REPORT" (KQKN-DV) as a PDF following the LAB's template:
 * header with company details and logo, watermark, numbered sample information, results table, two signatories,
 * notes and the BM.QT.14.02 footer with page numbers. Fonts are embedded so Vietnamese renders on any viewer.
 */
@Service
public class ReportPdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color GREY = new Color(0x55, 0x55, 0x55);
    private static final Color LINE = new Color(0x99, 0x99, 0x99);

    private final String lookupBaseUrl;
    private final byte[] logo;
    private final byte[] watermark;
    private final Map<String, byte[]> fonts;

    public ReportPdfService(@Value("${app.public.lookup-url:}") String lookupBaseUrl) throws IOException {
        this.lookupBaseUrl = lookupBaseUrl == null ? "" : lookupBaseUrl.trim();
        this.logo = read("report/logo.png");
        this.watermark = read("report/watermark.png");
        this.fonts = Map.of("r", read("report/fonts/LiberationSans-Regular.ttf"), "b", read("report/fonts/LiberationSans-Bold.ttf"),
                "i", read("report/fonts/LiberationSans-Italic.ttf"), "bi", read("report/fonts/LiberationSans-BoldItalic.ttf"));
    }

    private static byte[] read(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) { return in.readAllBytes(); }
    }

    private BaseFont base(String key) {
        try {
            return BaseFont.createFont("LiberationSans-" + key + ".ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fonts.get(key), null);
        } catch (DocumentException | IOException exception) { throw new IllegalStateException("Không nạp được font báo cáo", exception); }
    }

    public byte[] render(ReportData data) {
        BaseFont regular = base("r"), bold = base("b"), italic = base("i"), boldItalic = base("bi");
        Font body = new Font(regular, 9.5f), bodyBold = new Font(bold, 9.5f), small = new Font(regular, 7.5f);
        Font smallItalic = new Font(italic, 8f), title = new Font(bold, 15f), titleEn = new Font(boldItalic, 11f);
        Font section = new Font(bold, 10.5f), note = new Font(regular, 8f), noteBold = new Font(bold, 8f);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 37f, 37f, 112f, 48f);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPdfVersion(PdfWriter.VERSION_1_7);
            writer.setPageEvent(new PageDecorator(regular, bold, small, data.preview()));
            document.addTitle("Phiếu kết quả thử nghiệm " + (data.resultNumber() == null ? "(bản xem trước)" : data.resultNumber()));
            document.addAuthor("CFC Lab");
            document.open();

            // Title block + QR
            PdfPTable head = new PdfPTable(new float[]{3.2f, 1f});
            head.setWidthPercentage(100);
            PdfPCell left = noBorder();
            Paragraph t = new Paragraph("PHIẾU KẾT QUẢ THỬ NGHIỆM", title); t.setAlignment(Element.ALIGN_CENTER);
            Paragraph te = new Paragraph("TEST REPORT", titleEn); te.setAlignment(Element.ALIGN_CENTER);
            Paragraph no = new Paragraph("Số/ No.: " + (data.resultNumber() == null ? "—" : data.resultNumber()), bodyBold); no.setAlignment(Element.ALIGN_CENTER); no.setSpacingBefore(4);
            Paragraph date = new Paragraph("Ngày/ Date: " + format(data.reportDate()), bodyBold); date.setAlignment(Element.ALIGN_CENTER);
            left.addElement(t); left.addElement(te); left.addElement(no); left.addElement(date);
            head.addCell(left);
            PdfPCell right = noBorder(); right.setHorizontalAlignment(Element.ALIGN_CENTER);
            Image qr = qrImage(data);
            if (qr != null) { qr.scaleToFit(66, 66); qr.setAlignment(Element.ALIGN_CENTER); right.addElement(qr); }
            if (data.lookupCode() != null) {
                Paragraph code = new Paragraph("Mã tra cứu/ Lookup code\n" + groups(data.lookupCode()), new Font(regular, 7f));
                code.setAlignment(Element.ALIGN_CENTER); right.addElement(code);
            } else {
                Paragraph code = new Paragraph("Mã QR được in\nkhi phát hành phiếu", new Font(italic, 7f)); code.setAlignment(Element.ALIGN_CENTER); right.addElement(code);
            }
            head.addCell(right);
            document.add(head);

            // Sample information
            Paragraph info = new Paragraph("THÔNG TIN MẪU  / Sample information", section); info.setSpacingBefore(8); info.setSpacingAfter(3);
            document.add(info);
            PdfPTable infoTable = new PdfPTable(new float[]{0.35f, 2.6f, 0.2f, 4.2f});
            infoTable.setWidthPercentage(100);
            int n = 1;
            String nameBlock = String.join("\n", nonEmpty(data.sampleName()),
                    line("Mã vùng trồng/ Orchard code", data.orchardCode()), line("Mã cơ sở đóng gói/ Packaging number", data.packingFacility()),
                    line("Biển số xe/ Truck number", data.truckNumber()), line("Biển số mooc/ Mooc number", data.moocNumber()),
                    line("Số container/ Container number", data.containerNumber()), line("Số seal container", data.sealNumber())).strip();
            Object[][] rows = {
                    {"Đơn vị/ Khách hàng/ Customer", data.customerName()}, {"Địa chỉ/ Address", data.customerAddress()},
                    {"Tên mẫu/ Name of sample", nameBlock}, {"Mã số mẫu/ Sample code", data.sampleCode()},
                    {"Người lấy mẫu/ Sample collector", data.sampleCollector()}, {"Ngày lấy mẫu/ Sampling date", format(data.samplingDate())},
                    {"Tình trạng mẫu/ Sample condition", data.sampleCondition()}, {"Lượng mẫu/ Sample quantity", data.sampleQuantity()},
                    {"Ngày nhận mẫu/ Date sample received", format(data.dateReceived())}, {"Ngày thử nghiệm/ Date of testing", format(data.testingDate())}};
            for (Object[] row : rows) {
                infoTable.addCell(plain(n++ + ".", body)); infoTable.addCell(plain((String) row[0], body));
                infoTable.addCell(plain(":", body)); infoTable.addCell(plain(nonEmpty((String) row[1]), bodyBold));
            }
            document.add(infoTable);

            // Results
            Paragraph results = new Paragraph("KẾT QUẢ THỬ NGHIỆM  / Test results", section); results.setSpacingBefore(10); results.setSpacingAfter(3);
            document.add(results);
            PdfPTable table = new PdfPTable(new float[]{0.55f, 2.6f, 0.9f, 2.2f, 1.2f, 1.6f});
            table.setWidthPercentage(100); table.setHeaderRows(1); table.setSplitRows(false);
            for (String header : new String[]{"STT\nNo.", "Chỉ tiêu thử nghiệm\nParameters", "Đơn vị\nUnit", "Phương pháp thử nghiệm\nTest Method", "Kết quả\nResults", "Ghi chú\nNote"}) {
                PdfPCell cell = new PdfPCell(new Phrase(header, bodyBold));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER); cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBackgroundColor(new Color(0xEE, 0xEE, 0xEE)); cell.setPadding(4); cell.setBorderColor(LINE);
                table.addCell(cell);
            }
            int index = 1;
            for (ReportData.Row row : data.rows()) {
                table.addCell(cell(String.valueOf(index++), body, Element.ALIGN_CENTER));
                table.addCell(cell(row.parameter(), body, Element.ALIGN_LEFT));
                table.addCell(cell(row.unit(), body, Element.ALIGN_CENTER));
                table.addCell(cell(row.method(), body, Element.ALIGN_LEFT));
                table.addCell(cell(row.result(), bodyBold, Element.ALIGN_CENTER));
                table.addCell(cell(row.note(), body, Element.ALIGN_LEFT));
            }
            document.add(table);

            // Signatories
            PdfPTable sign = new PdfPTable(2);
            sign.setWidthPercentage(100); sign.setSpacingBefore(16); sign.setKeepTogether(true);
            sign.addCell(signCell("Trưởng phòng Quản lý chất lượng sản phẩm", "Head of Quality Management Department", data.qualityHeadName(), bodyBold, smallItalic, body));
            sign.addCell(signCell("Tổng Giám đốc", "General Director", data.directorName(), bodyBold, smallItalic, body));
            document.add(sign);

            // Notes
            Paragraph notes = new Paragraph("GHI CHÚ/ NOTES", noteBold); notes.setSpacingBefore(10); notes.setSpacingAfter(2);
            document.add(notes);
            PdfPTable noteTable = new PdfPTable(new float[]{1.25f, 1f});
            noteTable.setWidthPercentage(100);
            PdfPCell abbreviations = noBorder();
            for (String text : new String[]{
                    "KPH: Không phát hiện (dưới ngưỡng phát hiện của phương pháp)/ Not detected (below limit of detection)",
                    "LOD: Giới hạn phát hiện/ Limit of Detection; LOQ: Giới hạn định lượng/ Limit of Quantification",
                    "(1) Chỉ tiêu được công nhận bởi ISO/IEC 17025/ Parameters are accredited by ISO/IEC 17025",
                    "(2) Chỉ tiêu đã đăng ký hoạt động thử nghiệm theo Nghị định 22/ Parameters are registered under Decree 22 on testing activities",
                    "(3) Chỉ tiêu đã được các bộ ngành chỉ định/ Parameters are designated by the ministry",
                    "(*) Phép thử sử dụng nhà thầu phụ/ Parameters are tested by subcontractors"}) abbreviations.addElement(new Paragraph(text, note));
            noteTable.addCell(abbreviations);
            PdfPCell terms = noBorder();
            for (String text : new String[]{
                    "1. Kết quả chỉ có giá trị đối với mẫu được thử nghiệm/ Results are valid only for the tested sample.",
                    "2. Mục 1, 2, 3 là thông tin do khách hàng/ đơn vị gửi mẫu cung cấp/ Items 1, 2 and 3 are based on information provided by the customer.",
                    "3. Không được trích, sao một phần phiếu nếu không có sự đồng ý bằng văn bản của công ty/ This report can not be quoted or reproduced in part, except when permitted.",
                    "4. Khiếu nại về kết quả được giải quyết trong vòng 7 ngày kể từ ngày nhận phiếu/ Any claim should be sent to the lab within 7 days."}) terms.addElement(new Paragraph(text, note));
            noteTable.addCell(terms);
            document.add(noteTable);
        } catch (DocumentException exception) {
            throw new IllegalStateException("Không tạo được PDF phiếu kết quả", exception);
        } finally {
            if (document.isOpen()) document.close();
        }
        return out.toByteArray();
    }

    private Image qrImage(ReportData data) {
        if (data.lookupCode() == null || lookupBaseUrl.isBlank()) return null;
        try {
            String url = lookupBaseUrl + (lookupBaseUrl.contains("?") ? "&" : "?") + "code=" + data.lookupCode();
            var matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 220, 220, Map.of(EncodeHintType.MARGIN, 1));
            var raster = new java.awt.image.BufferedImage(matrix.getWidth(), matrix.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) raster.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(raster, "PNG", png);
            return Image.getInstance(png.toByteArray());
        } catch (Exception exception) {
            return null; // the printed lookup code is still on the report
        }
    }

    private static String groups(String code) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < code.length(); i += 4) builder.append(i > 0 ? "-" : "").append(code, i, Math.min(code.length(), i + 4));
        return builder.toString();
    }

    private static String format(LocalDate date) { return date == null ? "" : DATE.format(date); }
    private static String nonEmpty(String value) { return value == null ? "" : value; }
    private static String line(String label, String value) { return value == null || value.isBlank() ? "" : label + ": " + value; }

    private static PdfPCell noBorder() { PdfPCell cell = new PdfPCell(); cell.setBorder(Rectangle.NO_BORDER); return cell; }

    private static PdfPCell plain(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font)); cell.setBorder(Rectangle.NO_BORDER); cell.setPaddingBottom(3); return cell;
    }

    private static PdfPCell cell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(nonEmpty(text), font));
        cell.setHorizontalAlignment(align); cell.setVerticalAlignment(Element.ALIGN_MIDDLE); cell.setPadding(4); cell.setBorderColor(LINE);
        return cell;
    }

    private static PdfPCell signCell(String titleVi, String titleEn, String name, Font bold, Font italic, Font body) {
        PdfPCell cell = noBorder();
        Paragraph vi = new Paragraph(titleVi, bold); vi.setAlignment(Element.ALIGN_CENTER);
        Paragraph en = new Paragraph(titleEn, italic); en.setAlignment(Element.ALIGN_CENTER);
        Paragraph who = new Paragraph(nonEmpty(name), bold); who.setAlignment(Element.ALIGN_CENTER); who.setSpacingBefore(54);
        cell.addElement(vi); cell.addElement(en); cell.addElement(who);
        return cell;
    }

    /** Header (company, logo), watermark, footer with form code and "page x / y". */
    private final class PageDecorator extends PdfPageEventHelper {
        private final BaseFont regular, bold; private final Font small; private final boolean preview;
        private PdfTemplate total;

        PageDecorator(BaseFont regular, BaseFont bold, Font small, boolean preview) {
            this.regular = regular; this.bold = bold; this.small = small; this.preview = preview;
        }

        @Override public void onOpenDocument(PdfWriter writer, Document document) { total = writer.getDirectContent().createTemplate(30, 12); }

        @Override public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte under = writer.getDirectContentUnder();
                Image mark = Image.getInstance(watermark);
                mark.scaleToFit(360, 260);
                mark.setAbsolutePosition((PageSize.A4.getWidth() - mark.getScaledWidth()) / 2, 300);
                under.addImage(mark);

                PdfContentByte cb = writer.getDirectContent();
                Image logoImage = Image.getInstance(logo);
                logoImage.scaleToFit(62, 50); logoImage.setAbsolutePosition(37, 770);
                cb.addImage(logoImage);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase("CÔNG TY CỔ PHẦN PHÂN BÓN VÀ HOÁ CHẤT CẦN THƠ", new Font(bold, 11f)), 320, 806, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase("CAN THO FERTILIZER AND CHEMICAL JOINT STOCK COMPANY", new Font(bold, 8.5f)), 320, 793, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase("Khu Công Nghiệp Trà Nóc 1, Phường Thới An Đông, Thành Phố Cần Thơ, Việt Nam", new Font(regular, 7.5f)), 320, 781, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase("Tra Noc 1 Industrial park, Thoi An Dong Ward, Can Tho City, Viet Nam", new Font(regular, 7.5f)), 320, 771, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase("Tel: 1900 5307 - Email: info@cfccobay.com - www.cfcobay.com", new Font(regular, 7.5f)), 320, 761, 0);
                cb.setColorStroke(LINE); cb.setLineWidth(0.6f); cb.moveTo(37, 752); cb.lineTo(PageSize.A4.getWidth() - 37, 752); cb.stroke();

                cb.moveTo(37, 40); cb.lineTo(PageSize.A4.getWidth() - 37, 40); cb.stroke();
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase("BM.QT.14.02 · Lần ban hành: 01 · Ngày hiệu lực: 01/01/2026", small), 37, 28, 0);
                String text = "Trang " + writer.getPageNumber() + " / ";
                float width = regular.getWidthPoint(text, 7.5f);
                float x = PageSize.A4.getWidth() - 37 - width - 12;
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(text, small), x, 28, 0);
                cb.addTemplate(total, x + width, 28);

                if (preview) {
                    PdfContentByte over = writer.getDirectContent();
                    over.saveState();
                    PdfGState state = new PdfGState(); state.setFillOpacity(0.18f); over.setGState(state);
                    over.setColorFill(new Color(0xC0, 0x39, 0x2B));
                    over.beginText(); over.setFontAndSize(bold, 46);
                    over.showTextAligned(Element.ALIGN_CENTER, "BẢN XEM TRƯỚC - CHƯA PHÁT HÀNH", 297, 420, 40);
                    over.endText(); over.restoreState();
                }
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText(); total.setFontAndSize(regular, 7.5f); total.setTextMatrix(0, 0);
            total.showText(String.valueOf(writer.getPageNumber() - 1)); total.endText();
        }
    }
}
