-- =====================================================================
-- SL-SLICE-01-WU-05 迁移脚本：semantic_layer 独立 schema 建表
-- 依据：数据架构 §5 物理模型 + §11 Router 映射（01 主导写 term/term_alias/audit_log）
-- 表归属互斥：term/term_alias -> SL-SLICE-01；audit_log -> SL-SLICE-06 横切（01 建表）
-- 人工确认项：DDL / 迁移 / 回滚约束（见 Execution Result WU-05 待评审清单）
-- 说明：
--   1) id 统一为 BIGINT（mybatis-plus id-type=assign_id 雪花，YSS 分布式 id 基线）
--   2) 审计日志表 audit_log 为不可变只追加存储，无 UPDATE/DELETE 路径（应用层保证）
--   3) 仅创建本切片表；metric_definition / metric_version / synonym_set /
--      synonym_word / attachment 分别由 SL-SLICE-02/03/04 建表，禁止越界建表
-- 数据库：MySQL 8（utf8mb4）
-- =====================================================================

-- 独立 schema（应用基线：semantic_layer，见 implementation-repo-registry-backend §5 / 数据架构 §2）
CREATE DATABASE IF NOT EXISTS `semantic_layer`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `semantic_layer`;

-- ---------------------------------------------------------------------
-- term：术语（术语库聚合根，SL-001）
-- 物理删除仅草稿且未被挂接 / 未被同义词组关联（409 阻断，改用弃用）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `term` (
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花 id）',
    `name`           VARCHAR(200) NOT NULL COMMENT '术语名称（全局唯一）',
    `definition`     TEXT         NOT NULL COMMENT '术语定义',
    `description`    VARCHAR(1000) DEFAULT NULL COMMENT '描述',
    `owner`          VARCHAR(100) NOT NULL COMMENT '负责人（SB-01：语义对象自带，创建必填）',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'draft' COMMENT '状态：draft / certified / deprecated',
    `certified_by`   VARCHAR(100)  DEFAULT NULL COMMENT '认证人',
    `certified_at`   DATETIME      DEFAULT NULL COMMENT '认证时间',
    `deprecated_by`  VARCHAR(100)  DEFAULT NULL COMMENT '弃用人',
    `deprecated_at`  DATETIME      DEFAULT NULL COMMENT '弃用时间',
    `synonym_set_id` BIGINT        DEFAULT NULL COMMENT '关联同义词组 id（0..1，SL-SLICE-03 单侧持有写）',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（WHERE version=? 条件更新）',
    `created_by`     VARCHAR(100) NOT NULL COMMENT '创建人',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_name` (`name`),
    UNIQUE KEY `uk_term_synonym_set_id` (`synonym_set_id`),
    KEY `idx_term_status` (`status`),
    KEY `idx_term_updated_at` (`updated_at` DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '术语（术语库聚合根）';

-- ---------------------------------------------------------------------
-- term_alias：术语别名（keyword 检索来源；与 term 同属一个聚合）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `term_alias` (
    `id`      BIGINT       NOT NULL COMMENT '主键（雪花 id）',
    `term_id` BIGINT       NOT NULL COMMENT '术语 id',
    `alias`   VARCHAR(200) NOT NULL COMMENT '别名',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_alias` (`term_id`, `alias`),
    KEY `idx_term_alias_term_id` (`term_id`),
    KEY `idx_term_alias_alias` (`alias`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '术语别名';

-- ---------------------------------------------------------------------
-- audit_log：审计日志（不可变只追加；与业务写操作同事务；SL-SLICE-06 横切查询）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id`          BIGINT        NOT NULL COMMENT '主键（雪花 id）',
    `operator`    VARCHAR(100)  NOT NULL COMMENT '操作者（当前用户，system 兜底）',
    `action`      VARCHAR(50)   NOT NULL COMMENT '动作：CREATE/UPDATE/CERTIFY/DEPRECATE/DELETE/PERMISSION_DENIED',
    `object_type` VARCHAR(50)   NOT NULL COMMENT '对象类型：term',
    `object_id`   BIGINT         DEFAULT NULL COMMENT '对象 id（被拒绝操作可为空，如只读用户新建被拒）',
    `note`        VARCHAR(1000)  DEFAULT NULL COMMENT '操作备注',
    `result`      VARCHAR(50)   NOT NULL COMMENT '结果：SUCCESS / DENIED',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审计时间',
    PRIMARY KEY (`id`),
    KEY `idx_audit_created_at` (`created_at` DESC),
    KEY `idx_audit_object` (`object_type`, `object_id`),
    KEY `idx_audit_operator` (`operator`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '审计日志（不可变只追加，与写操作同事务）';
