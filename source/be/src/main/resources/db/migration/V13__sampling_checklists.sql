-- Checklist data of Phiếu 07 (kế hoạch lấy mẫu) and Phiếu 02 (lấy mẫu hiện trường), following the Word templates.
ALTER TABLE sampling_plans
    ADD COLUMN representative_phone VARCHAR(30) NULL AFTER representative,
    ADD COLUMN sample_forms VARCHAR(80) NULL,
    ADD COLUMN lot_size_band VARCHAR(20) NULL,
    ADD COLUMN transport_modes VARCHAR(120) NULL;

ALTER TABLE field_sampling_forms
    ADD COLUMN representative_phone VARCHAR(30) NULL AFTER representative,
    ADD COLUMN storage_temperature VARCHAR(20) NULL;

CREATE TABLE field_sampling_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    field_sampling_id BIGINT NOT NULL,
    row_order INT NOT NULL,
    sample_name VARCHAR(200) NOT NULL,
    sampling_method VARCHAR(255) NULL,
    quantity VARCHAR(120) NULL,
    parameters VARCHAR(80) NULL,
    parameters_other VARCHAR(200) NULL,
    PRIMARY KEY (id),
    KEY idx_field_sampling_items_form (field_sampling_id, row_order),
    CONSTRAINT fk_field_sampling_items_form FOREIGN KEY (field_sampling_id) REFERENCES field_sampling_forms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
