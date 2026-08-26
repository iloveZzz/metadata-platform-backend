package com.yss.datamiddle.semantic.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.semantic.application.model.MetricCreateInput;
import com.yss.datamiddle.semantic.application.model.MetricVersionInput;
import com.yss.datamiddle.semantic.application.service.MetricService;
import com.yss.datamiddle.semantic.client.dto.cmd.MetricCertifyCmd;
import com.yss.datamiddle.semantic.client.dto.cmd.MetricCreateCmd;
import com.yss.datamiddle.semantic.client.dto.cmd.MetricStatusCmd;
import com.yss.datamiddle.semantic.client.dto.cmd.MetricVersionCmd;
import com.yss.datamiddle.semantic.client.vo.MetricVO;
import com.yss.datamiddle.semantic.client.vo.MetricVersionVO;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import com.yss.datamiddle.semantic.metric.model.MetricStatus;
import com.yss.datamiddle.semantic.metric.model.MetricVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 指标口径 REST API 控制器（SL-002 / SL-006）。
 */
@RestController
@RequestMapping("/api/semantic/metric-definitions")
@RequiredArgsConstructor
@Validated
public class MetricController {

    private final MetricService metricService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<MetricVO> create(@Valid @RequestBody MetricCreateCmd cmd) {
        MetricCreateInput input = MetricCreateInput.builder()
                .name(cmd.getName())
                .metricGroup(cmd.getMetricGroup())
                .description(cmd.getDescription())
                .owner(cmd.getOwner())
                .build();
        MetricDefinition created = metricService.create(input);
        return SingleResult.of(toVO(created));
    }

    @GetMapping
    public MultiResult<MetricVO> list() {
        List<MetricVO> list = metricService.list().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return MultiResult.of(list);
    }

    @GetMapping("/{id}")
    public SingleResult<MetricVO> getById(@PathVariable("id") Long id) {
        MetricDefinition m = metricService.getById(id);
        return SingleResult.of(toVO(m));
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<MetricVersionVO> addVersion(@PathVariable("id") Long id, @Valid @RequestBody MetricVersionCmd cmd) {
        MetricVersionInput input = MetricVersionInput.builder()
                .expression(cmd.getExpression())
                .logicDescription(cmd.getLogicDescription())
                .dimensions(cmd.getDimensions())
                .filters(cmd.getFilters())
                .build();
        MetricVersion v = metricService.addVersion(id, input);
        return SingleResult.of(toVersionVO(v));
    }

    @PostMapping("/{id}/versions/{versionNo}/rollback")
    public SingleResult<MetricVersionVO> rollback(@PathVariable("id") Long id, @PathVariable("versionNo") Integer versionNo) {
        MetricVersion v = metricService.rollback(id, versionNo);
        return SingleResult.of(toVersionVO(v));
    }

    @PostMapping("/{id}/certify")
    public SingleResult<MetricVO> certify(@PathVariable("id") Long id, @RequestBody(required = false) MetricCertifyCmd cmd) {
        boolean force = cmd == null || cmd.getForce() == null || cmd.getForce();
        MetricDefinition m = metricService.certify(id, force);
        return SingleResult.of(toVO(m));
    }

    @PutMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleStatus(@PathVariable("id") Long id, @Valid @RequestBody MetricStatusCmd cmd) {
        MetricStatus status = MetricStatus.valueOf(cmd.getStatus().toUpperCase());
        metricService.toggleStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        metricService.delete(id);
    }

    private MetricVO toVO(MetricDefinition m) {
        return MetricVO.builder()
                .id(m.getId())
                .name(m.getName())
                .metricGroup(m.getMetricGroup())
                .description(m.getDescription())
                .owner(m.getOwner())
                .status(m.getStatus() != null ? m.getStatus().name() : null)
                .authoritative(m.getAuthoritative())
                .currentVersionNo(m.getCurrentVersionNo())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private MetricVersionVO toVersionVO(MetricVersion v) {
        return MetricVersionVO.builder()
                .versionNo(v.getVersionNo())
                .expression(v.getExpression())
                .logicDescription(v.getLogicDescription())
                .dimensions(v.getDimensions())
                .filters(v.getFilters())
                .rollbackFromNo(v.getRollbackFromNo())
                .createdBy(v.getCreatedBy())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
