package com.yss.metadata.application.integration.service.convertor;

import com.yss.metadata.client.vo.DataHubConfigVO;
import com.yss.metadata.client.vo.GravitinoConfigVO;
import com.yss.metadata.client.vo.IntegrationVO;
import com.yss.metadata.client.vo.OpenLineageConfigVO;
import com.yss.metadata.domain.integration.model.IntegrationConfig;
import com.yss.metadata.domain.integration.model.OpenLineageStats;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Locale;

/**
 * 集成域应用转换器（MapStruct；Domain → VO；组合 VO 默认方法）。
 */
@Mapper(config = MapStructAppConfig.class)
public interface IntegrationAppConvertor {

    @Mapping(source = "gravitinoEndpoint", target = "endpoint")
    @Mapping(source = "gravitinoEnabled", target = "enabled")
    @Mapping(source = "gravitinoLastTest", target = "lastTest")
    GravitinoConfigVO toGravitinoVO(IntegrationConfig config);

    @Mapping(source = "datahubEndpoint", target = "endpoint")
    DataHubConfigVO toDatahubVO(IntegrationConfig config);

    /**
     * 组合 VO：Gravitino/DataHub 配置（无配置空结构非错误）+ OpenLineage 接收端点与统计。
     */
    default IntegrationVO toVO(IntegrationConfig config, String receiveEndpoint, OpenLineageStats stats) {
        IntegrationVO vo = new IntegrationVO();
        if (config != null) {
            vo.setGravitino(toGravitinoVO(config));
            vo.setDatahub(toDatahubVO(config));
        } else {
            vo.setGravitino(new GravitinoConfigVO());
            vo.setDatahub(new DataHubConfigVO());
        }
        OpenLineageConfigVO ol = new OpenLineageConfigVO();
        ol.setReceiveEndpoint(receiveEndpoint);
        ol.setRecent24h(stats == null ? 0 : stats.getRecent24hCount());
        ol.setParseSuccessRate(formatRate(stats));
        vo.setOpenLineage(ol);
        return vo;
    }

    /** 解析成功率（0~1 → 百分比字符串，1 位小数；0 事件返回空）。 */
    default String formatRate(OpenLineageStats stats) {
        if (stats == null || (stats.getRecent24hCount() == 0 && stats.getParseSuccessRate() == 0.0)) {
            return null;
        }
        return String.format(Locale.ROOT, "%.1f%%", stats.getParseSuccessRate() * 100);
    }
}
