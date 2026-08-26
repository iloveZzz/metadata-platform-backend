-- =====================================================================
-- V1__dq_ingestion_tables.rollback.sql（人工回滚脚本，Flyway 不自动执行）
-- DQ Insight 切片 01：回滚 dq_batch / dq_rule_result / dq_asset_linkage / dq_audit_log
-- 回滚顺序：先删规则明细 / 关联（引用批次），再删批次与审计（数据架构 §12 回滚顺序）。
-- 人工确认项：回滚前确认无存量引用；回滚约束见切片 01 证据。
-- =====================================================================

DROP TABLE IF EXISTS dq_rule_result;
DROP TABLE IF EXISTS dq_asset_linkage;
DROP TABLE IF EXISTS dq_batch;
DROP TABLE IF EXISTS dq_audit_log;
