package com.yss.datamiddle.aicontextlayer.infrastructure.convertor;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;
import com.yss.datamiddle.aicontextlayer.repository.entity.McpSessionPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * {@link McpSession} 领域模型 ↔ {@link McpSessionPO} 转换（MapStruct）。
 *
 * <p>标识映射：mcp_session 表主键 {@code id} 即会话 ID（数据架构 §5 / DDL 注释），
 * 领域模型以 {@code sessionId} 表达，故 PO.id ↔ 领域 sessionId 显式互映。</p>
 *
 * <p>时间口径：与 {@link AgentCredentialConvertor} 一致，统一按 Asia/Shanghai
 * （bootstrap 数据源 serverTimezone）换算。</p>
 *
 * <p>状态映射：McpSessionStatus 枚举 ↔ status 字符串按枚举名自动映射。</p>
 */
@Mapper(config = MapStructInfraConfig.class)
public interface McpSessionConvertor {

    ZoneId TIME_ZONE = ZoneId.of("Asia/Shanghai");

    @Mapping(target = "id", source = "sessionId")
    McpSessionPO toPO(McpSession source);

    @Mapping(target = "sessionId", source = "id")
    McpSession toDomain(McpSessionPO source);

    List<McpSession> toDomainList(List<McpSessionPO> source);

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, TIME_ZONE);
    }

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(TIME_ZONE).toInstant();
    }
}
