package com.yss.metadata.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.ai.AskMetadataApplicationService;
import com.yss.metadata.application.ai.convertor.AskMetadataConvertor;
import com.yss.metadata.client.dto.cmd.AskMetadataCmd;
import com.yss.metadata.client.vo.AskMetadataVO;
import com.yss.metadata.domain.ai.model.AskMetadataSession;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * AI 自然语言智能找数控制器
 *
 * @author ai
 * @since 2026-08-15
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Api(tags = "ai-discovery")
public class AskMetadataController {

    private final AskMetadataApplicationService askMetadataApplicationService;
    private final AskMetadataConvertor askMetadataConvertor;

    /**
     * AI 自然语言找数
     */
    @PostMapping("/ask-metadata")
    @ApiOperation(value = "AI 自然语言智能找数", notes = "输入自然语言需求，语义理解意图并返回结构化匹配资产卡片")
    public SingleResult<AskMetadataVO> askMetadata(
            @Valid @RequestBody AskMetadataCmd cmd,
            @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        String currentUserId = CurrentUser.resolve(userId);
        AskMetadataSession session = askMetadataApplicationService.askMetadata(
                cmd.getQuery(),
                cmd.getDomain(),
                cmd.getLimit(),
                currentUserId);
        AskMetadataVO vo = askMetadataConvertor.toVO(session);
        return SingleResult.of(vo);
    }
}
