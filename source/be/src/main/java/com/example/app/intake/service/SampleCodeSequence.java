package com.example.app.intake.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * Atomic per-day sample sequence. The counter row is incremented with a single
 * {@code INSERT ... ON DUPLICATE KEY UPDATE}, so concurrent receipts never share a number and a
 * number is never handed out twice (not even after a correction retires a code). It must run in the
 * caller's transaction: if the receipt rolls back the increment rolls back with it, leaving no gap.
 */
@Component
public class SampleCodeSequence {
    static final int MAX_DAILY_SEQUENCE = 999;
    private final JdbcTemplate jdbc;

    public SampleCodeSequence(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public int next(LocalDate businessDate) {
        jdbc.update("INSERT INTO sample_code_counters (counter_date, last_sequence) VALUES (?, LAST_INSERT_ID(1)) "
                + "ON DUPLICATE KEY UPDATE last_sequence = LAST_INSERT_ID(last_sequence + 1)", businessDate);
        Integer value = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        if (value == null || value > MAX_DAILY_SEQUENCE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Đã hết số thứ tự mã mẫu trong ngày (tối đa " + MAX_DAILY_SEQUENCE + ")");
        }
        return value;
    }
}
