package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.Result;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.RecognitionBatchLogVO;
import com.yss.datasecurity.application.dto.RecognitionResultDetailVO;
import com.yss.datasecurity.application.dto.RecognitionResultEditDTO;
import com.yss.datasecurity.application.dto.RecognitionResultImportPreviewVO;
import com.yss.datasecurity.application.dto.RecognitionResultManualAddDTO;
import com.yss.datasecurity.application.dto.RecognitionResultPageQueryDTO;
import com.yss.datasecurity.application.dto.RecognitionResultVO;
import com.yss.datasecurity.application.service.RecognitionResultAppService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "数据安全-数据识别结果管理")
@RestController
@RequestMapping("/api/v1/sec/recognition-results")
@RequiredArgsConstructor
@Validated
public class RecognitionResultController {

    private final RecognitionResultAppService recognitionResultAppService;

    @ApiOperation("分页查询数据识别结果列表")
    @GetMapping
    public PageResult<RecognitionResultVO> pageRecognitionResults(@Valid RecognitionResultPageQueryDTO query) {
        return recognitionResultAppService.pageRecognitionResults(query);
    }

    @ApiOperation("获取单条字段识别详情（含基本信息、生效结果与识别记录池）")
    @GetMapping("/{id}")
    public SingleResult<RecognitionResultDetailVO> getDetail(@PathVariable("id") Long id) {
        return SingleResult.of(recognitionResultAppService.getRecognitionResultDetail(id));
    }

    @ApiOperation("切换单条字段脱敏生效状态")
    @PutMapping("/{id}/masking-status")
    public SingleResult<Boolean> updateMaskingStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status
    ) {
        recognitionResultAppService.updateMaskingStatus(id, status);
        return SingleResult.of(true);
    }

    @ApiOperation("批量切换字段脱敏生效状态")
    @PutMapping("/batch-masking-status")
    public SingleResult<Boolean> batchUpdateMaskingStatus(
            @RequestParam("ids") List<Long> ids,
            @RequestParam("status") String status
    ) {
        recognitionResultAppService.batchUpdateMaskingStatus(ids, status);
        return SingleResult.of(true);
    }

    @ApiOperation("锁定/解锁单个识别结果")
    @PutMapping("/{id}/lock")
    public SingleResult<Boolean> lockResult(
            @PathVariable("id") Long id,
            @RequestParam("isLocked") boolean isLocked
    ) {
        recognitionResultAppService.lockResult(id, isLocked);
        return SingleResult.of(true);
    }

    @ApiOperation("批量锁定/解锁识别结果")
    @PutMapping("/batch-lock")
    public SingleResult<Boolean> batchLockResults(
            @RequestParam("ids") List<Long> ids,
            @RequestParam("isLocked") boolean isLocked
    ) {
        recognitionResultAppService.batchLockResults(ids, isLocked);
        return SingleResult.of(true);
    }

    @ApiOperation("编辑识别结果（修改分类/识别方式）")
    @PutMapping("/edit")
    public SingleResult<Boolean> editResult(@Valid @RequestBody RecognitionResultEditDTO dto) {
        recognitionResultAppService.editResult(dto);
        return SingleResult.of(true);
    }

    @ApiOperation("采纳系统推荐的识别分类或指定候选记录生效")
    @PostMapping("/{id}/adopt-recommendation")
    public SingleResult<Boolean> adoptRecommendation(
            @PathVariable("id") Long id,
            @RequestParam(name = "candidateCategoryId", required = false) Long candidateCategoryId
    ) {
        recognitionResultAppService.adoptRecommendation(id, candidateCategoryId);
        return SingleResult.of(true);
    }

    @ApiOperation("删除识别结果")
    @DeleteMapping("/{id}")
    public SingleResult<Boolean> deleteResult(@PathVariable("id") Long id) {
        recognitionResultAppService.deleteResult(id);
        return SingleResult.of(true);
    }

    @ApiOperation("批量删除识别结果")
    @DeleteMapping("/batch")
    public SingleResult<Boolean> batchDeleteResults(@RequestParam("ids") List<Long> ids) {
        recognitionResultAppService.batchDeleteResults(ids);
        return SingleResult.of(true);
    }

    @ApiOperation("手动添加识别结果")
    @PostMapping("/manual-add")
    public SingleResult<Boolean> manualAdd(@Valid @RequestBody RecognitionResultManualAddDTO dto) {
        recognitionResultAppService.manualAdd(dto);
        return SingleResult.of(true);
    }

    @ApiOperation("批量导入 Excel 预校验")
    @PostMapping("/import-preview")
    public SingleResult<RecognitionResultImportPreviewVO> importPreview(
            @RequestParam(name = "assetType", defaultValue = "DATAPHIN") String assetType,
            @RequestParam(name = "conflictStrategy", defaultValue = "OVERWRITE_ALL") String conflictStrategy,
            @RequestParam(name = "maskingPolicy", defaultValue = "UNIFIED_ENABLED") String maskingPolicy
    ) {
        return SingleResult.of(recognitionResultAppService.importPreview(assetType, conflictStrategy, maskingPolicy));
    }

    @ApiOperation("确认执行批量导入")
    @PostMapping("/import-execute")
    public SingleResult<Boolean> importExecute(
            @RequestParam(name = "assetType", defaultValue = "DATAPHIN") String assetType,
            @RequestParam(name = "conflictStrategy", defaultValue = "OVERWRITE_ALL") String conflictStrategy,
            @RequestParam(name = "maskingPolicy", defaultValue = "UNIFIED_ENABLED") String maskingPolicy,
            @RequestParam(name = "fileName", required = false) String fileName
    ) {
        recognitionResultAppService.importExecute(assetType, conflictStrategy, maskingPolicy, fileName);
        return SingleResult.of(true);
    }

    @ApiOperation("获取批量导入/操作历史记录")
    @GetMapping("/import-history")
    public com.yss.cloud.dto.result.MultiResult<RecognitionBatchLogVO> listImportHistory() {
        return com.yss.cloud.dto.result.MultiResult.of(recognitionResultAppService.listImportHistory());
    }
}
