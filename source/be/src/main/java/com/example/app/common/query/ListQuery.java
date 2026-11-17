package com.example.app.common.query;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;

/** Server-side paging, whitelisted sorting and Vietnam-time date ranges shared by list endpoints. */
public final class ListQuery {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final int MAX_PAGE_SIZE = 100;

    private ListQuery() {}

    public record Parsed(Sort sort, String canonical) {}

    /**
     * @param whitelist public sort key -> entity property path; anything else is a 400.
     * @param defaultSort value used when the client sends none, e.g. {@code createdAt,desc}.
     */
    public static Parsed parseSort(String raw, Map<String, String> whitelist, String defaultSort) {
        String value = raw == null || raw.isBlank() ? defaultSort : raw.trim();
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort phải có dạng field,direction");
        }
        String key = parts[0].trim();
        String property = whitelist.get(key);
        if (property == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trường sort không hợp lệ: " + key);
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1].trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chiều sort chỉ nhận asc hoặc desc");
        }
        Sort sort = Sort.by(direction, property);
        if (!"id".equals(property)) sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        return new Parsed(sort, key + "," + direction.name().toLowerCase(Locale.ROOT));
    }

    public static PageRequest page(int page, int size, Parsed parsed) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số trang phải từ 0 trở lên");
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích thước trang phải từ 1 đến " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, parsed.sort());
    }

    public static void requireOrderedRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu không được sau ngày kết thúc");
        }
    }

    public static Instant startOfDay(LocalDate date) { return date.atStartOfDay(BUSINESS_ZONE).toInstant(); }

    /** Exclusive upper bound for an inclusive end date. */
    public static Instant endExclusive(LocalDate date) { return date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant(); }

    /** Escaped LIKE pattern, lower-cased, for use with {@code like(lower(x), pattern, '\\')}. */
    public static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    public static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
