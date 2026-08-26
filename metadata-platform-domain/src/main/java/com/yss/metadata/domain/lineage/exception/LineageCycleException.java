package com.yss.metadata.domain.lineage.exception;

import com.yss.cloud.exception.BizException;
import com.yss.metadata.domain.lineage.model.LineageEdge;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 血缘成环（409 语义，CYCLE）。
 *
 * <p>人工补录保存时检测到新增边将形成环：冲突边为补录边本身（from→to），
 * 闭环路径为从 to 出发到达 from 的既有边链（供前端定位修复）。
 * 错误码 {@code lineage.cycle}。</p>
 */
public class LineageCycleException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 冲突边（本次补录的 from→to，即闭环缺失边） */
    private final LineageEdge conflictEdge;

    /** 闭环既有路径（to → ... → from；自环为空） */
    private final List<LineageEdge> cyclePath;

    public LineageCycleException(LineageEdge conflictEdge, List<LineageEdge> cyclePath) {
        super("lineage.cycle", buildMessage(conflictEdge, cyclePath));
        this.conflictEdge = conflictEdge;
        this.cyclePath = cyclePath == null ? Collections.emptyList() : cyclePath;
    }

    public LineageEdge getConflictEdge() {
        return conflictEdge;
    }

    public List<LineageEdge> getCyclePath() {
        return cyclePath;
    }

    private static String buildMessage(LineageEdge conflictEdge, List<LineageEdge> cyclePath) {
        String path = (cyclePath == null || cyclePath.isEmpty())
                ? "无既有路径（自环）"
                : cyclePath.stream().map(LineageEdge::getId)
                        .collect(Collectors.joining(" → "));
        return "血缘成环（CYCLE）：补录边 " + conflictEdge.describe()
                + " 将形成环，冲突路径 [" + path + "]，已阻断保存，请调整连线";
    }
}
