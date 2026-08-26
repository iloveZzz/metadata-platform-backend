-- MP-SLICE-05 / WU-05-DB 追加回滚（顺序：integration_config、openlineage_event 最前，无被依赖方）
DROP TABLE IF EXISTS `integration_config`;
DROP TABLE IF EXISTS `openlineage_event`;
-- export_task 变更回滚：恢复 asset_id NOT NULL（在切片 03 DROP export_task 之前执行）
ALTER TABLE `export_task` MODIFY `asset_id` varchar(36) NOT NULL COMMENT '影响分析源资产';

-- =====================================================================
-- 数据中台元数据发现管理平台（MP-SLICE-01 / WU-01-03）回滚脚本
-- 依据：db/schema.sql 追加的元数据平台表（数据架构草案 §5）
-- 说明：按依赖逆序 DROP；模板演示表 t_user / leaf_alloc 已从 schema.sql 移除。
-- =====================================================================

-- MP-SLICE-04 / WU-04-DB 追加回滚（顺序：propagate_task 最前，无被依赖方）
DROP TABLE IF EXISTS `propagate_task`;

-- MP-SLICE-03 / WU-03-DB 追加回滚（顺序：export_task 最前，无被依赖方）
DROP TABLE IF EXISTS `export_task`;

-- MP-SLICE-02 / WU-02-DB 追加回滚（顺序：asset_tag、asset_favorite，先于 asset）
DROP TABLE IF EXISTS `asset_tag`;
DROP TABLE IF EXISTS `asset_favorite`;

DROP TABLE IF EXISTS `audit_log`;
DROP TABLE IF EXISTS `role_domain`;
DROP TABLE IF EXISTS `role`;
DROP TABLE IF EXISTS `data_domain`;
DROP TABLE IF EXISTS `class_rule`;
DROP TABLE IF EXISTS `classification`;
DROP TABLE IF EXISTS `openlineage_event`;
DROP TABLE IF EXISTS `integration_config`;
DROP TABLE IF EXISTS `ai_ask_log`;
DROP TABLE IF EXISTS `dq_root_cause_record`;
ALTER TABLE `asset` DROP COLUMN `taint_status`;

DROP TABLE IF EXISTS `lineage_edge`;
DROP TABLE IF EXISTS `asset_version`;
DROP TABLE IF EXISTS `asset_column`;
DROP TABLE IF EXISTS `asset`;
DROP TABLE IF EXISTS `collector_task`;
DROP TABLE IF EXISTS `data_source`;
