--liquibase formatted sql

--changeset dq-insight:004-dq-audit-log-query-index
--comment: 切片 05 审计查询复合索引（原 V4__dq_audit_log_query_index.sql 迁移至 Liquibase，2026-08-13）：仅 ADD INDEX，不触碰数据行（append-only 红线）

CREATE INDEX idx_dq_audit_log_action_time
    ON dq_audit_log (action, event_time DESC);
