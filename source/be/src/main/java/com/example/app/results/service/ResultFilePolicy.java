package com.example.app.results.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Validates result dossier files: size, extension, declared MIME type, magic bytes and, for Office
 * Open XML, structure and dangerous parts (macros, embedded objects, external links/templates).
 * This is a best-effort content check; it is not a substitute for an antivirus scan (needs LAB decision).
 */
final class ResultFilePolicy {
    static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    static final int MAX_FILES_PER_RELEASE = 10;
    private static final long MAX_UNCOMPRESSED = 100L * 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 3000;
    private static final Map<String, String> TYPES = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final Map<String, String> REQUIRED_PART = Map.of(
            "docx", "word/document.xml", "xlsx", "xl/workbook.xml");
    private static final Pattern PDF_ACTIVE = Pattern.compile("/(JavaScript|JS|Launch|EmbeddedFile)\\b");
    private static final Set<String> DANGEROUS_RELS = Set.of("attachedTemplate", "oleObject", "externalLink");

    record Validated(String originalName, String extension, String contentType, byte[] content) {
        boolean isPdf() { return "pdf".equals(extension); }
    }

    private ResultFilePolicy() {}

    static Validated validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad(HttpStatus.BAD_REQUEST, "File kết quả không được để trống");
        if (file.getSize() > MAX_FILE_SIZE) {
            throw bad(HttpStatus.PAYLOAD_TOO_LARGE, "Mỗi file kết quả không được vượt quá 10 MB");
        }
        String name = sanitizeName(file.getOriginalFilename());
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        String expectedType = TYPES.get(extension);
        if (expectedType == null) {
            throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Chỉ chấp nhận file PDF, DOCX hoặc XLSX");
        }
        String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!declared.equals(expectedType)) {
            throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Loại nội dung khai báo không khớp với đuôi file");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw bad(HttpStatus.BAD_REQUEST, "Không đọc được file tải lên");
        }
        if (content.length > MAX_FILE_SIZE) {
            throw bad(HttpStatus.PAYLOAD_TOO_LARGE, "Mỗi file kết quả không được vượt quá 10 MB");
        }
        if (extension.equals("pdf")) checkPdf(content); else checkOoxml(content, extension);
        return new Validated(name, extension, expectedType, content);
    }

    static String sanitizeName(String original) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty() || name.length() > 200 || name.contains("..")
                || name.chars().anyMatch(ch -> ch < 0x20 || ch == 0x7f)) {
            throw bad(HttpStatus.BAD_REQUEST, "Tên file không hợp lệ");
        }
        return name;
    }

    static void checkPdf(byte[] content) {
        String head = new String(content, 0, Math.min(content.length, 1024), StandardCharsets.ISO_8859_1);
        if (!head.startsWith("%PDF-")) throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Nội dung không phải PDF hợp lệ");
        int tailStart = Math.max(0, content.length - 2048);
        String tail = new String(content, tailStart, content.length - tailStart, StandardCharsets.ISO_8859_1);
        if (!tail.contains("%%EOF")) throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File PDF bị cắt hoặc hỏng");
        if (PDF_ACTIVE.matcher(new String(content, StandardCharsets.ISO_8859_1)).find()) {
            throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "PDF chứa mã chủ động hoặc tệp nhúng nên bị từ chối");
        }
    }

    private static void checkOoxml(byte[] content, String extension) {
        if (content.length < 4 || content[0] != 'P' || content[1] != 'K' || content[2] != 3 || content[3] != 4) {
            throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Nội dung không phải tài liệu Office hợp lệ");
        }
        boolean hasContentTypes = false;
        boolean hasMainPart = false;
        int entries = 0;
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ZIP_ENTRIES) throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tài liệu có quá nhiều thành phần");
                String name = entry.getName();
                String lower = name.toLowerCase(Locale.ROOT);
                if (name.contains("..") || name.startsWith("/")) {
                    throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tài liệu có đường dẫn nội bộ không hợp lệ");
                }
                if (lower.contains("vbaproject") || lower.contains("/embeddings/") || lower.contains("activex")
                        || lower.contains("externallinks/")) {
                    throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                            "Tài liệu chứa macro, đối tượng nhúng hoặc liên kết ngoài nên bị từ chối");
                }
                boolean inspect = lower.equals("[content_types].xml") || lower.endsWith(".rels");
                java.io.ByteArrayOutputStream part = inspect ? new java.io.ByteArrayOutputStream() : null;
                int read;
                while ((read = zip.read(buffer)) > 0) {
                    total += read;
                    if (total > MAX_UNCOMPRESSED) throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tài liệu giải nén quá lớn");
                    if (part != null && part.size() < 1_000_000) part.write(buffer, 0, read);
                }
                if (name.equals("[Content_Types].xml")) hasContentTypes = true;
                if (name.equals(REQUIRED_PART.get(extension))) hasMainPart = true;
                if (part != null) {
                    String text = part.toString(StandardCharsets.UTF_8);
                    if (lower.equals("[content_types].xml") && text.contains("macroEnabled")) {
                        throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tài liệu có macro nên bị từ chối");
                    }
                    if (lower.endsWith(".rels") && DANGEROUS_RELS.stream().anyMatch(text::contains)) {
                        throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                                "Tài liệu chứa mẫu đính kèm, đối tượng OLE hoặc liên kết ngoài nên bị từ chối");
                    }
                }
            }
        } catch (IOException exception) {
            throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tài liệu Office bị hỏng");
        }
        if (!hasContentTypes || !hasMainPart) {
            throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Cấu trúc tài liệu Office không hợp lệ");
        }
    }

    private static ResponseStatusException bad(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
