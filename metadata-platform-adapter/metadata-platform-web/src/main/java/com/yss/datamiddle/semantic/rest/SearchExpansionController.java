package com.yss.datamiddle.semantic.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.datamiddle.semantic.application.model.QueryExpansionResult;
import com.yss.datamiddle.semantic.application.service.SearchExpansionService;
import com.yss.datamiddle.semantic.client.dto.cmd.SearchExpandCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 检索同义词展开 REST API 控制器（SL-005 / SB-05）。
 */
@RestController
@RequestMapping("/api/semantic/search")
@RequiredArgsConstructor
@Validated
public class SearchExpansionController {

    private final SearchExpansionService searchExpansionService;

    @PostMapping("/expand")
    public MultiResult<QueryExpansionResult> expand(@Valid @RequestBody SearchExpandCmd cmd) {
        List<QueryExpansionResult> results = searchExpansionService.expand(cmd.getQueries());
        return MultiResult.of(results);
    }
}
