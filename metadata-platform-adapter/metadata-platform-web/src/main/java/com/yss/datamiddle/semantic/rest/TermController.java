package com.yss.datamiddle.semantic.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.semantic.application.service.TermQueryService;
import com.yss.datamiddle.semantic.application.service.TermService;
import com.yss.datamiddle.semantic.client.dto.cmd.TermCertifyCmd;
import com.yss.datamiddle.semantic.client.dto.cmd.TermCreateCmd;
import com.yss.datamiddle.semantic.client.dto.cmd.TermUpdateCmd;
import com.yss.datamiddle.semantic.client.vo.TermDetailVO;
import com.yss.datamiddle.semantic.client.vo.TermVO;
import com.yss.datamiddle.semantic.rest.convertor.TermWebConvertor;
import com.yss.datamiddle.semantic.term.exception.BusinessValidationException;
import com.yss.datamiddle.semantic.term.gateway.TermPage;
import com.yss.datamiddle.semantic.term.gateway.TermQuery;
import com.yss.datamiddle.semantic.term.model.Term;
import com.yss.datamiddle.semantic.term.model.TermStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
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

/**
 * 术语管理控制器（冻结契约 semantic-terms 端点）。
 */
@RestController
@RequestMapping("/api/semantic/terms")
@RequiredArgsConstructor
public class TermController {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE_INDEX = 1;

    private final TermService termService;
    private final TermQueryService termQueryService;
    private final TermWebConvertor termWebConvertor;

    /**
     * 新建术语（保存为草稿；名称重复 422 TERM_NAME_DUPLICATE）。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<TermVO> create(@Valid @RequestBody TermCreateCmd cmd) {
        Term term = termService.createTerm(termWebConvertor.toCreateInput(cmd));
        return SingleResult.of(termWebConvertor.toVO(term));
    }

    /**
     * 术语列表 / 筛选（关键词 / 状态 / 仅看已认证；分页 0 条以空分页表达）。
     */
    @GetMapping
    public PageResult<TermVO> page(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) Boolean onlyCertified) {
        int pageIndex = normalizePage(page);
        int pageSize = normalizeSize(size);
        TermQuery query = TermQuery.builder()
                .keyword(StringUtils.hasText(keyword) ? keyword.trim() : null)
                .status(normalizeStatus(status))
                .onlyCertified(onlyCertified)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
        TermPage result = termQueryService.pageTerms(query);
        return PageResult.of(termWebConvertor.toVOList(result.getList()),
                result.getTotalCount(), result.getPageSize(), result.getPageIndex());
    }

    /**
     * 术语详情（synonymSet / attachments 由切片 03/04 填充，本切片占位）。
     */
    @GetMapping("/{id}")
    public SingleResult<TermDetailVO> detail(@PathVariable Long id) {
        Term term = termQueryService.getById(id);
        return SingleResult.of(termWebConvertor.toDetailVO(term));
    }

    /**
     * 更新术语（乐观锁 version；过期 409 VERSION_CONFLICT 携带最新对象）。
     */
    @PutMapping("/{id}")
    public SingleResult<TermVO> update(@PathVariable Long id, @Valid @RequestBody TermUpdateCmd cmd) {
        Term term = termService.updateTerm(id, termWebConvertor.toUpdateInput(cmd));
        return SingleResult.of(termWebConvertor.toVO(term));
    }

    /**
     * 删除术语（仅草稿且未被引用可物理删除；否则 409 提示改用弃用）。
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        termService.deleteTerm(id);
    }

    /**
     * 认证 / 弃用（幂等返回当前状态；写审计）。
     */
    @PostMapping("/{id}/certify")
    public SingleResult<TermVO> certify(@PathVariable Long id, @Valid @RequestBody TermCertifyCmd cmd) {
        Term term = termService.certifyTerm(id, termWebConvertor.toCertifyInput(cmd));
        return SingleResult.of(termWebConvertor.toVO(term));
    }

    private int normalizePage(int page) {
        return page < DEFAULT_PAGE_INDEX ? DEFAULT_PAGE_INDEX : page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return TermStatus.fromCode(status.trim()).getCode();
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException("PARAM_VALIDATION_ERROR", "status", "INVALID_ENUM",
                    "状态筛选值非法，仅支持 draft / certified / deprecated");
        }
    }
}
