-- H2 内存库测试 DDL（镜像生产 db/schema.sql 的业务表；截至切片 04 含 data_source /
-- collector_task / asset / asset_favorite / asset_tag / asset_column / asset_version /
-- lineage_edge / export_task / audit_log / classification / class_rule / propagate_task）。
-- 生产 MySQL DDL 见 db/schema.sql；此处为 H2 兼容写法（无 ENGINE/CHARSET 子句）。
CREATE TABLE IF NOT EXISTS data_source (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL,
  host VARCHAR(255) NOT NULL,
  port INT NOT NULL,
  dialect VARCHAR(32) NOT NULL,
  username VARCHAR(128),
  cred_ref VARCHAR(512),
  auto_classify BOOLEAN DEFAULT TRUE,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_data_source_name ON data_source (name);

CREATE TABLE IF NOT EXISTS collector_task (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  connector_id VARCHAR(36) NOT NULL,
  schedule VARCHAR(128) NOT NULL,
  mode VARCHAR(32) NOT NULL,
  strategy VARCHAR(32) NOT NULL,
  auto_classify BOOLEAN DEFAULT TRUE,
  status VARCHAR(32) NOT NULL,
  fail_reason VARCHAR(1024),
  last_run_at TIMESTAMP,
  owner VARCHAR(64),
  description VARCHAR(1000),
  enabled BOOLEAN DEFAULT TRUE,
  datasource_type VARCHAR(64),
  source_system VARCHAR(64),
  scope_type VARCHAR(32) DEFAULT 'all',
  selected_databases TEXT,
  retry_enabled BOOLEAN DEFAULT TRUE,
  retry_count INT DEFAULT 1,
  retry_interval INT DEFAULT 5,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_collector_task_source_schedule ON collector_task (connector_id, schedule);

CREATE TABLE IF NOT EXISTS collector_instance (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  collector_id VARCHAR(64),
  collector_name VARCHAR(128),
  connector_id VARCHAR(64),
  connector_name VARCHAR(128),
  datasource_type VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  execution_mode VARCHAR(32) NOT NULL,
  schedule_description VARCHAR(128),
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  duration_ms BIGINT,
  executor VARCHAR(64),
  owner VARCHAR(64),
  error_message TEXT,
  is_dry_run BOOLEAN DEFAULT FALSE,
  retry_count INT DEFAULT 0,
  max_retries INT DEFAULT 3,
  workflow_nodes TEXT,
  diff_summary TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- 元数据资产表（目录域；WU-01-03 资产入库）
CREATE TABLE IF NOT EXISTS asset (
  id VARCHAR(36) PRIMARY KEY,
  source_id VARCHAR(36) NOT NULL,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(32) NOT NULL,
  domain VARCHAR(64),
  owner VARCHAR(128),
  classification VARCHAR(64),
  database_name VARCHAR(100),
  source_system VARCHAR(100),
  collector_task_id VARCHAR(36),
  is_excluded BOOLEAN DEFAULT FALSE,
  taint_status VARCHAR(20) DEFAULT 'NORMAL',
  status VARCHAR(32) NOT NULL,
  version VARCHAR(64),
  description VARCHAR(1024),
  row_count BIGINT,
  storage_size VARCHAR(64),
  updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_asset_source ON asset (source_id);
CREATE INDEX IF NOT EXISTS idx_asset_source_db ON asset (source_id, database_name);
CREATE INDEX IF NOT EXISTS idx_asset_source_system ON asset (source_system);
CREATE INDEX IF NOT EXISTS idx_asset_is_excluded ON asset (is_excluded);

-- 资产列（明细）表
CREATE TABLE IF NOT EXISTS asset_column (
  id VARCHAR(36) PRIMARY KEY,
  asset_id VARCHAR(36) NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(64),
  comment VARCHAR(512),
  pk BOOLEAN DEFAULT FALSE,
  ordinal_position INT,
  classification VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_asset_column_asset ON asset_column (asset_id);

-- 资产版本表（变更快照，可回溯）
CREATE TABLE IF NOT EXISTS asset_version (
  id VARCHAR(36) PRIMARY KEY,
  asset_id VARCHAR(36) NOT NULL,
  version INT NOT NULL,
  schema_diff VARCHAR(2048),
  created_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_asset_version ON asset_version (asset_id, version);

-- 用户收藏表（切片 02 新增；复合主键 (asset_id, user_id)，幂等切换）
CREATE TABLE IF NOT EXISTS asset_favorite (
  asset_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP,
  PRIMARY KEY (asset_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_asset_favorite_user ON asset_favorite (user_id, asset_id);

-- 资产标签表（切片 02 新增；复合主键 (asset_id, tag)，覆盖式更新）
CREATE TABLE IF NOT EXISTS asset_tag (
  asset_id VARCHAR(36) NOT NULL,
  tag VARCHAR(64) NOT NULL,
  PRIMARY KEY (asset_id, tag)
);

CREATE INDEX IF NOT EXISTS idx_asset_tag ON asset_tag (asset_id);

-- 血缘边表（切片 01 既有表；H2 测试 DDL 于切片 03 补齐，邻接表，影响分析递归 CTE；生产 DDL 见 db/schema.sql，勿重复建）
CREATE TABLE IF NOT EXISTS lineage_edge (
  id VARCHAR(36) PRIMARY KEY,
  from_asset VARCHAR(36) NOT NULL,
  to_asset VARCHAR(36) NOT NULL,
  from_column_id VARCHAR(36),
  to_column_id VARCHAR(36),
  transform_expr VARCHAR(1024),
  expr_type VARCHAR(32) NOT NULL DEFAULT 'DIRECT',
  type VARCHAR(32) NOT NULL,
  confidence VARCHAR(32) NOT NULL,
  remark VARCHAR(512),
  graph_version VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_lineage_from ON lineage_edge (from_asset);
CREATE INDEX IF NOT EXISTS idx_lineage_to ON lineage_edge (to_asset);
CREATE INDEX IF NOT EXISTS idx_lineage_col_from ON lineage_edge (from_column_id, to_column_id);
CREATE INDEX IF NOT EXISTS idx_lineage_col_to ON lineage_edge (to_column_id, from_column_id);
CREATE INDEX IF NOT EXISTS idx_lineage_asset_col_pair ON lineage_edge (from_asset, from_column_id);

-- 导出异步任务表（切片 03 新增；影响分析 CSV/JSON 导出；幂等=同资产同格式进行中任务复用）
CREATE TABLE IF NOT EXISTS export_task (
  id VARCHAR(36) PRIMARY KEY,
  asset_id VARCHAR(36),
  format VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'pending',
  file_ref VARCHAR(512),
  operator VARCHAR(64),
  created_at TIMESTAMP,
  finished_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_export_asset_status ON export_task (asset_id, status);

-- 审计日志表（切片 03 基础写入；只读不可变）
CREATE TABLE IF NOT EXISTS audit_log (
  id VARCHAR(36) PRIMARY KEY,
  operator VARCHAR(128),
  action VARCHAR(64) NOT NULL,
  object VARCHAR(128),
  result VARCHAR(16),
  time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_log_time ON audit_log (time);

-- 数据域表（切片 06 新增；角色绑定按名称幂等 upsert；owner 列镜像生产 schema）
CREATE TABLE IF NOT EXISTS data_domain (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  owner VARCHAR(128)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_data_domain_name ON data_domain (name);

-- 角色表（切片 06 新增；name 唯一）
CREATE TABLE IF NOT EXISTS role (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  scope VARCHAR(64)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_name ON role (name);

-- 角色-数据域关联表（切片 06 新增；N:M 联合主键）
CREATE TABLE IF NOT EXISTS role_domain (
  role_id VARCHAR(36) NOT NULL,
  domain_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (role_id, domain_id)
);

CREATE INDEX IF NOT EXISTS idx_role_domain_domain ON role_domain (domain_id);

-- 分级分类结果表（切片 04 新增；候选自动识别 + 确认/修正；状态 pending/confirmed/corrected）
CREATE TABLE IF NOT EXISTS classification (
  id VARCHAR(36) PRIMARY KEY,
  asset_id VARCHAR(36),
  column_id VARCHAR(36),
  name VARCHAR(128) NOT NULL,
  level VARCHAR(32),
  source VARCHAR(32),
  status VARCHAR(32) NOT NULL DEFAULT 'pending'
);

CREATE INDEX IF NOT EXISTS idx_classification_asset ON classification (asset_id);

-- 分类规则表（切片 04 新增；内置规则 + 自定义正则/列名/字典；启停审计）
CREATE TABLE IF NOT EXISTS class_rule (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL,
  pattern VARCHAR(1024),
  enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_class_rule_enabled ON class_rule (enabled);

-- 分类传播异步任务表（切片 04 新增；同版本只跑一次幂等；覆盖范围可核验）
CREATE TABLE IF NOT EXISTS propagate_task (
  id VARCHAR(36) PRIMARY KEY,
  classification_id VARCHAR(36) NOT NULL,
  version VARCHAR(64),
  status VARCHAR(16) NOT NULL DEFAULT 'pending',
  coverage VARCHAR(512),
  operator VARCHAR(64),
  created_at TIMESTAMP,
  finished_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_propagate_class_status ON propagate_task (classification_id, status);

-- 集成配置表（切片 05 新增；单例行 id=1；Gravitino/DataHub 集成配置）
CREATE TABLE IF NOT EXISTS integration_config (
  id VARCHAR(36) PRIMARY KEY,
  gravitino_endpoint VARCHAR(512),
  gravitino_auth_ref VARCHAR(512),
  gravitino_enabled BOOLEAN DEFAULT FALSE,
  gravitino_last_test VARCHAR(512),
  datahub_endpoint VARCHAR(512),
  datahub_auth_ref VARCHAR(512),
  updated_at TIMESTAMP
);

-- OpenLineage 事件接收记录表（切片 05 新增；事件统计近 24h/解析成功率依据）
CREATE TABLE IF NOT EXISTS openlineage_event (
  id VARCHAR(36) PRIMARY KEY,
  event_type VARCHAR(16) NOT NULL,
  event_time TIMESTAMP,
  run_id VARCHAR(64),
  job_namespace VARCHAR(256),
  job_name VARCHAR(256),
  parse_status VARCHAR(16) NOT NULL DEFAULT 'received',
  received_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_openlineage_received ON openlineage_event (received_at);

-- AI 智能找数审计表（切片 07 新增）
CREATE TABLE IF NOT EXISTS ai_ask_log (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(64),
  query_text VARCHAR(512) NOT NULL,
  matched_asset_ids VARCHAR(512),
  confidence_score VARCHAR(32),
  model_name VARCHAR(64),
  created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_ask_created ON ai_ask_log (created_at);

-- 资产存疑状态与质量根因溯源表（切片 08 新增）
ALTER TABLE asset ADD COLUMN IF NOT EXISTS taint_status VARCHAR(20) DEFAULT 'NORMAL';

CREATE TABLE IF NOT EXISTS dq_root_cause_record (
  id VARCHAR(36) PRIMARY KEY,
  target_asset_id VARCHAR(36) NOT NULL,
  root_asset_id VARCHAR(36) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  actual_metric VARCHAR(128),
  threshold VARCHAR(128),
  confidence VARCHAR(32),
  fault_time VARCHAR(32),
  operator VARCHAR(64),
  created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rc_target ON dq_root_cause_record (target_asset_id);

-- db-scheduler 分布式任务调度队列表（H2 内存库镜像）
CREATE TABLE IF NOT EXISTS scheduled_tasks (
  task_name VARCHAR(100) NOT NULL,
  task_instance VARCHAR(100) NOT NULL,
  task_data BLOB,
  execution_time TIMESTAMP(6) NOT NULL,
  picked BOOLEAN DEFAULT FALSE,
  picked_by VARCHAR(50),
  last_success TIMESTAMP(6),
  last_failure TIMESTAMP(6),
  consecutive_failures INT DEFAULT 0,
  last_heartbeat TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (task_name, task_instance)
);

CREATE INDEX IF NOT EXISTS idx_scheduled_tasks_execution_time ON scheduled_tasks (execution_time);
CREATE INDEX IF NOT EXISTS idx_scheduled_tasks_last_heartbeat ON scheduled_tasks (last_heartbeat);
