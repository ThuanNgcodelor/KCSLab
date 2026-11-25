-- Keep existing physical files in place; each file remembers its own storage location.
CREATE TABLE storage_locations (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 label VARCHAR(120) NOT NULL,
 legacy_key VARCHAR(30) NULL,
 root_path VARCHAR(700) COLLATE utf8mb4_bin NULL,
 marker VARCHAR(36) COLLATE utf8mb4_bin NULL,
 volume_identity VARCHAR(500) COLLATE utf8mb4_bin NULL,
 validated_at TIMESTAMP(6) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_storage_legacy (legacy_key),
 UNIQUE KEY uk_storage_root (root_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO storage_locations(id,label,legacy_key) VALUES
 (1,'Tiếp nhận hiện tại','RECEIPTS'),(2,'Kết quả hiện tại','RESULTS');
CREATE TABLE storage_write_settings (
 id INT NOT NULL PRIMARY KEY,
 location_id BIGINT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_storage_write_location FOREIGN KEY(location_id) REFERENCES storage_locations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO storage_write_settings(id) VALUES (1);
CREATE TABLE dossier_audit (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 actor_user_id BIGINT NOT NULL,
 action VARCHAR(40) NOT NULL,
 object_id VARCHAR(80) NULL,
 detail VARCHAR(500) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_dossier_audit_created(created_at),
 CONSTRAINT fk_dossier_audit_user FOREIGN KEY(actor_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE receipts ADD COLUMN archive_path VARCHAR(255) COLLATE utf8mb4_bin NULL;
ALTER TABLE receipt_attachments
 ADD COLUMN storage_location_id BIGINT NOT NULL DEFAULT 1,
 ADD COLUMN relative_path VARCHAR(1024) COLLATE utf8mb4_bin NULL,
 ADD CONSTRAINT fk_receipt_attachment_storage FOREIGN KEY(storage_location_id) REFERENCES storage_locations(id);
ALTER TABLE result_release_files
 ADD COLUMN storage_location_id BIGINT NOT NULL DEFAULT 2,
 ADD COLUMN relative_path VARCHAR(1024) COLLATE utf8mb4_bin NULL,
 ADD CONSTRAINT fk_result_file_storage FOREIGN KEY(storage_location_id) REFERENCES storage_locations(id);
