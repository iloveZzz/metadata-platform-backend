package com.yss.datamiddle.dqinsight.infrastructure.convertor;

import com.yss.datamiddle.dqinsight.domain.model.ChannelState;
import com.yss.datamiddle.dqinsight.domain.model.ChannelType;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import com.yss.datamiddle.dqinsight.repository.entity.DqChannelPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 接入通道转换（IngestionChannel ↔ DqChannelPO，MapStruct，C12）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface DqChannelConvertor {

    DqChannelPO toPO(IngestionChannel channel);

    @ObjectFactory
    default IngestionChannel createIngestionChannel() {
        return IngestionChannel.forPersistenceLoad();
    }

    IngestionChannel toDomain(DqChannelPO po);

    default String channelTypeToString(ChannelType value) {
        return value == null ? null : value.getCode();
    }

    default ChannelType stringToChannelType(String value) {
        return ChannelType.fromCodeOrNull(value);
    }

    default String channelStateToString(ChannelState value) {
        return value == null ? null : value.getCode();
    }

    default ChannelState stringToChannelState(String value) {
        return ChannelState.fromCodeOrNull(value);
    }

    default String formatTypeToString(FormatType value) {
        return value == null ? null : value.getCode();
    }

    default FormatType stringToFormatType(String value) {
        return FormatType.fromCodeOrNull(value);
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
}
