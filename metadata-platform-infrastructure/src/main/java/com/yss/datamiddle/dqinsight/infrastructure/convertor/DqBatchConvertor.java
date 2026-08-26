package com.yss.datamiddle.dqinsight.infrastructure.convertor;

import com.yss.datamiddle.dqinsight.client.vo.IngestionRecordVO;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionStatus;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import com.yss.datamiddle.dqinsight.domain.util.IsoTimes;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 批次转换（DQResultBatch ↔ DqBatchPO → IngestionRecordVO，MapStruct）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface DqBatchConvertor {

    DqBatchPO toPO(DQResultBatch batch);

    @Mapping(source = "id", target = "batchId")
    IngestionRecordVO toIngestionRecordVO(DqBatchPO po);

    @ObjectFactory
    default DQResultBatch createBatch() {
        return DQResultBatch.forPersistenceLoad();
    }

    DQResultBatch toDomain(DqBatchPO po);

    default String sourceToolToString(SourceTool value) {
        return value == null ? null : value.getCode();
    }

    default SourceTool stringToSourceTool(String value) {
        return SourceTool.fromCodeOrNull(value);
    }

    default String formatTypeToString(FormatType value) {
        return value == null ? null : value.getCode();
    }

    default FormatType stringToFormatType(String value) {
        return FormatType.fromCodeOrNull(value);
    }

    default String ingestionStatusToString(IngestionStatus value) {
        return value == null ? null : value.getCode();
    }

    default IngestionStatus stringToIngestionStatus(String value) {
        return IngestionStatus.fromCodeOrNull(value);
    }

    default String linkageStateToString(LinkageState value) {
        return value == null ? null : value.getCode();
    }

    default LinkageState stringToLinkageState(String value) {
        return LinkageState.fromCodeOrNull(value);
    }

    default String errorCategoryToString(ErrorCategory value) {
        return value == null ? null : value.getCode();
    }

    default ErrorCategory stringToErrorCategory(String value) {
        return ErrorCategory.fromCodeOrNull(value);
    }

    default LocalDateTime instantToLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    default Instant localDateTimeToInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    default String localDateTimeToString(LocalDateTime value) {
        return value == null ? null : IsoTimes.format(value.atZone(ZoneId.systemDefault()).toInstant());
    }
}
