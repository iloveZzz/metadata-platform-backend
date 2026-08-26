package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.RecognitionRuleBatchRunDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleCreateDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleManualScanDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleTestDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleTestResultVO;
import com.yss.datasecurity.application.dto.RecognitionRuleTransferOwnerDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleUpdateDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleVO;
import com.yss.datasecurity.application.service.RecognitionRuleAppService;
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

@Api(tags = "敏感数据识别规则管理")
@RestController
@RequestMapping("/api/v1/sec/recognition-rules")
@RequiredArgsConstructor
@Validated
public class RecognitionRuleController {

    private final RecognitionRuleAppService recognitionRuleAppService;

    @ApiOperation("分页查询识别规则")
    @GetMapping
    public PageResult<RecognitionRuleVO> pageRules(
            @RequestParam(name = "pageIndex", defaultValue = "1") int pageIndex,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "owner", required = false) String owner,
            @RequestParam(name = "onlyMine", required = false) Boolean onlyMine,
            @RequestParam(name = "currentUsername", required = false) String currentUsername) {
        return recognitionRuleAppService.pageRules(pageIndex, pageSize, keyword, categoryId, owner, onlyMine, currentUsername);
    }

    @ApiOperation("查询识别规则详情")
    @GetMapping("/{id}")
    public SingleResult<RecognitionRuleVO> getRuleDetail(@PathVariable("id") Long id) {
        RecognitionRuleVO vo = recognitionRuleAppService.getDetail(id);
        return SingleResult.of(vo);
    }

    @ApiOperation("新建识别规则")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<Long> createRule(@Valid @RequestBody RecognitionRuleCreateDTO dto) {
        Long id = recognitionRuleAppService.create(dto);
        return SingleResult.of(id);
    }

    @ApiOperation("编辑识别规则")
    @PutMapping("/{id}")
    public SingleResult<Boolean> updateRule(@PathVariable("id") Long id, @Valid @RequestBody RecognitionRuleUpdateDTO dto) {
        recognitionRuleAppService.update(id, dto);
        return SingleResult.of(true);
    }

    @ApiOperation("删除识别规则")
    @DeleteMapping("/{id}")
    public SingleResult<Boolean> deleteRule(@PathVariable("id") Long id) {
        recognitionRuleAppService.delete(id);
        return SingleResult.of(true);
    }

    @ApiOperation("切换识别规则是否生效")
    @PutMapping("/{id}/status")
    public SingleResult<Boolean> changeRuleStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        recognitionRuleAppService.updateStatus(id, status);
        return SingleResult.of(true);
    }

    @ApiOperation("重置识别规则（清空打标并重新识别）")
    @PostMapping("/{id}/reset")
    public SingleResult<Boolean> resetRule(@PathVariable("id") Long id) {
        recognitionRuleAppService.resetRule(id);
        return SingleResult.of(true);
    }

    @ApiOperation("复制/克隆识别规则")
    @PostMapping("/{id}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<Long> cloneRule(@PathVariable("id") Long id) {
        Long newId = recognitionRuleAppService.cloneRule(id);
        return SingleResult.of(newId);
    }

    @ApiOperation("批量转交识别规则负责人")
    @PostMapping("/transfer-owner")
    public SingleResult<Boolean> transferOwner(@Valid @RequestBody RecognitionRuleTransferOwnerDTO dto) {
        recognitionRuleAppService.transferOwner(dto);
        return SingleResult.of(true);
    }

    @ApiOperation("手动运行/批量手动运行识别规则")
    @PostMapping("/run")
    public SingleResult<Integer> batchRun(@RequestBody RecognitionRuleBatchRunDTO dto) {
        int count = recognitionRuleAppService.batchRun(dto);
        return SingleResult.of(count);
    }

    @ApiOperation("手动规则扫描触发")
    @PostMapping("/manual-scan")
    public SingleResult<Integer> manualScan(@Valid @RequestBody RecognitionRuleManualScanDTO dto) {
        int count = recognitionRuleAppService.manualScan(dto);
        return SingleResult.of(count);
    }

    @ApiOperation("抽样规则测试（无副作用）")
    @PostMapping("/test")
    public MultiResult<RecognitionRuleTestResultVO> testRule(@Valid @RequestBody RecognitionRuleTestDTO dto) {
        List<RecognitionRuleTestResultVO> list = recognitionRuleAppService.testRule(dto);
        return MultiResult.of(list);
    }
}
