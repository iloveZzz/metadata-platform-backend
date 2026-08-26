package com.yss.datamiddle.smartgovernance.infrastructure.repository.mapper;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.po.MetricConflictPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetricConflictMapper extends BasePlusRepository<MetricConflictPO> {
}
