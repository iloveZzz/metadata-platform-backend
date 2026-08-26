--liquibase formatted sql

--changeset dq-insight:001-dq-ingestion-tables
--comment: 切片 01 结果接入表（原 V1__dq_ingestion_tables.sql 迁移至 Liquibase，2026-08-13，对齐主平台 Liquibase 技术栈）：dq_batch / dq_rule_result / dq_asset_linkage / dq_audit_log

CREATE TABLE dq_batch (
    id              BIGINT       NOT NULL,
    batch_no        VARCHAR(64)  NOT NULL,
    source_tool     VARCHAR(32)  NOT NULL,
    format_type     VARCHAR(16)  NOT NULL,
    channel_id      VARCHAR(64)  NULL,
    status          VARCHAR(32)  NOT NULL,
    linkage_status  VARCHAR(16)  NOT NULL DEFAULT 'none',
    received_at     DATETIME     NOT NULL,
    execution_time  DATETIME     NULL,
    row_count       INT          NOT NULL DEFAULT 0,
    error_category  VARCHAR(16)  NULL,
    error_message   VARCHAR(2048) NULL,
    valid_until     DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dq_batch_source_tool_batch_no UNIQUE (source_tool, batch_no)
);

CREATE INDEX idx_dq_batch_channel_id ON dq_batch (channel_id);
CREATE INDEX idx_dq_batch_status ON dq_batch (status);
CREATE INDEX idx_dq_batch_received_at ON dq_batch (received_at);

CREATE TABLE dq_rule_result (
    id              BIGINT        NOT NULL,
    batch_id        BIGINT        NOT NULL,
    asset_id        VARCHAR(128)  NULL,
    field_name      VARCHAR(255)  NULL,
    rule_name       VARCHAR(255)  NOT NULL,
    rule_type       VARCHAR(32)   NOT NULL,
    status          VARCHAR(16)   NOT NULL,
    failure_reason  VARCHAR(2048) NULL,
    execution_time  DATETIME      NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_dq_rule_result_batch_id ON dq_rule_result (batch_id);
CREATE INDEX idx_dq_rule_result_asset ON dq_rule_result (asset_id, field_name);
CREATE INDEX idx_dq_rule_result_asset_status ON dq_rule_result (asset_id, field_name, status);

CREATE TABLE dq_asset_linkage (
    id                BIGINT        NOT NULL,
    batch_id          BIGINT        NOT NULL,
    source_asset_id   VARCHAR(128)  NOT NULL,
    resolved_asset_id VARCHAR(128)  NULL,
    asset_name        VARCHAR(255)  NULL,
    domain            VARCHAR(255)  NULL,
    asset_type        VARCHAR(32)   NULL,
    match_mode        VARCHAR(16)   NULL,
    state             VARCHAR(16)   NOT NULL,
    created_at        DATETIME      NOT NULL,
    mapped_at         DATETIME      NULL,
    mapped_by         VARCHAR(64)   NULL,
    note              VARCHAR(1024) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dq_asset_linkage_batch_asset UNIQUE (batch_id, source_asset_id)
);

CREATE INDEX idx_dq_asset_linkage_state ON dq_asset_linkage (state);

CREATE TABLE dq_audit_log (
    id          BIGINT        NOT NULL,
    event_time  DATETIME      NOT NULL,
    operator    VARCHAR(64)   NULL,
    action      VARCHAR(32)   NOT NULL,
    object      VARCHAR(255)  NULL,
    result      VARCHAR(16)   NOT NULL,
    detail      VARCHAR(2048) NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_dq_audit_log_event_time ON dq_audit_log (event_time);
CREATE INDEX idx_dq_audit_log_action ON dq_audit_log (action);
