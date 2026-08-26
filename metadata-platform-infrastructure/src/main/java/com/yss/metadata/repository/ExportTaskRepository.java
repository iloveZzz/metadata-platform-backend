package com.yss.metadata.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.metadata.repository.entity.ExportTaskPO;

/**
 * 导出任务持久化仓库（MyBatis-Plus，BasePlusRepository 接入）。
 */
public interface ExportTaskRepository extends BasePlusRepository<ExportTaskPO> {
}
