--liquibase formatted sql

--changeset dq-insight:003-dq-channel-tables
--comment: 切片 04 通道表（原 V3__dq_channel_tables.sql 迁移至 Liquibase，2026-08-13）：dq_channel

CREATE TABLE dq_channel (
    id              BIGINT         NOT NULL,
    name            VARCHAR(255)   NOT NULL,
    type            VARCHAR(32)    NOT NULL,
    schedule        VARCHAR(64)    NULL,
    format_type     VARCHAR(32)    NOT NULL,
    auth_token_enc  TEXT           NULL,
    auth_configured TINYINT(1)     NOT NULL DEFAULT 0,
    domain          VARCHAR(255)   NULL,
    state           VARCHAR(16)    NOT NULL,
    last_pull_at    DATETIME       NULL,
    last_error      VARCHAR(1024)  NULL,
    error_category  VARCHAR(16)    NULL,
    deleted_at      DATETIME       NULL,
    created_by      VARCHAR(64)    NULL,
    created_at      DATETIME       NOT NULL,
    updated_at      DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dq_channel_name UNIQUE (name)
);

CREATE INDEX idx_dq_channel_state ON dq_channel (state);
CREATE INDEX idx_dq_channel_type_state ON dq_channel (type, state);
