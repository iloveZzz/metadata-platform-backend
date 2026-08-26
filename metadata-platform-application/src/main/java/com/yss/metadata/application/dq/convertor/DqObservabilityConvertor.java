package com.yss.metadata.application.dq.convertor;

import com.yss.metadata.client.vo.BlastRadiusAssetVO;
import com.yss.metadata.client.vo.BlastRadiusVO;
import com.yss.metadata.client.vo.PropagationStepVO;
import com.yss.metadata.client.vo.RootCauseNodeVO;
import com.yss.metadata.client.vo.RootCauseVO;
import com.yss.metadata.domain.dq.model.BlastRadiusAsset;
import com.yss.metadata.domain.dq.model.BlastRadiusReport;
import com.yss.metadata.domain.dq.model.PropagationStep;
import com.yss.metadata.domain.dq.model.RootCauseNode;
import com.yss.metadata.domain.dq.model.RootCauseReport;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 质量可观测性领域对象到 VO 的 MapStruct 转换器
 *
 * @author ai
 * @since 2026-08-15
 */
@Mapper(config = MapStructAppConfig.class)
public interface DqObservabilityConvertor {

    @Mapping(target = "createdAt", expression = "java(formatDate(report.getCreatedAt()))")
    RootCauseVO toRootCauseVO(RootCauseReport report);

    RootCauseNodeVO toRootCauseNodeVO(RootCauseNode node);

    PropagationStepVO toPropagationStepVO(PropagationStep step);

    BlastRadiusVO toBlastRadiusVO(BlastRadiusReport report);

    BlastRadiusAssetVO toBlastRadiusAssetVO(BlastRadiusAsset asset);

    default String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
