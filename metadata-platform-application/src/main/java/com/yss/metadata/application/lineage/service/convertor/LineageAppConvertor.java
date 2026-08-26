package com.yss.metadata.application.lineage.service.convertor;

import com.yss.metadata.application.lineage.service.support.ImpactRisk;
import com.yss.metadata.client.vo.ExportTaskVO;
import com.yss.metadata.client.vo.ImpactGroupVO;
import com.yss.metadata.client.vo.ImpactItemVO;
import com.yss.metadata.client.vo.ImpactVO;
import com.yss.metadata.client.vo.LineageEdgeVO;
import com.yss.metadata.client.vo.LineageGraphVO;
import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;
import com.yss.metadata.domain.lineage.model.ImpactNode;
import com.yss.metadata.domain.lineage.model.ImpactSort;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 血缘/影响/导出对象转换器（MapStruct；禁止 BeanUtils.copyProperties 或手写字段映射）。
 *
 * <p>Domain → VO：枚举 → 冻结 OpenAPI 小写字符串；影响分析按深度分组
 * （sortBy depth/domain/risk 组内排序）；风险等级由分类推导。</p>
 */
@Mapper(config = MapStructAppConfig.class)
public interface LineageAppConvertor {

    // ---------- 血缘 ----------

    LineageEdgeVO toEdgeVO(LineageEdge edge);

    List<LineageEdgeVO> toEdgeVOList(List<LineageEdge> edges);

    /**
     * 图谱 → 图谱视图对象（edges + graphVersionToken）。
     */
    default LineageGraphVO toGraphVO(LineageGraph graph) {
        LineageGraphVO vo = new LineageGraphVO();
        vo.setEdges(toEdgeVOList(graph.getEdges()));
        vo.setGraphVersionToken(graph.getVersion());
        return vo;
    }

    default String toTypeString(LineageType type) {
        return type == null ? null : type.getValue();
    }

    default String toConfidenceString(LineageConfidence confidence) {
        return confidence == null ? null : confidence.getValue();
    }

    // ---------- 影响分析 ----------

    @Mapping(target = "risk", source = "classification", qualifiedByName = "classificationToRisk")
    ImpactItemVO toItemVO(ImpactNode node);

    /**
     * 影响节点列表 → 影响视图对象（按深度升序分组，组内按 sortBy 排序）。
     */
    default ImpactVO toImpactVO(List<ImpactNode> nodes, ImpactSort sort) {
        ImpactVO vo = new ImpactVO();
        vo.setSortBy(sort.getValue());
        Map<Integer, List<ImpactItemVO>> byDepth = new TreeMap<>();
        for (ImpactNode node : nodes) {
            ImpactItemVO item = toItemVO(node);
            if (item.getRisk() == null) {
                item.setRisk(ImpactRisk.of(null)); // 分类为空时风险显式 low
            }
            byDepth.computeIfAbsent(node.getDepth(), k -> new ArrayList<>()).add(item);
        }
        List<ImpactGroupVO> groups = new ArrayList<>();
        for (Map.Entry<Integer, List<ImpactItemVO>> entry : byDepth.entrySet()) {
            List<ImpactItemVO> items = entry.getValue();
            items.sort(comparatorFor(sort));
            ImpactGroupVO group = new ImpactGroupVO();
            group.setDepth(entry.getKey());
            group.setItems(items);
            groups.add(group);
        }
        vo.setGroups(groups);
        return vo;
    }

    /**
     * 组内排序器：depth→名称；domain→数据域（空值后置）再名称；risk→风险降序再名称。
     */
    default Comparator<ImpactItemVO> comparatorFor(ImpactSort sort) {
        switch (sort) {
            case DOMAIN:
                return Comparator
                        .comparing(ImpactItemVO::getDomain, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ImpactItemVO::getName, Comparator.nullsLast(String::compareTo));
            case RISK:
                return Comparator
                        .comparing((ImpactItemVO item) -> ImpactRisk.order(item.getRisk()))
                        .reversed()
                        .thenComparing(ImpactItemVO::getName, Comparator.nullsLast(String::compareTo));
            default:
                return Comparator.comparing(ImpactItemVO::getName, Comparator.nullsLast(String::compareTo));
        }
    }

    /**
     * 分类 → 风险等级（high/medium/low；受控解读见 {@link ImpactRisk}）。
     *
     * <p>@Named 限定：仅用于 toItemVO 的 risk 目标字段，
     * 避免 MapStruct 将其作为通用 String→String 转换套用其他字段。</p>
     */
    @Named("classificationToRisk")
    default String mapClassificationToRisk(String classification) {
        return ImpactRisk.of(classification);
    }

    // ---------- 导出任务 ----------

    ExportTaskVO toExportTaskVO(ExportTask task);

    default String toStatusString(ExportTaskStatus status) {
        return status == null ? null : status.getValue();
    }
}
