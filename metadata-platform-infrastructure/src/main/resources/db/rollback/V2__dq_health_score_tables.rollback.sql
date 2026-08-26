-- =====================================================================
-- V2__dq_health_score_tables.rollback.sql（人工回滚脚本，Flyway 不自动执行）
-- DQ Insight 切片 02：回滚 dq_health_score / dq_rule_detail
-- 回滚顺序：先删规则明细（引用批次），再删健康分（数据架构 §12 回滚顺序）。
-- 人工确认项：回滚前确认无存量引用；回滚约束见切片 02 证据。
-- =====================================================================

DROP TABLE IF EXISTS dq_rule_detail;
DROP TABLE IF EXISTS dq_health_score;
