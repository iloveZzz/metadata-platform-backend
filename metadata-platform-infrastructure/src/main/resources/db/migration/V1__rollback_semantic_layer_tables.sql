-- =====================================================================
-- SL-SLICE-01-WU-05 回滚脚本：DROP / 降级
-- 依据：数据架构 §11 表归属互斥 + 系统概要 §11.2 回滚约束（人工确认项）
-- 执行顺序：先删子表（term_alias / audit_log），再删父表 term
-- 注意：回滚为不可逆操作，仅限开发 / 预发环境使用；生产回滚须经人工评审
-- =====================================================================
USE `semantic_layer`;

DROP TABLE IF EXISTS `term_alias`;
DROP TABLE IF EXISTS `audit_log`;
DROP TABLE IF EXISTS `term`;
