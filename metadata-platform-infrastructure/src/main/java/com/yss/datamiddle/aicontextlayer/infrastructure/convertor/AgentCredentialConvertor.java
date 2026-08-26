package com.yss.datamiddle.aicontextlayer.infrastructure.convertor;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;
import com.yss.datamiddle.aicontextlayer.repository.entity.AgentCredentialPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * {@link AgentCredential} 领域模型 ↔ {@link AgentCredentialPO} 转换（MapStruct）。
 *
 * <p>时间口径：领域层使用 {@link Instant}（UTC 绝对时刻），存储层使用数据库
 * {@code datetime}（本地时区墙钟时间）。为与 bootstrap 数据源配置
 * {@code serverTimezone=Asia/Shanghai} 保持一致，统一按 Asia/Shanghai 时区换算，
 * 保证同一时刻写入 / 读取往返一致。</p>
 *
 * <p>状态映射：CredentialStatus 枚举 ↔ status 字符串按枚举名自动映射。</p>
 *
 * <p>安全（SEC-05）：credentialRef 为 KMS 密文引用，本转换不做任何解密 / 明文处理。</p>
 */
@Mapper(config = MapStructInfraConfig.class)
public interface AgentCredentialConvertor {

    ZoneId TIME_ZONE = ZoneId.of("Asia/Shanghai");

    AgentCredentialPO toPO(AgentCredential source);

    AgentCredential toDomain(AgentCredentialPO source);

    List<AgentCredential> toDomainList(List<AgentCredentialPO> source);

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, TIME_ZONE);
    }

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(TIME_ZONE).toInstant();
    }
}
