package com.example.app.dossier.storage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ArchivePaths {
    private ArchivePaths() {}
    /** Old dossiers may share one Số/No. (V16); only the owner may use the plain number as a folder name. */
    public static boolean owns(String number, String uniqueNumber) { return number != null && number.equalsIgnoreCase(uniqueNumber); }
    public static String folder(long receiptId, String number, boolean owner, Instant receivedAt) {
        String name = number == null || number.isBlank() ? "ho-so" : number.trim();
        boolean plain = name.matches("(?i)CFC[0-9]{2}\\.[0-9]{4,}");
        if (!plain) {
            name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
            name = name.substring(0, Math.min(80, name.length()));
        }
        return separate(DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(receivedAt) + "/" + name, receiptId, plain && owner);
    }
    public static String separate(String folder, long receiptId, boolean owner) {
        String suffix = "-r" + receiptId;
        return owner || folder.endsWith(suffix) ? folder : folder + suffix;
    }
}
