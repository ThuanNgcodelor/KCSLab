package com.example.app.results.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Atomic result-number sequence per prefix (product code + MMYYYY), producing numbers like {@code SR092026-001}
 * as in the LAB's KQKN template. LAB has not formally confirmed this rule yet. Runs inside the publish transaction,
 * so a failed publication does not burn a number.
 */
@Component
public class ResultNumberSequence {
    private static final int MAX = 999;
    private final JdbcTemplate jdbc;

    public ResultNumberSequence(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(String prefix) {
        jdbc.update("INSERT INTO result_number_counters (prefix, last_sequence) VALUES (?, LAST_INSERT_ID(1)) "
                + "ON DUPLICATE KEY UPDATE last_sequence = LAST_INSERT_ID(last_sequence + 1)", prefix);
        Integer value = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        if (value == null || value > MAX) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đã hết số phiếu kết quả trong kỳ " + prefix);
        }
        return "%s-%03d".formatted(prefix, value);
    }
}
