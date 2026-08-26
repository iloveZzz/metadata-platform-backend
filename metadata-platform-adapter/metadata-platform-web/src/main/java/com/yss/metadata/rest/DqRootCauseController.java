package com.yss.metadata.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.dq.BlastRadiusApplicationService;
import com.yss.metadata.application.dq.DqRootCauseApplicationService;
import com.yss.metadata.application.dq.convertor.DqObservabilityConvertor;
import com.yss.metadata.client.vo.BlastRadiusVO;
import com.yss.metadata.client.vo.RootCauseVO;
import com.yss.metadata.domain.dq.model.BlastRadiusReport;
import com.yss.metadata.domain.dq.model.RootCauseReport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 质量可观测性与根因/爆炸半径控制器 (GET /api/dq/assets/{id}/root-cause, GET /api/dq/assets/{id}/blast-radius)
 *
 * @author ai
 * @since 2026-08-15
 */
@RestController
@RequestMapping("/api/dq/assets")
@RequiredArgsConstructor
@Api(tags = "dq-observability")
public class DqRootCauseController {

    private final DqRootCauseApplicationService dqRootCauseApplicationService;
    private final BlastRadiusApplicationService blastRadiusApplicationService;
    private final DqObservabilityConvertor convertor;

    /**
     * 质量故障一键根因溯源分析 (GET /api/dq/assets/{id}/root-cause)
     */
    @GetMapping("/{id}/root-cause")
    @ApiOperation(value = "质量故障一键根因溯源分析", notes = "基于血缘拓扑向上寻源与时序比对，定位最上游故障节点与置信度")
    public SingleResult<RootCauseVO> getRootCause(@PathVariable("id") String id) {
        RootCauseReport report = dqRootCauseApplicationService.analyzeRootCause(id);
        return SingleResult.of(convertor.toRootCauseVO(report));
    }

    /**
     * 下游爆炸半径与受影响资产分析 (GET /api/dq/assets/{id}/blast-radius)
     */
    @GetMapping("/{id}/blast-radius")
    @ApiOperation(value = "下游爆炸半径分析", notes = "递归遍历下游血缘节点，评估受污染范围与深度")
    public SingleResult<BlastRadiusVO> getBlastRadius(@PathVariable("id") String id,
                                                      @RequestParam(value = "maxDepth", required = false, defaultValue = "5") Integer maxDepth) {
        BlastRadiusReport report = blastRadiusApplicationService.calculateBlastRadius(id, maxDepth != null ? maxDepth : 5);
        return SingleResult.of(convertor.toBlastRadiusVO(report));
    }
}
