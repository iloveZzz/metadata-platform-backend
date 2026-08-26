package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.MaskingRuleCreateDTO;
import com.yss.datasecurity.application.dto.MaskingRuleUpdateDTO;
import com.yss.datasecurity.application.dto.MaskingRuleVO;
import com.yss.datasecurity.application.service.MaskingRuleAppService;
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

@Api(tags = "脱敏保护规则配置")
@RestController
@RequestMapping("/api/v1/masking-rules")
@RequiredArgsConstructor
@Validated
public class MaskingRuleController {

    private final MaskingRuleAppService maskingRuleAppService;

    @ApiOperation("分页查询脱敏规则列表")
    @GetMapping
    public PageResult<MaskingRuleVO> pageRules(
            @RequestParam(name = "pageIndex", defaultValue = "1") int pageIndex,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "ruleType", required = false) String ruleType,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "applyScene", required = false) String applyScene) {
        return maskingRuleAppService.pageRules(pageIndex, pageSize, keyword, ruleType, categoryId, applyScene);
    }

    @ApiOperation("创建脱敏规则")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<Long> createRule(@Valid @RequestBody MaskingRuleCreateDTO dto) {
        Long id = maskingRuleAppService.createRule(dto);
        return SingleResult.of(id);
    }

    @ApiOperation("更新脱敏规则")
    @PutMapping("/{id}")
    public SingleResult<Boolean> updateRule(@PathVariable("id") Long id, @Valid @RequestBody MaskingRuleUpdateDTO dto) {
        maskingRuleAppService.updateRule(id, dto);
        return SingleResult.of(true);
    }

    @ApiOperation("切换脱敏规则生效状态")
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/status")
    public SingleResult<Boolean> updateStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        maskingRuleAppService.updateStatus(id, status);
        return SingleResult.of(true);
    }

    @ApiOperation("批量转交脱敏规则负责人")
    @PostMapping("/transfer")
    public SingleResult<Boolean> transferOwner(@Valid @RequestBody com.yss.datasecurity.application.dto.MaskingRuleTransferOwnerDTO dto) {
        maskingRuleAppService.transferOwner(dto);
        return SingleResult.of(true);
    }

    @ApiOperation("获取默认脱敏策略")
    @GetMapping("/default-policy")
    public SingleResult<com.yss.datasecurity.application.dto.DefaultMaskingPolicyVO> getDefaultPolicy() {
        return SingleResult.of(maskingRuleAppService.getDefaultPolicy());
    }

    @ApiOperation("更新默认脱敏策略")
    @PutMapping("/default-policy")
    public SingleResult<Boolean> saveDefaultPolicy(@Valid @RequestBody com.yss.datasecurity.application.dto.DefaultMaskingPolicyDTO dto) {
        maskingRuleAppService.saveDefaultPolicy(dto);
        return SingleResult.of(true);
    }

    @ApiOperation("删除脱敏规则")
    @DeleteMapping("/{id}")
    public SingleResult<Boolean> deleteRule(@PathVariable("id") Long id) {
        maskingRuleAppService.deleteRule(id);
        return SingleResult.of(true);
    }
}
