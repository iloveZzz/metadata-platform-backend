# metadata-platform API

> 本文件为仓库内快速索引；**接口契约以冻结 OpenAPI 为唯一权威**：
> `docs/.scratch/metadata-platform/api/metadata-platform.yaml`（frontend 工具消费副本：`app/frontend/openapi/metadata-platform.yaml`）。

## 端点清单（截至切片 03）

| 域 | 端点 | 说明 |
|---|---|---|
| 连接器 | GET/POST `/api/connectors`、GET/PUT/DELETE `/api/connectors/{id}`、POST `/api/connectors/{id}/test` | CRUD + 测试连接（network/credential/dialect 分类；删除引用冲突 409） |
| 采集任务 | GET/POST `/api/collectors`、PUT `/api/collectors/{id}`、POST `/api/collectors/run`、POST `/api/collectors/{id}/cancel`、POST `/api/collectors/{id}/retry` | 全生命周期（运行中幂等/取消/局部重采） |
| 资产目录 | GET `/api/assets`、GET `/api/assets/{id}`、POST `/api/assets/{id}/favorite`、POST `/api/assets/{id}/claim`、PUT `/api/assets/{id}/tags`、POST `/api/assets/{id}/archive`、POST `/api/assets/{id}/unarchive` | 搜索（列级命中/筛选/排序/分页）、详情聚合、收藏/认领/标签/归档 |
| 血缘 | GET `/api/assets/{id}/lineage`、POST `/api/lineage/manual` | 图谱（confidence 筛选/空血缘）、人工补录（成环 CYCLE 409 定位冲突边 / 版本 token CONFLICT 409） |
| 影响分析 | GET `/api/assets/{id}/impact-analysis`、GET `/api/assets/{id}/impact-analysis/export` | 下游全量召回（深度分组/sortBy）、导出（202 异步幂等 + 审计） |

## 生成

- 契约一致性以冻结 OpenAPI + Web 层契约测试（`AssetControllerTest` / `ConnectorControllerTest` / `CollectorControllerTest` / `LineageControllerTest` / `ImpactControllerTest`）为准。
- smart-doc 插件坐标（pom `smart-doc-maven-plugin:yss-4.0.0`）在本环境无法解析（m2 仅有 `com.ly.smart-doc:3.1.2`），生成任务待脚手架维护（记录于 implementation-repo-registry-backend.md §4）。
