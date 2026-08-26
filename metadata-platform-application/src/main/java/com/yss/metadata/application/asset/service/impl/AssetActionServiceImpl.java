package com.yss.metadata.application.asset.service.impl;

import com.yss.metadata.application.asset.service.AssetActionService;
import com.yss.metadata.application.asset.service.convertor.AssetAppConvertor;
import com.yss.metadata.client.vo.AssetVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.exception.AssetStateConflictException;
import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 资产操作用例应用服务实现（WU-02-02）。
 *
 * <p>用例边界（Domain/Application）：收藏幂等切换（已收藏→取消，未收藏→收藏，
 * 已删除资产阻断）；认领 owner 唯一（他人已认领 409）；标签覆盖式更新
 * （归一化后全量替换，归档/已删除阻断）；归档-取消归档只读状态机
 * （重复归档/已删除 409）。事务边界为单聚合事务。</p>
 *
 * <p>当前用户上下文 seam（RBAC slice 06 替换）：收藏/认领/我的资产使用
 * Web 层解析的请求头 X-User-Id（缺省 default-user）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetActionServiceImpl implements AssetActionService {

    /** 单标签长度上限（资产标签表 tag varchar(64)） */
    private static final int MAX_TAG_LENGTH = 64;

    /** 标签数量上限（对齐 AssetTagUpdateCmd 校验） */
    private static final int MAX_TAG_COUNT = 50;

    private final AssetRepository assetRepository;
    private final AssetAppConvertor assetAppConvertor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetVO toggleFavorite(String id, String currentUserId) {
        Asset asset = requireById(id);
        if (asset.getStatus() == AssetStatus.DELETED) {
            throw new AssetStateConflictException("已删除资产不可收藏");
        }
        boolean favorited = assetRepository.isFavorite(id, currentUserId);
        if (favorited) {
            assetRepository.removeFavorite(id, currentUserId);
            log.info("取消收藏成功，assetId={}, userId={}", id, currentUserId);
        } else {
            assetRepository.addFavorite(id, currentUserId);
            log.info("收藏成功，assetId={}, userId={}", id, currentUserId);
        }
        AssetVO vo = assetAppConvertor.toVO(asset);
        vo.setFavorite(!favorited);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetVO claim(String id, String currentUserId) {
        Asset asset = requireById(id);
        asset.claim(currentUserId);
        assetRepository.save(asset);
        log.info("认领成功，assetId={}, owner={}", id, asset.getOwner());
        return assetAppConvertor.toVO(asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetVO updateTags(String id, List<String> tags) {
        Asset asset = requireById(id);
        asset.ensureWritable("编辑标签");
        List<String> normalized = normalizeTags(tags);
        assetRepository.replaceTags(id, normalized);
        log.info("标签更新成功，assetId={}, tags={}", id, normalized);
        return assetAppConvertor.toVO(asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetVO archive(String id) {
        Asset asset = requireById(id);
        asset.archive();
        assetRepository.save(asset);
        log.info("归档成功，assetId={}", id);
        return assetAppConvertor.toVO(asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetVO unarchive(String id) {
        Asset asset = requireById(id);
        asset.unarchive();
        assetRepository.save(asset);
        log.info("取消归档成功，assetId={}, status={}", id, asset.getStatus().getValue());
        return assetAppConvertor.toVO(asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetVO exclude(String id) {
        Asset asset = requireById(id);
        asset.exclude();
        assetRepository.save(asset);
        log.info("剔除资产成功，assetId={}", id);
        return assetAppConvertor.toVO(asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetVO recover(String id) {
        Asset asset = requireById(id);
        asset.recover();
        assetRepository.save(asset);
        log.info("恢复资产成功，assetId={}", id);
        return assetAppConvertor.toVO(asset);
    }

    private Asset requireById(String id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(id));
    }

    /**
     * 标签归一化：trim、去空白、去重（保序）、长度/数量校验。
     */
    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) {
                continue;
            }
            String normalized = tag.trim();
            if (normalized.length() > MAX_TAG_LENGTH) {
                throw new IllegalArgumentException("单个标签长度不能超过 " + MAX_TAG_LENGTH + " 字符");
            }
            distinct.add(normalized);
        }
        if (distinct.size() > MAX_TAG_COUNT) {
            throw new IllegalArgumentException("标签数量不能超过 " + MAX_TAG_COUNT + " 个");
        }
        return new ArrayList<>(distinct);
    }
}
