-- Atomic per-year counter for test request numbers (CFC{YY}.{NNNN}).
CREATE TABLE test_request_counters (
    year_code CHAR(2) NOT NULL,
    last_sequence INT NOT NULL,
    PRIMARY KEY (year_code)
);

-- Seed from numbers already issued in the new format so automatic numbering never collides.
INSERT INTO test_request_counters (year_code, last_sequence)
SELECT SUBSTRING(request_number, 4, 2), MAX(CAST(SUBSTRING(request_number, 7) AS UNSIGNED))
FROM test_requests
WHERE request_number REGEXP '^CFC[0-9]{2}[.][0-9]{4,6}$'
GROUP BY SUBSTRING(request_number, 4, 2);
