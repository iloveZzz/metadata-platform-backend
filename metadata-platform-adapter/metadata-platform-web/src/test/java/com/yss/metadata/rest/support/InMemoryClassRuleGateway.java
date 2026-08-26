package com.yss.metadata.rest.support;

import com.yss.metadata.domain.governance.gateway.ClassRuleGateway;
import com.yss.metadata.domain.governance.model.ClassRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分类规则仓储内存实现（Web 契约测试 seam，与 application 测试替身一致）。
 */
public class InMemoryClassRuleGateway implements ClassRuleGateway {

    private final Map<String, ClassRule> store = new LinkedHashMap<>();

    public void seed(ClassRule rule) {
        store.put(rule.getId(), rule);
    }

    @Override
    public List<ClassRule> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<ClassRule> findEnabled() {
        List<ClassRule> enabled = new ArrayList<>();
        for (ClassRule rule : store.values()) {
            if (Boolean.TRUE.equals(rule.getEnabled())) {
                enabled.add(rule);
            }
        }
        return enabled;
    }

    @Override
    public Optional<ClassRule> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public ClassRule save(ClassRule rule) {
        store.put(rule.getId(), rule);
        return rule;
    }
}
