package com.yss.datamiddle.smartgovernance.web.controller;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.smartgovernance.application.service.MetricGovernanceApplicationService;
import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictStatus;
import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictType;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricAstDiff;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricConflictRecord;
import com.yss.datamiddle.smartgovernance.web.dto.MarkMetricConflictSuspectDTO;
import com.yss.datamiddle.smartgovernance.web.dto.ReconcileMetricConflictDTO;
import com.yss.datamiddle.smartgovernance.web.vo.MetricConflictDiffVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Api(tags = "智能指标对齐与冲突治理")
@RequestMapping("/api/smart-governance/metrics")
public class MetricGovernanceController {

    private final MetricGovernanceApplicationService metricService;

    @GetMapping("/conflicts")
    @ApiOperation("分页查询指标语义与 AST 公式冲突列表")
    public PageResult<MetricConflictRecord> listConflicts(
            @RequestParam(defaultValue = "1") Integer pageIndex,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String conflictType,
            @RequestParam(required = false) String keyword
    ) {
        ConflictStatus conflictStatus = status != null ? ConflictStatus.valueOf(status) : null;
        ConflictType type = conflictType != null ? ConflictType.valueOf(conflictType) : null;

        List<MetricConflictRecord> list = metricService.queryConflicts(pageIndex, pageSize, conflictStatus, type, keyword);
        long total = metricService.countConflicts(conflictStatus, type, keyword);

        return PageResult.of(list, total, pageSize, pageIndex);
    }

    @PostMapping("/conflict-scan")
    @ApiOperation("触发指标相似度与公式 AST 冲突探测全量扫描")
    public SingleResult<String> triggerConflictScan() {
        return SingleResult.of(metricService.triggerConflictScan());
    }

    @GetMapping("/conflicts/{id}/diff")
    @ApiOperation("获取冲突指标对 Side-by-Side 详细差异 (公式/AST/过滤条件)")
    @SuppressWarnings("unchecked")
    public SingleResult<MetricConflictDiffVO> getConflictDiff(@PathVariable String id) {
        Map<String, Object> map = metricService.getConflictDiff(id);
        MetricConflictDiffVO vo = MetricConflictDiffVO.builder()
                .conflict((MetricConflictRecord) map.get("conflict"))
                .indicatorA((Map<String, Object>) map.get("indicatorA"))
                .indicatorB((Map<String, Object>) map.get("indicatorB"))
                .astDiff((MetricAstDiff) map.get("astDiff"))
                .build();
        return SingleResult.of(vo);
    }

    @PostMapping("/conflicts/{id}/reconcile")
    @ApiOperation("执行指标一键标准化对齐归并")
    public SingleResult<Boolean> reconcileConflict(
            @PathVariable String id,
            @Valid @RequestBody ReconcileMetricConflictDTO dto,
            @RequestHeader(value = "X-User-Id", defaultValue = "gov_admin") String operator
    ) {
        metricService.reconcileConflict(id, dto.getCanonicalIndicatorId(), dto.getReconcileStrategy(), dto.getComment(), operator);
        return SingleResult.of(true);
    }

    @PostMapping("/conflicts/{id}/mark-suspect")
    @ApiOperation("标记指标冲突存疑并在消费端显示警示")
    public SingleResult<Boolean> markSuspect(
            @PathVariable String id,
            @Valid @RequestBody MarkMetricConflictSuspectDTO dto,
            @RequestHeader(value = "X-User-Id", defaultValue = "gov_admin") String operator
    ) {
        metricService.markSuspect(id, dto.getReason(), operator);
        return SingleResult.of(true);
    }

    @PostMapping("/conflicts/{id}/dismiss")
    @ApiOperation("忽略误报指标冲突")
    public SingleResult<Boolean> dismiss(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "gov_admin") String operator
    ) {
        metricService.dismissConflict(id, "专员忽略该冲突", operator);
        return SingleResult.of(true);
    }
}
