package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.MaskEvaluationResponseVO;
import com.yss.datasecurity.application.dto.MaskQueryEvaluationRequestDTO;
import com.yss.datasecurity.application.service.MaskingRuleAppService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "脱敏计算评估引擎")
@RestController
@RequestMapping("/api/v1/masking-engine")
@RequiredArgsConstructor
@Validated
public class MaskingEngineController {

    private final MaskingRuleAppService maskingRuleAppService;

    @ApiOperation("执行动态查询脱敏运算评估")
    @PostMapping("/mask-query")
    public SingleResult<MaskEvaluationResponseVO> evaluateMaskQuery(
            @Valid @RequestBody MaskQueryEvaluationRequestDTO dto) {
        MaskEvaluationResponseVO response = maskingRuleAppService.evaluateMaskQuery(dto);
        return SingleResult.of(response);
    }
}
