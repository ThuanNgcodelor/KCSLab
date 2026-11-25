package com.example.app.dossier.storage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ArchivePaths {
    private ArchivePaths() {}
    public static String folder(long receiptId, String number, Instant receivedAt) {
        String name = number == null ? "ho-so" : number.trim();
        if (!name.matches("(?i)CFC[0-9]{2}\\.[0-9]{4,}")) {
            name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
            name = name.substring(0, Math.min(80, name.length())) + "-r" + receiptId;
        }
        return DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(receivedAt) + "/" + name;
    }
}
