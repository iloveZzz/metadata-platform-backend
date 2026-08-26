package com.yss.datamiddle.smartgovernance.infrastructure.repository.mapper;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.po.SensitiveCandidatePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SensitiveCandidateMapper extends BasePlusRepository<SensitiveCandidatePO> {
}
