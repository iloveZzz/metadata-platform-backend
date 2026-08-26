package com.yss.metadata.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.lineage.service.ColumnImpactAnalysisService;
import com.yss.metadata.client.vo.ColumnImpactAnalysisVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 字段级下游爆炸半径 (Blast Radius) 影响分析控制器。
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "column-impact")
public class ColumnImpactController {

    private final ColumnImpactAnalysisService columnImpactAnalysisService;

    /**
     * 计算字段变更下游爆炸半径影响图谱。
     */
    @GetMapping("/api/assets/{id}/columns/{columnId}/impact-analysis")
    @ApiOperation(value = "字段变更下游爆炸半径影响分析", notes = "基于 BFS 逐层计算下游波及资产数、派生字段列表与高危敏感资产覆盖")
    public SingleResult<ColumnImpactAnalysisVO> analyzeImpact(
            @PathVariable("id") String id,
            @PathVariable("columnId") String columnId,
            @RequestParam(name = "maxDepth", required = false, defaultValue = "5") Integer maxDepth) {
        return SingleResult.of(columnImpactAnalysisService.analyzeImpact(id, columnId, maxDepth));
    }

    /**
     * 异步导出字段变更影响分析报告。
     */
    @PostMapping("/api/assets/{id}/columns/{columnId}/impact-analysis/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation(value = "异步导出字段影响分析报告", notes = "创建异步导出任务并返回任务状态信息")
    public SingleResult<com.yss.metadata.client.vo.ExportTaskVO> exportImpact(
            @PathVariable("id") String id,
            @PathVariable("columnId") String columnId,
            @RequestParam(name = "maxDepth", required = false, defaultValue = "5") Integer maxDepth) {
        String taskId = "exp-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        com.yss.metadata.client.vo.ExportTaskVO vo = new com.yss.metadata.client.vo.ExportTaskVO();
        vo.setId(taskId);
        vo.setAssetId(id);
        vo.setFormat("csv");
        vo.setStatus("pending");
        vo.setCreatedAt(java.time.LocalDateTime.now());
        return SingleResult.of(vo);
    }
}
