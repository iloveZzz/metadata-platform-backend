package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.domain.governance.gateway.ClassificationGateway;
import com.yss.metadata.domain.governance.model.Classification;
import com.yss.metadata.repository.AssetColumnRepository;
import com.yss.metadata.repository.ClassificationRepository;
import com.yss.metadata.infrastructure.convertor.ClassificationConvertor;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.entity.ClassificationPO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 分级分类结果仓储实现（MyBatis-Plus；classification 表）。
 *
 * <p>识别候选幂等：同 asset_id+column_id+name 已存在则跳过（重复采集不重复产候选）；
 * 列级分类源资产解析经 asset_column 反查父资产。</p>
 */
@Repository
public class ClassificationGatewayImpl implements ClassificationGateway {

    private final ClassificationRepository classificationRepository;
    private final AssetColumnRepository assetColumnRepository;
    private final ClassificationConvertor classificationConvertor;

    @Autowired
    public ClassificationGatewayImpl(ClassificationRepository classificationRepository,
                                     AssetColumnRepository assetColumnRepository) {
        this(classificationRepository, assetColumnRepository, Mappers.getMapper(ClassificationConvertor.class));
    }

    public ClassificationGatewayImpl(ClassificationRepository classificationRepository,
                                     AssetColumnRepository assetColumnRepository,
                                     ClassificationConvertor classificationConvertor) {
        this.classificationRepository = classificationRepository;
        this.assetColumnRepository = assetColumnRepository;
        this.classificationConvertor = classificationConvertor != null ? classificationConvertor : Mappers.getMapper(ClassificationConvertor.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Classification> findAll() {
        return classificationConvertor.toDomainList(
                classificationRepository.selectList(Wrappers.<ClassificationPO>lambdaQuery().orderByAsc(ClassificationPO::getId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Classification> findById(String id) {
        ClassificationPO po = classificationRepository.selectById(id);
        return po == null ? Optional.empty() : Optional.of(classificationConvertor.toDomain(po));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Classification save(Classification classification) {
        ClassificationPO po = classificationConvertor.toPO(classification);
        if (classificationRepository.selectById(po.getId()) != null) {
            classificationRepository.updateById(po);
        } else {
            classificationRepository.insert(po);
        }
        return classificationConvertor.toDomain(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCandidate(Classification candidate) {
        ClassificationPO existing = classificationRepository.selectOne(
                Wrappers.<ClassificationPO>lambdaQuery()
                        .eq(ClassificationPO::getAssetId, candidate.getAssetId())
                        .eq(ClassificationPO::getColumnId, candidate.getColumnId())
                        .eq(ClassificationPO::getName, candidate.getName())
                        .last("LIMIT 1"));
        if (existing != null) {
            return false;
        }
        classificationRepository.insert(classificationConvertor.toPO(candidate));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> resolveSourceAssetId(Classification classification) {
        if (classification.getAssetId() != null && !classification.getAssetId().trim().isEmpty()) {
            return Optional.of(classification.getAssetId());
        }
        if (classification.getColumnId() == null || classification.getColumnId().trim().isEmpty()) {
            return Optional.empty();
        }
        AssetColumnPO column = assetColumnRepository.selectById(classification.getColumnId());
        if (column == null || column.getAssetId() == null) {
            return Optional.empty();
        }
        return Optional.of(column.getAssetId());
    }
}
