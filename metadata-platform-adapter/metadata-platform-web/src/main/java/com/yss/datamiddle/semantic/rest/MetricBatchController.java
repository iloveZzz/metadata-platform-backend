package com.yss.datamiddle.semantic.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.semantic.application.service.MetricBatchImportExportService;
import com.yss.datamiddle.semantic.client.dto.cmd.MetricBatchImportCmd;
import com.yss.datamiddle.semantic.client.vo.MetricImportResultVO;
import com.yss.datamiddle.semantic.metric.batch.MetricImportResult;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 指标口径批量导入与导出 REST API 控制器 (SL-P1-01)
 */
@RestController
@RequestMapping("/api/semantic/metrics/batch")
@RequiredArgsConstructor
public class MetricBatchController {

    private final MetricBatchImportExportService batchService;

    @PostMapping("/import-csv")
    public SingleResult<MetricImportResultVO> importCsv(
            @Valid @RequestBody MetricBatchImportCmd cmd
    ) {
        MetricImportResult result = batchService.importFromCsv(
                cmd.getCsvContent(),
                Boolean.TRUE.equals(cmd.getOverwriteExisting())
        );

        MetricImportResultVO vo = MetricImportResultVO.builder()
                .totalCount(result.getTotalCount())
                .successCount(result.getSuccessCount())
                .failureCount(result.getFailureCount())
                .errors(result.getErrors())
                .build();

        return SingleResult.of(vo);
    }

    @GetMapping(value = "/export-csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportCsv() {
        String csv = batchService.exportToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"metrics.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }
}
