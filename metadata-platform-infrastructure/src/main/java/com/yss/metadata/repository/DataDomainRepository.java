package com.yss.metadata.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.metadata.repository.entity.DataDomainPO;

/**
 * 数据域持久化仓库（MyBatis-Plus，BasePlusRepository 接入；name 唯一）。
 */
public interface DataDomainRepository extends BasePlusRepository<DataDomainPO> {
}
