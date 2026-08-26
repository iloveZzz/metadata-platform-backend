package com.yss.metadata.application.asset.service.impl;

import com.yss.metadata.application.asset.service.AssetQueryService;
import com.yss.metadata.application.asset.service.convertor.AssetAppConvertor;
import com.yss.metadata.client.dto.query.AssetSearchQuery;
import com.yss.metadata.client.vo.AssetDetailVO;
import com.yss.metadata.client.vo.AssetPageVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.asset.gateway.SearchIndex;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetSearchResult;
import com.yss.metadata.domain.asset.model.AssetVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 资产查询应用服务实现（WU-02-01 / WU-02-03）。
 *
 * <p>搜索编排：SearchIndex 端口返回分页命中（关系库 LIKE，可替换 seam）→
 * VO 组装；详情聚合：目录上下文 AssetRepository 读取资产 + 字段 + 版本 +
 * 标签 + 收藏状态。当前用户上下文 seam：收藏状态依赖传入的 currentUserId
 * （RBAC slice 06 前由 Web 层从 X-User-Id 解析，缺省 default-user）。</p>
 */
@Service
@RequiredArgsConstructor
public class AssetQueryServiceImpl implements AssetQueryService {

    private final SearchIndex searchIndex;
    private final AssetRepository assetRepository;
    private final AssetAppConvertor assetAppConvertor;

    @Override
    @Transactional(readOnly = true)
    public AssetPageVO search(AssetSearchQuery query) {
        AssetSearchResult result = searchIndex.search(query);
        return assetAppConvertor.toPageVO(result);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetDetailVO getDetail(String id, String currentUserId) {
        Asset asset = requireById(id);
        boolean favorite = assetRepository.isFavorite(id, currentUserId);
        List<String> tags = assetRepository.findTags(id);
        List<AssetColumn> columns = assetRepository.findColumns(id);
        List<AssetVersion> versions = assetRepository.findVersions(id);

        AssetDetailVO detail = assetAppConvertor.toDetailVO(asset);
        detail.setFavorite(favorite);
        detail.setTags(tags);
        detail.setColumns(assetAppConvertor.toColumnVOList(columns));
        detail.setVersions(assetAppConvertor.toVersionVOList(versions));
        return detail;
    }

    private Asset requireById(String id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(id));
    }
}
