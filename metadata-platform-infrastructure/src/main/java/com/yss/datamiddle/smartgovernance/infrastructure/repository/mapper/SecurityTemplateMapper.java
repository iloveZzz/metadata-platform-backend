package com.yss.datamiddle.smartgovernance.infrastructure.repository.mapper;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.po.SecurityTemplatePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SecurityTemplateMapper extends BasePlusRepository<SecurityTemplatePO> {
}
