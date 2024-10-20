CREATE TABLE documents (
    id                  INTEGER PRIMARY KEY,
    is_active           TINYINT NOT NULL,
    document_key        VARCHAR(100) NOT NULL,
    title               VARCHAR(200),
    description         VARCHAR(400),
    note                VARCHAR(400),
    upload_file_path    VARCHAR(300),
    timestamp_file_path VARCHAR(300),
    verified_at         DATETIME,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
