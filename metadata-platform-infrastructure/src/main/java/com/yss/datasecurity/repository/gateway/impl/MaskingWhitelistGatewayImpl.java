package com.yss.datasecurity.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datasecurity.domain.gateway.MaskingWhitelistGateway;
import com.yss.datasecurity.domain.model.MaskingWhitelist;
import com.yss.datasecurity.repository.entity.MaskingWhitelistPO;
import com.yss.datasecurity.repository.mapper.MaskingWhitelistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MaskingWhitelistGatewayImpl implements MaskingWhitelistGateway {

    private final MaskingWhitelistRepository repository;

    @Override
    public Long save(MaskingWhitelist whitelist) {
        MaskingWhitelistPO po = toPO(whitelist);
        repository.insert(po);
        whitelist.setId(po.getId());
        return po.getId();
    }

    @Override
    public void update(MaskingWhitelist whitelist) {
        MaskingWhitelistPO po = toPO(whitelist);
        repository.updateById(po);
    }

    @Override
    public Optional<MaskingWhitelist> findById(Long id) {
        MaskingWhitelistPO po = repository.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<MaskingWhitelist> findActiveByGrantee(String granteeType, String granteeId) {
        LambdaQueryWrapper<MaskingWhitelistPO> wrapper = new LambdaQueryWrapper<MaskingWhitelistPO>()
                .eq(MaskingWhitelistPO::getGranteeType, granteeType)
                .eq(MaskingWhitelistPO::getGranteeId, granteeId)
                .eq(MaskingWhitelistPO::getStatus, "ACTIVE")
                .le(MaskingWhitelistPO::getStartTime, LocalDateTime.now())
                .ge(MaskingWhitelistPO::getEndTime, LocalDateTime.now());
        return repository.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<MaskingWhitelist> findPage(int pageIndex, int pageSize, String status) {
        LambdaQueryWrapper<MaskingWhitelistPO> wrapper = new LambdaQueryWrapper<MaskingWhitelistPO>()
                .eq(StringUtils.hasText(status), MaskingWhitelistPO::getStatus, status)
                .orderByDesc(MaskingWhitelistPO::getId);
        Page<MaskingWhitelistPO> page = repository.selectPage(new Page<>(pageIndex, pageSize), wrapper);
        return page.getRecords().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countPage(String status) {
        LambdaQueryWrapper<MaskingWhitelistPO> wrapper = new LambdaQueryWrapper<MaskingWhitelistPO>()
                .eq(StringUtils.hasText(status), MaskingWhitelistPO::getStatus, status);
        return repository.selectCount(wrapper);
    }

    private MaskingWhitelistPO toPO(MaskingWhitelist d) {
        return MaskingWhitelistPO.builder()
                .id(d.getId())
                .whitelistName(d.getWhitelistName())
                .granteeType(d.getGranteeType())
                .granteeId(d.getGranteeId())
                .categoryId(d.getCategoryId())
                .ruleId(d.getRuleId())
                .startTime(d.getStartTime())
                .endTime(d.getEndTime())
                .status(d.getStatus())
                .reason(d.getReason())
                .createdBy(d.getCreatedBy())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private MaskingWhitelist toDomain(MaskingWhitelistPO po) {
        return MaskingWhitelist.builder()
                .id(po.getId())
                .whitelistName(po.getWhitelistName())
                .granteeType(po.getGranteeType())
                .granteeId(po.getGranteeId())
                .categoryId(po.getCategoryId())
                .ruleId(po.getRuleId())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .status(po.getStatus())
                .reason(po.getReason())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
