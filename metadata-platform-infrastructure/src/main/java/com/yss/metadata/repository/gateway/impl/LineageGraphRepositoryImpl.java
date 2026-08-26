package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.domain.lineage.gateway.LineageGraphRepository;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;
import com.yss.metadata.repository.LineageEdgeRepository;
import com.yss.metadata.infrastructure.convertor.LineageEdgeConvertor;
import com.yss.metadata.repository.entity.LineageEdgePO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 血缘图谱仓储实现（MyBatis-Plus；lineage_edge 邻接表）。
 *
 * <p>图谱邻域查询（from=资产 或 to=资产，confidence 筛选，空血缘空结构）；
 * 全量图加载（环检测/版本校验用）；补录边写入（id 由实现生成，graphVersion
 * 由应用层推进）。图版本 = 全部边 graph_version 最大值（乐观锁 token）。</p>
 */
@Repository
public class LineageGraphRepositoryImpl implements LineageGraphRepository {

    private final LineageEdgeRepository lineageEdgeRepository;
    private final LineageEdgeConvertor lineageEdgeConvertor;

    @Autowired
    public LineageGraphRepositoryImpl(LineageEdgeRepository lineageEdgeRepository) {
        this(lineageEdgeRepository, Mappers.getMapper(LineageEdgeConvertor.class));
    }

    public LineageGraphRepositoryImpl(LineageEdgeRepository lineageEdgeRepository, LineageEdgeConvertor lineageEdgeConvertor) {
        this.lineageEdgeRepository = lineageEdgeRepository;
        this.lineageEdgeConvertor = lineageEdgeConvertor != null ? lineageEdgeConvertor : Mappers.getMapper(LineageEdgeConvertor.class);
    }

    @Override
    @Transactional(readOnly = true)
    public LineageGraph loadGraph() {
        List<LineageEdgePO> pos = lineageEdgeRepository.selectList(null);
        return LineageGraph.of(lineageEdgeConvertor.toDomainList(pos), currentVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public LineageGraph findGraph(String assetId, LineageConfidence confidence) {
        List<LineageEdgePO> pos = lineageEdgeRepository.selectList(
                Wrappers.<LineageEdgePO>lambdaQuery()
                        .and(wrapper -> wrapper.eq(LineageEdgePO::getFromAsset, assetId)
                                .or().eq(LineageEdgePO::getToAsset, assetId))
                        .eq(confidence != null, LineageEdgePO::getConfidence,
                                confidence == null ? null : confidence.getValue()));
        return LineageGraph.of(lineageEdgeConvertor.toDomainList(pos), currentVersion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LineageEdge save(LineageEdge edge) {
        LineageEdgePO po = lineageEdgeConvertor.toPO(edge);
        if (po.getId() == null || po.getId().trim().isEmpty()) {
            po.setId(UUID.randomUUID().toString());
        }
        lineageEdgeRepository.insert(po);
        return lineageEdgeConvertor.toDomain(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String edgeId) {
        if (edgeId != null && !edgeId.trim().isEmpty()) {
            lineageEdgeRepository.deleteById(edgeId);
        }
    }

    /**
     * 当前图版本：全部边 graph_version 最大值（全局；乐观锁 token）。
     */
    private String currentVersion() {
        return lineageEdgeRepository.selectLatestGraphVersion();
    }
}
