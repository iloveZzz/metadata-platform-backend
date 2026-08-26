package com.yss.datamiddle.dqinsight.core.service.convertor;

import com.yss.datamiddle.dqinsight.client.vo.IngestionReceiptVO;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 接入回执转换（DQResultBatch → IngestionReceiptVO，MapStruct）。
 */
@Mapper(config = MapStructAppConfig.class)
public interface IngestionReceiptConvertor {

    @Mapping(source = "id", target = "batchId")
    @Mapping(target = "receivedAt",
            expression = "java(com.yss.datamiddle.dqinsight.domain.util.IsoTimes.format(batch.getReceivedAt()))")
    IngestionReceiptVO toVO(DQResultBatch batch);
}
