package com.yss.datamiddle.smartgovernance.infrastructure.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.datamiddle.smartgovernance.domain.security.gateway.SecurityTemplateGateway;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityTemplate;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.mapper.SecurityTemplateMapper;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.po.SecurityTemplatePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SecurityTemplateGatewayImpl implements SecurityTemplateGateway {

    private final SecurityTemplateMapper templateMapper;

    @Override
    public List<SecurityTemplate> listTemplates(String keyword) {
        LambdaQueryWrapper<SecurityTemplatePO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(SecurityTemplatePO::getTemplateName, keyword.trim())
                    .or().like(SecurityTemplatePO::getTemplateCode, keyword.trim());
        }
        wrapper.orderByDesc(SecurityTemplatePO::getCreatedAt);
        return templateMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SecurityTemplate> findById(String id) {
        return Optional.ofNullable(templateMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<SecurityTemplate> findByCode(String templateCode) {
        LambdaQueryWrapper<SecurityTemplatePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityTemplatePO::getTemplateCode, templateCode);
        return Optional.ofNullable(templateMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public void save(SecurityTemplate template) {
        templateMapper.insert(toPO(template));
    }

    @Override
    public void update(SecurityTemplate template) {
        templateMapper.updateById(toPO(template));
    }

    private SecurityTemplate toDomain(SecurityTemplatePO po) {
        if (po == null) return null;
        return SecurityTemplate.builder()
                .id(po.getId())
                .templateCode(po.getTemplateCode())
                .templateName(po.getTemplateName())
                .standardAuthority(po.getStandardAuthority())
                .description(po.getDescription())
                .defaultAutoApproval(po.getDefaultAutoApproval() != null && po.getDefaultAutoApproval() == 1)
                .defaultThreshold(po.getDefaultThreshold())
                .isSystemBuiltIn(po.getIsSystemBuiltIn() != null && po.getIsSystemBuiltIn() == 1)
                .isActive(po.getIsActive() != null && po.getIsActive() == 1)
                .createdBy(po.getCreatedBy())
                .updatedBy(po.getUpdatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private SecurityTemplatePO toPO(SecurityTemplate d) {
        if (d == null) return null;
        return SecurityTemplatePO.builder()
                .id(d.getId())
                .templateCode(d.getTemplateCode())
                .templateName(d.getTemplateName())
                .standardAuthority(d.getStandardAuthority())
                .description(d.getDescription())
                .defaultAutoApproval(Boolean.TRUE.equals(d.getDefaultAutoApproval()) ? 1 : 0)
                .defaultThreshold(d.getDefaultThreshold())
                .isSystemBuiltIn(Boolean.TRUE.equals(d.getIsSystemBuiltIn()) ? 1 : 0)
                .isActive(Boolean.TRUE.equals(d.getIsActive()) ? 1 : 0)
                .createdBy(d.getCreatedBy())
                .updatedBy(d.getUpdatedBy())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
