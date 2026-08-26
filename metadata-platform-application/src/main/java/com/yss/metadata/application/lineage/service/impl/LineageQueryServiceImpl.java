package com.yss.metadata.application.lineage.service.impl;

import com.yss.metadata.application.lineage.service.LineageQueryService;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.yss.metadata.client.vo.LineageGraphVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.lineage.gateway.LineageGraphRepository;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageGraph;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 血缘图谱查询应用服务实现（WU-03-01）。
 *
 * <p>图谱邻域查询（from=资产 或 to=资产）+ confidence 筛选（all/缺省不过滤）；
 * 空血缘以空结构表达（非错误）；资产不存在抛 404；非法 confidence 抛 422。</p>
 */
@Service
@RequiredArgsConstructor
public class LineageQueryServiceImpl implements LineageQueryService {

    private final AssetRepository assetRepository;
    private final LineageGraphRepository lineageGraphRepository;
    private final LineageAppConvertor lineageAppConvertor;

    @Override
    @Transactional(readOnly = true)
    public LineageGraphVO getGraph(String assetId, String confidence) {
        requireAsset(assetId);
        LineageConfidence filter = resolveConfidenceFilter(confidence);
        LineageGraph graph = lineageGraphRepository.findGraph(assetId, filter);
        return lineageAppConvertor.toGraphVO(graph);
    }

    /**
     * confidence 参数 → 筛选枚举（all/缺省=不过滤；未知值抛非法参数 → 422）。
     */
    private LineageConfidence resolveConfidenceFilter(String confidence) {
        if (confidence == null || confidence.trim().isEmpty() || "all".equalsIgnoreCase(confidence.trim())) {
            return null;
        }
        return LineageConfidence.fromValue(confidence.trim());
    }

    private void requireAsset(String assetId) {
        assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
    }
}
