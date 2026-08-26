package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.CategoryActiveFieldVO;
import com.yss.datasecurity.application.dto.CategoryBatchDeleteDTO;
import com.yss.datasecurity.application.dto.CategoryBatchGradeDTO;
import com.yss.datasecurity.application.dto.CategoryBatchMoveDTO;
import com.yss.datasecurity.application.dto.CategoryBatchStatusDTO;
import com.yss.datasecurity.application.dto.CategoryStatusChangeDTO;
import com.yss.datasecurity.application.dto.DataCategoryCreateDTO;
import com.yss.datasecurity.application.dto.DataCategoryUpdateDTO;
import com.yss.datasecurity.application.dto.DataCategoryVO;
import com.yss.datasecurity.application.service.DataCategoryAppService;
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

@Api(tags = "数据分类管理")
@RestController
@RequestMapping("/api/v1/data-categories")
@RequiredArgsConstructor
@Validated
public class DataCategoryController {

    private final DataCategoryAppService dataCategoryAppService;

    @ApiOperation("分页查询数据分类列表")
    @GetMapping
    public PageResult<DataCategoryVO> pageDataCategories(
            @RequestParam(name = "pageIndex", defaultValue = "1") int pageIndex,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "treeNodeId", required = false) Long treeNodeId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status) {
        return dataCategoryAppService.pageCategories(pageIndex, pageSize, treeNodeId, keyword, status);
    }

    @ApiOperation("全量导出数据分类列表")
    @GetMapping("/export")
    public MultiResult<DataCategoryVO> exportCategories(
            @RequestParam(name = "treeNodeId", required = false) Long treeNodeId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status) {
        List<DataCategoryVO> list = dataCategoryAppService.exportCategories(treeNodeId, keyword, status);
        return MultiResult.of(list);
    }

    @ApiOperation("查询数据分类详情")
    @GetMapping("/{id}")
    public SingleResult<DataCategoryVO> getDataCategoryDetail(@PathVariable("id") Long id) {
        DataCategoryVO vo = dataCategoryAppService.getDetail(id);
        return SingleResult.of(vo);
    }

    @ApiOperation("查询数据分类覆盖生效的表字段清单")
    @GetMapping("/{id}/active-fields")
    public MultiResult<CategoryActiveFieldVO> getActiveFields(@PathVariable("id") Long id) {
        List<CategoryActiveFieldVO> list = dataCategoryAppService.getActiveFields(id);
        return MultiResult.of(list);
    }

    @ApiOperation("创建数据分类")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<Long> createDataCategory(@Valid @RequestBody DataCategoryCreateDTO dto) {
        Long id = dataCategoryAppService.create(dto);
        return SingleResult.of(id);
    }

    @ApiOperation("更新数据分类")
    @PutMapping("/{id}")
    public SingleResult<Boolean> updateDataCategory(@PathVariable("id") Long id, @Valid @RequestBody DataCategoryUpdateDTO dto) {
        dataCategoryAppService.update(id, dto);
        return SingleResult.of(true);
    }

    @ApiOperation("删除数据分类")
    @DeleteMapping("/{id}")
    public SingleResult<Boolean> deleteDataCategory(@PathVariable("id") Long id) {
        dataCategoryAppService.delete(id);
        return SingleResult.of(true);
    }

    @ApiOperation("启停数据分类（支持保留打标 vs 同步删除策略）")
    @PutMapping("/{id}/status")
    public SingleResult<Boolean> changeDataCategoryStatus(@PathVariable("id") Long id, @Valid @RequestBody CategoryStatusChangeDTO dto) {
        dataCategoryAppService.changeStatus(id, dto);
        return SingleResult.of(true);
    }

    @ApiOperation("批量移动数据分类至指定目录")
    @PutMapping("/batch/move")
    public SingleResult<Boolean> batchMove(@Valid @RequestBody CategoryBatchMoveDTO dto) {
        dataCategoryAppService.batchMove(dto);
        return SingleResult.of(true);
    }

    @ApiOperation("批量为数据分类指定数据分级")
    @PutMapping("/batch/grade")
    public SingleResult<Boolean> batchGrade(@Valid @RequestBody CategoryBatchGradeDTO dto) {
        dataCategoryAppService.batchUpdateGrade(dto);
        return SingleResult.of(true);
    }

    @ApiOperation("批量启停数据分类")
    @PutMapping("/batch/status")
    public SingleResult<Boolean> batchStatus(@Valid @RequestBody CategoryBatchStatusDTO dto) {
        dataCategoryAppService.batchChangeStatus(dto);
        return SingleResult.of(true);
    }

    @ApiOperation("批量删除数据分类")
    @DeleteMapping("/batch")
    public SingleResult<Boolean> batchDelete(@Valid @RequestBody CategoryBatchDeleteDTO dto) {
        dataCategoryAppService.batchDelete(dto);
        return SingleResult.of(true);
    }
}
