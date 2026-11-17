package com.example.app.results.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Converts an uploaded KQKN DOCX to PDF with LibreOffice running headless ({@code soffice --convert-to pdf}).
 * Each run gets its own temporary directory and LibreOffice profile (so parallel runs do not lock each other),
 * is limited by a timeout and by a small number of concurrent slots, and leaves nothing behind.
 * The DOCX has already passed {@link ResultFilePolicy} (no macros, OLE objects, external links or templates).
 */
@Component
public class OfficeConverter {
    private final String command;
    private final long timeoutSeconds;
    private final Semaphore slots = new Semaphore(2);

    public OfficeConverter(@Value("${app.report.office-command:soffice}") String command,
                           @Value("${app.report.office-timeout-seconds:90}") long timeoutSeconds) {
        this.command = command == null || command.isBlank() ? "soffice" : command.trim();
        this.timeoutSeconds = Math.max(10, timeoutSeconds);
    }

    public byte[] docxToPdf(byte[] docx) {
        boolean acquired;
        try {
            acquired = slots.tryAcquire(30, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw busy();
        }
        if (!acquired) throw busy();
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("kqkn-convert-");
            Path input = workDir.resolve("phieu.docx");
            Path output = Files.createDirectory(workDir.resolve("out"));
            Path profile = Files.createDirectory(workDir.resolve("profile"));
            Files.write(input, docx);
            ProcessBuilder builder = new ProcessBuilder(List.of(command, "--headless", "--norestore", "--nologo",
                    "--nodefault", "--nolockcheck", "--nofirststartwizard",
                    "-env:UserInstallation=" + profile.toUri(),
                    "--convert-to", "pdf:writer_pdf_Export", "--outdir", output.toString(), input.toString()))
                    .directory(workDir.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(workDir.resolve("convert.log").toFile());
            builder.environment().put("HOME", workDir.toString());
            Process process;
            try {
                process = builder.start();
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Máy chủ chưa cài LibreOffice nên chưa chuyển được DOCX sang PDF. "
                                + "Hãy cài LibreOffice (soffice) hoặc tải lên bản PDF xuất từ Word.", exception);
            }
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "Chuyển DOCX sang PDF quá lâu. Hãy kiểm tra file hoặc tải lên bản PDF xuất từ Word.");
            }
            Path pdf = output.resolve("phieu.pdf");
            if (process.exitValue() != 0 || !Files.isRegularFile(pdf) || Files.size(pdf) == 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "Không chuyển được DOCX sang PDF. Hãy mở file bằng Word kiểm tra lại hoặc tải lên bản PDF.");
            }
            return Files.readAllBytes(pdf);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể chuyển DOCX sang PDF", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw busy();
        } finally {
            slots.release();
            deleteQuietly(workDir);
        }
    }

    private static ResponseStatusException busy() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Máy chủ đang bận chuyển đổi file, vui lòng thử lại sau");
    }

    private static void deleteQuietly(Path directory) {
        if (directory == null) return;
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
