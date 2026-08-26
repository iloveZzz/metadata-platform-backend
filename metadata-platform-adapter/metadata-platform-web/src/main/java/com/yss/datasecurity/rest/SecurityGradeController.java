package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.SecurityGradeCreateDTO;
import com.yss.datasecurity.application.dto.SecurityGradeUpdateDTO;
import com.yss.datasecurity.application.dto.SecurityGradeVO;
import com.yss.datasecurity.application.service.SecurityGradeAppService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "数据安全分级管理")
@RestController
@RequestMapping("/api/v1/security-grades")
@RequiredArgsConstructor
@Validated
public class SecurityGradeController {

    private final SecurityGradeAppService securityGradeAppService;

    @ApiOperation("查询数据分级列表")
    @GetMapping
    public MultiResult<SecurityGradeVO> listSecurityGrades() {
        List<SecurityGradeVO> list = securityGradeAppService.listAll();
        return MultiResult.of(list);
    }

    @ApiOperation("查询数据分级详情")
    @GetMapping("/{id}")
    public SingleResult<SecurityGradeVO> getSecurityGradeDetail(@PathVariable("id") Long id) {
        SecurityGradeVO vo = securityGradeAppService.getDetail(id);
        return SingleResult.of(vo);
    }

    @ApiOperation("创建数据分级")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<Long> createSecurityGrade(@Valid @RequestBody SecurityGradeCreateDTO dto) {
        Long id = securityGradeAppService.create(dto);
        return SingleResult.of(id);
    }

    @ApiOperation("更新数据分级")
    @PutMapping("/{id}")
    public SingleResult<Boolean> updateSecurityGrade(@PathVariable("id") Long id, @Valid @RequestBody SecurityGradeUpdateDTO dto) {
        securityGradeAppService.update(id, dto);
        return SingleResult.of(true);
    }

    @ApiOperation("删除数据分级（强引用删除拦截）")
    @DeleteMapping("/{id}")
    public SingleResult<Boolean> deleteSecurityGrade(@PathVariable("id") Long id) {
        securityGradeAppService.delete(id);
        return SingleResult.of(true);
    }
}
