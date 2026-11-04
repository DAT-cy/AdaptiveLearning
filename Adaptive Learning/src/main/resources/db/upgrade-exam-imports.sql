CREATE TABLE exam_imports (
    import_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    certificate_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    source_content LONGTEXT NOT NULL,
    answer_content LONGTEXT NULL,
    questions_json LONGTEXT NOT NULL,
    answer_generated BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING_REVIEW',
    uploaded_by BIGINT NOT NULL,
    created_id VARCHAR(254) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_id VARCHAR(254) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_exam_import_status_created (status, created_at),
    INDEX idx_exam_import_certificate_topic (certificate_id, topic_id)
);
