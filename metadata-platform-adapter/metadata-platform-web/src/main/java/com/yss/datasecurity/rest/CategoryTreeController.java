package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.CategoryTreeNodeCreateDTO;
import com.yss.datasecurity.application.dto.CategoryTreeNodeUpdateDTO;
import com.yss.datasecurity.application.dto.CategoryTreeNodeVO;
import com.yss.datasecurity.application.service.CategoryTreeAppService;
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

@Api(tags = "分类目录树管理")
@RestController
@RequestMapping("/api/v1/category-tree")
@RequiredArgsConstructor
@Validated
public class CategoryTreeController {

    private final CategoryTreeAppService categoryTreeAppService;

    @ApiOperation("查询完整分类目录树 (<= 10 级)")
    @GetMapping
    public MultiResult<CategoryTreeNodeVO> getCategoryTree() {
        List<CategoryTreeNodeVO> tree = categoryTreeAppService.getTree();
        return MultiResult.of(tree);
    }

    @ApiOperation("创建分类目录节点")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<Long> createCategoryTreeNode(@Valid @RequestBody CategoryTreeNodeCreateDTO dto) {
        Long id = categoryTreeAppService.createNode(dto);
        return SingleResult.of(id);
    }

    @ApiOperation("更新分类目录节点")
    @PutMapping("/{id}")
    public SingleResult<Boolean> updateCategoryTreeNode(@PathVariable("id") Long id, @Valid @RequestBody CategoryTreeNodeUpdateDTO dto) {
        categoryTreeAppService.updateNode(id, dto);
        return SingleResult.of(true);
    }

    @ApiOperation("级联删除分类目录节点及其分类")
    @DeleteMapping("/{id}")
    public SingleResult<Boolean> deleteCategoryTreeNode(@PathVariable("id") Long id) {
        categoryTreeAppService.deleteNode(id);
        return SingleResult.of(true);
    }
}
