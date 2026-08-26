package com.yss.metadata.application.asset.service.convertor;

import com.yss.metadata.client.vo.AssetColumnVO;
import com.yss.metadata.client.vo.AssetDetailVO;
import com.yss.metadata.client.vo.AssetPageVO;
import com.yss.metadata.client.vo.AssetVersionVO;
import com.yss.metadata.client.vo.AssetVO;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetSearchResult;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.asset.model.AssetVersion;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 资产对象转换器（MapStruct；禁止 BeanUtils.copyProperties 或手写字段映射）。
 *
 * <p>Domain → VO（列表/详情/字段/版本/分页组装）；状态枚举 → 字符串
 * （与冻结 OpenAPI 枚举 value 一致）。</p>
 */
@Mapper(config = MapStructAppConfig.class)
public interface AssetAppConvertor {

    /**
     * 资产 → 列表视图对象（sourceName → source 数据源名称）。
     */
    @Mapping(source = "sourceName", target = "source")
    AssetVO toVO(Asset asset);

    /**
     * 资产列表 → 视图对象列表。
     */
    List<AssetVO> toVOList(List<Asset> assets);

    /**
     * 资产 → 详情视图对象（tags/columns/versions 由用例组装后填充）。
     */
    @Mapping(source = "sourceName", target = "source")
    AssetDetailVO toDetailVO(Asset asset);

    /**
     * 字段 → 字段视图对象。
     */
    AssetColumnVO toColumnVO(AssetColumn column);

    /**
     * 字段列表 → 视图对象列表。
     */
    List<AssetColumnVO> toColumnVOList(List<AssetColumn> columns);

    /**
     * 版本 → 版本视图对象。
     */
    AssetVersionVO toVersionVO(AssetVersion version);

    /**
     * 版本列表 → 视图对象列表。
     */
    List<AssetVersionVO> toVersionVOList(List<AssetVersion> versions);

    /**
     * 搜索分页结果 → 分页视图对象（list/total/page/size 语义）。
     */
    default AssetPageVO toPageVO(AssetSearchResult result) {
        AssetPageVO vo = new AssetPageVO();
        vo.setList(toVOList(result.getItems()));
        vo.setTotal(result.getTotal());
        vo.setPage(result.getPageIndex());
        vo.setSize(result.getPageSize());
        return vo;
    }

    default String toStatusString(AssetStatus status) {
        return status == null ? null : status.getValue();
    }
}
