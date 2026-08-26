package com.yss.datamiddle.dqinsight.infrastructure.convertor;

import com.yss.datamiddle.dqinsight.client.vo.AuditLogVO;
import com.yss.datamiddle.dqinsight.domain.model.AuditAction;
import com.yss.datamiddle.dqinsight.domain.model.AuditLogEntry;
import com.yss.datamiddle.dqinsight.domain.model.AuditResult;
import com.yss.datamiddle.dqinsight.domain.util.IsoTimes;
import com.yss.datamiddle.dqinsight.repository.entity.DqAuditLogPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 审计记录转换（AuditLogEntry → DqAuditLogPO → AuditLogVO，MapStruct；仅 INSERT + 只读查询）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface DqAuditLogConvertor {

    DqAuditLogPO toPO(AuditLogEntry entry);

    /** 查询投影（PO → VO；eventTime → time ISO 8601，冻结契约 AuditLogEntry 字段名 time） */
    @Mapping(source = "eventTime", target = "time")
    AuditLogVO toVO(DqAuditLogPO po);

    default String auditActionToString(AuditAction value) {
        return value == null ? null : value.getCode();
    }

    default AuditAction stringToAuditAction(String value) {
        return AuditAction.fromCode(value);
    }

    default String auditResultToString(AuditResult value) {
        return value == null ? null : value.getCode();
    }

    default AuditResult stringToAuditResult(String value) {
        return AuditResult.fromCode(value);
    }

    default LocalDateTime instantToLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    default String localDateTimeToIso(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return IsoTimes.format(value.atZone(ZoneId.systemDefault()).toInstant());
    }
}
