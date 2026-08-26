package com.yss.datamiddle.semantic.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.semantic.application.model.SynonymSetCreateInput;
import com.yss.datamiddle.semantic.application.service.SynonymSetService;
import com.yss.datamiddle.semantic.client.dto.cmd.SynonymSetCreateCmd;
import com.yss.datamiddle.semantic.client.vo.SynonymSetVO;
import com.yss.datamiddle.semantic.synonym.model.SynonymSet;
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
import java.util.stream.Collectors;

/**
 * 同义词组 REST API 控制器（SL-003）。
 */
@RestController
@RequestMapping("/api/semantic/synonym-sets")
@RequiredArgsConstructor
@Validated
public class SynonymSetController {

    private final SynonymSetService synonymSetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<SynonymSetVO> create(@Valid @RequestBody SynonymSetCreateCmd cmd) {
        SynonymSetCreateInput input = SynonymSetCreateInput.builder()
                .name(cmd.getName())
                .canonical(cmd.getCanonical())
                .words(cmd.getWords())
                .termId(cmd.getTermId())
                .build();
        SynonymSet created = synonymSetService.create(input);
        return SingleResult.of(toVO(created));
    }

    @GetMapping
    public MultiResult<SynonymSetVO> list() {
        List<SynonymSetVO> list = synonymSetService.list().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return MultiResult.of(list);
    }

    @GetMapping("/{id}")
    public SingleResult<SynonymSetVO> getById(@PathVariable("id") Long id) {
        SynonymSet s = synonymSetService.getById(id);
        return SingleResult.of(toVO(s));
    }

    @PutMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleStatus(@PathVariable("id") Long id, @RequestParam("enabled") boolean enabled) {
        synonymSetService.toggleStatus(id, enabled);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        synonymSetService.delete(id);
    }

    private SynonymSetVO toVO(SynonymSet s) {
        return SynonymSetVO.builder()
                .id(s.getId())
                .name(s.getName())
                .canonical(s.getCanonical())
                .words(s.getWords())
                .termId(s.getTermId())
                .enabled(s.getEnabled())
                .version(s.getVersion())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
