CREATE TABLE dossier_export_jobs (
 id VARCHAR(36) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
 request_id VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
 actor_user_id BIGINT NOT NULL,
 scope_json TEXT NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'QUEUED',
 cutoff_at TIMESTAMP(6) NOT NULL,
 snapshot_at TIMESTAMP(6) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 finished_at TIMESTAMP(6) NULL,
 expires_at TIMESTAMP(6) NULL,
 lease_token VARCHAR(36) COLLATE utf8mb4_bin NULL,
 lease_until TIMESTAMP(6) NULL,
 download_until TIMESTAMP(6) NULL,
 attempt VARCHAR(36) COLLATE utf8mb4_bin NULL,
 total_files BIGINT NOT NULL DEFAULT 0,
 total_bytes BIGINT NOT NULL DEFAULT 0,
 processed_files BIGINT NOT NULL DEFAULT 0,
 processed_bytes BIGINT NOT NULL DEFAULT 0,
 error_code VARCHAR(80) NULL,
 allow_missing BOOLEAN NOT NULL DEFAULT FALSE,
 attempts INT NOT NULL DEFAULT 0,
 warning_count BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_dossier_export_request(actor_user_id,request_id),
 KEY idx_dossier_export_claim(status,created_at),
 KEY idx_dossier_export_expiry(status,expires_at),
 CONSTRAINT fk_dossier_export_actor FOREIGN KEY(actor_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE dossier_export_entries (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 job_id VARCHAR(36) COLLATE utf8mb4_bin NOT NULL,
 entry_path VARCHAR(1024) COLLATE utf8mb4_bin NOT NULL,
 location_id BIGINT NULL,
 relative_path VARCHAR(1024) COLLATE utf8mb4_bin NULL,
 storage_name VARCHAR(255) NULL,
 size BIGINT NOT NULL,
 checksum VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
 payload MEDIUMTEXT NULL,
 KEY idx_export_entry_job(job_id,id),
 CONSTRAINT fk_export_entry_job FOREIGN KEY(job_id) REFERENCES dossier_export_jobs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE dossier_export_parts (
 job_id VARCHAR(36) COLLATE utf8mb4_bin NOT NULL,
 part_number INT NOT NULL,
 relative_path VARCHAR(255) COLLATE utf8mb4_bin NOT NULL,
 size BIGINT NOT NULL,
 checksum VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
 PRIMARY KEY(job_id,part_number),
 CONSTRAINT fk_export_part_job FOREIGN KEY(job_id) REFERENCES dossier_export_jobs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE dossier_export_worker (
 id INT NOT NULL PRIMARY KEY,
 lease_token VARCHAR(36) COLLATE utf8mb4_bin NULL,
 lease_until TIMESTAMP(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO dossier_export_worker(id) VALUES(1);
