package com.example.app.sampling.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test request numbers: {@code CFC{YY}.{NNNN}} (e.g. CFC26.0001). The sequence has at least four
 * digits and simply grows a digit after 9999. It is counted per two-digit year code, which defaults
 * to the current year but may be chosen by the user. Allocation uses one atomic
 * {@code INSERT ... ON DUPLICATE KEY UPDATE}, so concurrent requests never share a number; a number
 * typed by hand raises the counter so automatic numbers never collide with it.
 */
@Component
public class TestRequestNumberSequence {
    public static final Pattern FORMAT = Pattern.compile("^CFC(\\d{2})\\.(\\d{4,6})$");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Autowired
    public TestRequestNumberSequence(JdbcTemplate jdbc) { this(jdbc, Clock.system(ZONE)); }

    TestRequestNumberSequence(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }

    public String currentYearCode() { return String.format("%02d", clock.instant().atZone(ZONE).getYear() % 100); }

    public static String format(String yearCode, long sequence) { return String.format("CFC%s.%04d", yearCode, sequence); }

    /** Suggestion only: does not consume a number. */
    @Transactional(readOnly = true)
    public String peek() {
        String year = currentYearCode();
        Long last = jdbc.query("SELECT last_sequence FROM test_request_counters WHERE year_code = ?",
                rs -> rs.next() ? rs.getLong(1) : 0L, year);
        return format(year, (last == null ? 0 : last) + 1);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String allocate(String yearCode) {
        jdbc.update("INSERT INTO test_request_counters (year_code, last_sequence) VALUES (?, LAST_INSERT_ID(1)) "
                + "ON DUPLICATE KEY UPDATE last_sequence = LAST_INSERT_ID(last_sequence + 1)", yearCode);
        Long value = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return format(yearCode, value == null ? 1 : value);
    }

    /** Records a hand-typed number: raises the counter and locks the year row until the transaction ends. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void register(String number) {
        Matcher m = FORMAT.matcher(number);
        if (!m.matches()) throw new IllegalArgumentException("Invalid request number");
        jdbc.update("INSERT INTO test_request_counters (year_code, last_sequence) VALUES (?, ?) AS n "
                + "ON DUPLICATE KEY UPDATE last_sequence = GREATEST(test_request_counters.last_sequence, n.last_sequence)",
                m.group(1), Long.parseLong(m.group(2)));
    }

    /** Next free number for the same year code as {@code number}, used in conflict messages. */
    @Transactional(readOnly = true)
    public String nextAfter(String number) {
        Matcher m = FORMAT.matcher(number);
        String year = m.matches() ? m.group(1) : currentYearCode();
        Long last = jdbc.query("SELECT last_sequence FROM test_request_counters WHERE year_code = ?",
                rs -> rs.next() ? rs.getLong(1) : 0L, year);
        return format(year, (last == null ? 0 : last) + 1);
    }
}
