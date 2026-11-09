CREATE TABLE exam_import_resources (
    resource_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    import_id BIGINT NOT NULL,
    section_key VARCHAR(64) NOT NULL,
    section_title VARCHAR(120) NOT NULL,
    resource_type VARCHAR(24) NOT NULL DEFAULT 'AUDIO',
    storage_key VARCHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_id VARCHAR(254) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_id VARCHAR(254) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_exam_import_resource_import FOREIGN KEY (import_id)
        REFERENCES exam_imports(import_id) ON DELETE CASCADE,
    UNIQUE KEY uk_exam_import_resource_file (import_id, storage_key),
    INDEX idx_exam_import_resource_section (import_id, section_key, resource_type)
);
