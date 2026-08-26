--liquibase formatted sql

--changeset dq-insight:002-dq-health-score-tables
--comment: 切片 02 健康分表（原 V2__dq_health_score_tables.sql 迁移至 Liquibase，2026-08-13）：dq_health_score / dq_rule_detail

CREATE TABLE dq_health_score (
    id              BIGINT        NOT NULL,
    asset_id        VARCHAR(128)  NOT NULL,
    field_name      VARCHAR(255)  NULL,
    asset_name      VARCHAR(255)  NULL,
    domain          VARCHAR(255)  NULL,
    asset_type      VARCHAR(32)   NULL,
    score           INT           NULL,
    band            VARCHAR(8)    NULL,
    state           VARCHAR(16)   NOT NULL,
    rule_version    VARCHAR(16)   NOT NULL,
    batch_id        BIGINT        NOT NULL,
    computed_at     DATETIME      NOT NULL,
    pass_rate       VARCHAR(8)    NULL,
    last_result_at  DATETIME      NULL,
    valid_until     DATETIME      NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dq_health_score_asset_field UNIQUE (asset_id, field_name)
);

CREATE INDEX idx_dq_health_score_domain_band ON dq_health_score (domain, band);
CREATE INDEX idx_dq_health_score_domain_state ON dq_health_score (domain, state);
CREATE INDEX idx_dq_health_score_band ON dq_health_score (band);
CREATE INDEX idx_dq_health_score_asset ON dq_health_score (asset_id);

CREATE TABLE dq_rule_detail (
    id              BIGINT        NOT NULL,
    batch_id        BIGINT        NOT NULL,
    asset_id        VARCHAR(128)  NOT NULL,
    field_name      VARCHAR(255)  NULL,
    rule_name       VARCHAR(255)  NOT NULL,
    rule_type       VARCHAR(32)   NOT NULL,
    status          VARCHAR(16)   NOT NULL,
    weight          DECIMAL(4,2)  NOT NULL,
    failure_reason  VARCHAR(2048) NULL,
    execution_time  DATETIME      NULL,
    rule_version    VARCHAR(16)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dq_rule_detail_batch_asset_field_rule UNIQUE (batch_id, asset_id, field_name, rule_name)
);

CREATE INDEX idx_dq_rule_detail_asset ON dq_rule_detail (asset_id);
CREATE INDEX idx_dq_rule_detail_batch_asset_field ON dq_rule_detail (batch_id, asset_id, field_name);
