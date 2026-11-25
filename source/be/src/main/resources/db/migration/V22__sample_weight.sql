-- Keep the original quantity text. An independent weight must be measured/entered, not inferred from a count.
ALTER TABLE samples ADD COLUMN weight VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;
