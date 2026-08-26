package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.RuleSimulationRequestDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleCreateDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleUpdateDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleVO;
import com.yss.datasecurity.application.dto.SimulationFieldMatchVO;
import com.yss.datasecurity.application.service.SensitiveRuleAppService;
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

@Api(tags = "敏感特征识别规则管理")
@RestController
@RequestMapping("/api/v1/sensitive-rules")
@RequiredArgsConstructor
@Validated
public class SensitiveRuleController {

    private final SensitiveRuleAppService sensitiveRuleAppService;

    @ApiOperation("分页查询识别特征规则")
    @GetMapping
    public PageResult<SensitiveRuleVO> pageRules(
            @RequestParam(name = "pageIndex", defaultValue = "1") int pageIndex,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "scanScopeType", required = false) String scanScopeType,
            @RequestParam(name = "ruleType", required = false) String ruleType) {
        return sensitiveRuleAppService.pageRules(pageIndex, pageSize, keyword, status, scanScopeType, ruleType);
    }

    @ApiOperation("查询敏感识别规则详情")
    @GetMapping("/{id}")
    public SingleResult<SensitiveRuleVO> getRuleDetail(@PathVariable("id") Long id) {
        SensitiveRuleVO vo = sensitiveRuleAppService.getDetail(id);
        return SingleResult.of(vo);
    }

    @ApiOperation("创建敏感识别规则")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<Long> createRule(@Valid @RequestBody SensitiveRuleCreateDTO dto) {
        Long id = sensitiveRuleAppService.create(dto);
        return SingleResult.of(id);
    }

    @ApiOperation("更新敏感识别规则")
    @PutMapping("/{id}")
    public SingleResult<Boolean> updateRule(@PathVariable("id") Long id, @Valid @RequestBody SensitiveRuleUpdateDTO dto) {
        sensitiveRuleAppService.update(id, dto);
        return SingleResult.of(true);
    }

    @ApiOperation("删除敏感识别规则")
    @DeleteMapping("/{id}")
    public SingleResult<Boolean> deleteRule(@PathVariable("id") Long id) {
        sensitiveRuleAppService.delete(id);
        return SingleResult.of(true);
    }

    @ApiOperation("启停敏感识别规则")
    @PutMapping("/{id}/status")
    public SingleResult<Boolean> changeRuleStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        sensitiveRuleAppService.updateStatus(id, status);
        return SingleResult.of(true);
    }

    @ApiOperation("克隆敏感识别规则")
    @PostMapping("/{id}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<Long> cloneRule(@PathVariable("id") Long id) {
        Long newId = sensitiveRuleAppService.cloneRule(id);
        return SingleResult.of(newId);
    }

    @ApiOperation("重置敏感识别规则（清空打标）")
    @PostMapping("/{id}/reset")
    public SingleResult<Boolean> resetRule(@PathVariable("id") Long id) {
        sensitiveRuleAppService.resetRule(id);
        return SingleResult.of(true);
    }

    @ApiOperation("在线模拟采样测试（只读不落库）")
    @PostMapping("/simulate")
    public MultiResult<SimulationFieldMatchVO> simulate(@Valid @RequestBody RuleSimulationRequestDTO dto) {
        List<SimulationFieldMatchVO> matches = sensitiveRuleAppService.simulate(dto);
        return MultiResult.of(matches);
    }
}
