package com.example.app.intake.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/** Formatting rules of the base sample code: {@code SOURCE-PRODUCT-DDMMYYYY-SEQUENCE}. */
public final class SampleCodes {
    private static final DateTimeFormatter SAMPLE_DATE = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final Map<String, String> COMMON_PRODUCT_CODES = Map.of(
            "sr", "SR", "sau rieng", "SR", "mit", "MIT", "thanh long", "THANHLONG");

    private SampleCodes() {}

    static String format(String source, String productType, LocalDate date, int sequence) {
        return "%s-%s-%s-%03d".formatted(source, productCode(productType), SAMPLE_DATE.format(date), sequence);
    }

    static String normalizeSource(String sourceCode) {
        return sourceCode.trim().toUpperCase(Locale.ROOT);
    }

    public static String productCode(String productType) {
        String normalized = Normalizer.normalize(productType, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        String knownCode = COMMON_PRODUCT_CODES.get(normalized);
        if (knownCode != null) return knownCode;

        String compact = normalized.replace(" ", "").toUpperCase(Locale.ROOT);
        if (compact.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Loại sản phẩm phải có ít nhất một chữ hoặc số");
        }
        if (compact.length() <= 10) return compact;

        String[] words = normalized.split(" +");
        if (words.length > 1) {
            String initials = Arrays.stream(words)
                    .filter(word -> !word.isBlank())
                    .map(word -> word.substring(0, 1))
                    .reduce("", String::concat)
                    .toUpperCase(Locale.ROOT);
            if (initials.length() >= 2) return initials.substring(0, Math.min(10, initials.length()));
        }
        return compact.substring(0, 10);
    }
}
