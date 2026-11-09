ALTER TABLE exam_imports
    ADD COLUMN source_file_key VARCHAR(36) NULL,
    ADD COLUMN source_file_name VARCHAR(255) NULL,
    ADD COLUMN source_file_content_type VARCHAR(120) NULL,
    ADD COLUMN source_file_size BIGINT NULL,
    ADD COLUMN source_file_sha256 CHAR(64) NULL;
