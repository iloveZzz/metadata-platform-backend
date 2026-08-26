package com.yss.datamiddle.smartgovernance.domain.security.gateway;

import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityTemplate;

import java.util.List;
import java.util.Optional;

public interface SecurityTemplateGateway {
    List<SecurityTemplate> listTemplates(String keyword);

    Optional<SecurityTemplate> findById(String id);

    Optional<SecurityTemplate> findByCode(String templateCode);

    void save(SecurityTemplate template);

    void update(SecurityTemplate template);
}
