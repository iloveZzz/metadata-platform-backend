-- =====================================================================
-- SL-SLICE-01-WU-05 约束校验证据脚本（information_schema 查询）
-- 人工确认项：建表后执行，验证 UNIQUE / 索引 / 列完整性
-- 执行：USE semantic_layer 后执行各 SELECT；输出即验证证据
-- =====================================================================
USE `semantic_layer`;

-- 1) 三张表存在性
SELECT table_name, engine, table_collation
FROM information_schema.tables
WHERE table_schema = 'semantic_layer'
  AND table_name IN ('term', 'term_alias', 'audit_log')
ORDER BY table_name;

-- 2) term 唯一约束：UNIQUE(name)、UNIQUE(synonym_set_id)
SELECT table_name, index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns,
       non_unique
FROM information_schema.statistics
WHERE table_schema = 'semantic_layer' AND table_name = 'term'
GROUP BY table_name, index_name, non_unique
ORDER BY index_name;

-- 3) term 索引：idx(status)、idx(updated_at DESC)
SELECT index_name, column_name, seq_in_index, collation
FROM information_schema.statistics
WHERE table_schema = 'semantic_layer' AND table_name = 'term'
  AND index_name IN ('idx_term_status', 'idx_term_updated_at')
ORDER BY index_name, seq_in_index;

-- 4) term_alias 唯一约束：UNIQUE(term_id, alias) + 索引
SELECT table_name, index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns,
       non_unique
FROM information_schema.statistics
WHERE table_schema = 'semantic_layer' AND table_name = 'term_alias'
GROUP BY table_name, index_name, non_unique
ORDER BY index_name;

-- 5) audit_log 索引：idx(created_at DESC)、idx(object_type, object_id)、idx(operator)
SELECT table_name, index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns,
       non_unique
FROM information_schema.statistics
WHERE table_schema = 'semantic_layer' AND table_name = 'audit_log'
GROUP BY table_name, index_name, non_unique
ORDER BY index_name;

-- 6) 关键列完整性（NOT NULL / 类型）
SELECT table_name, column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'semantic_layer'
  AND table_name IN ('term', 'term_alias', 'audit_log')
  AND column_name IN ('id', 'name', 'status', 'version', 'term_id', 'alias',
                      'operator', 'action', 'object_type', 'result')
ORDER BY table_name, column_name;
