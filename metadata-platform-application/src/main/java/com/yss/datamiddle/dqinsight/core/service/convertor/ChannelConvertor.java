package com.yss.datamiddle.dqinsight.core.service.convertor;

import com.yss.datamiddle.dqinsight.client.vo.ChannelVO;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import com.yss.datamiddle.dqinsight.domain.util.IsoTimes;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;

import java.time.Instant;

/**
 * 通道转换（IngestionChannel → ChannelVO，MapStruct，C12；认证密文不回传，仅 authConfigured）。
 */
@Mapper(config = MapStructAppConfig.class)
public interface ChannelConvertor {

    ChannelVO toVO(IngestionChannel channel);

    default String longToString(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    default String instantToString(Instant value) {
        return value == null ? null : IsoTimes.format(value);
    }
}
