package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.SensitiveRecordCalibrateDTO;
import com.yss.datasecurity.application.dto.SensitiveRecordVO;
import com.yss.datasecurity.application.service.SensitiveRecordAppService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "敏感识别打标记录与人机校准")
@RestController
@RequestMapping("/api/v1/sensitive-records")
@RequiredArgsConstructor
@Validated
public class SensitiveRecordController {

    private final SensitiveRecordAppService sensitiveRecordAppService;

    @ApiOperation("分页查询敏感识别打标记录")
    @GetMapping
    public PageResult<SensitiveRecordVO> pageRecords(
            @RequestParam(name = "pageIndex", defaultValue = "1") int pageIndex,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "securityGradeId", required = false) Long securityGradeId,
            @RequestParam(name = "isLocked", required = false) Boolean isLocked,
            @RequestParam(name = "datasourceId", required = false) String datasourceId) {
        return sensitiveRecordAppService.pageRecords(pageIndex, pageSize, keyword, categoryId, securityGradeId, isLocked, datasourceId);
    }

    @ApiOperation("人工校准覆盖打标并永久锁定 (MANUAL)")
    @PutMapping
    public com.yss.cloud.dto.result.SingleResult<Boolean> calibrateRecord(@Valid @RequestBody SensitiveRecordCalibrateDTO dto) {
        sensitiveRecordAppService.calibrateRecord(dto);
        return com.yss.cloud.dto.result.SingleResult.of(true);
    }
}
