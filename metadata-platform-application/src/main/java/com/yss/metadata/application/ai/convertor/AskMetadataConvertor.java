package com.yss.metadata.application.ai.convertor;

import com.yss.metadata.application.config.MapStructAppConfig;
import com.yss.metadata.client.vo.AskMetadataVO;
import com.yss.metadata.client.vo.MatchedAssetCardVO;
import com.yss.metadata.domain.ai.model.AskMetadataSession;
import com.yss.metadata.domain.ai.model.MatchedAssetCard;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * AI 智能找数对象转换器（MapStruct）
 *
 * @author ai
 * @since 2026-08-15
 */
@Mapper(config = MapStructAppConfig.class)
public interface AskMetadataConvertor {

    AskMetadataVO toVO(AskMetadataSession session);

    MatchedAssetCardVO toCardVO(MatchedAssetCard card);

    List<MatchedAssetCardVO> toCardVOList(List<MatchedAssetCard> cards);
}
