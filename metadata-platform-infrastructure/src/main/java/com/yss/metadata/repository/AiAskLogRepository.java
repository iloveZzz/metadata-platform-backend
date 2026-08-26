package com.yss.metadata.repository;

import com.yss.metadata.repository.entity.AiAskLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 智能找数审计 Repository
 *
 * @author ai
 * @since 2026-08-15
 */
@Mapper
public interface AiAskLogRepository extends com.baomidou.mybatisplus.core.mapper.BaseMapper<AiAskLogPO> {
}
