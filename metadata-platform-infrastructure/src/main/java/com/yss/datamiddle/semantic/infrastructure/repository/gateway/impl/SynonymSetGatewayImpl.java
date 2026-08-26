package com.yss.datamiddle.semantic.infrastructure.repository.gateway.impl;

import com.yss.datamiddle.semantic.synonym.gateway.SynonymSetGateway;
import com.yss.datamiddle.semantic.synonym.model.SynonymSet;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 同义词组持久化网关实现（SL-003）。
 */
@Repository
public class SynonymSetGatewayImpl implements SynonymSetGateway {

    private final Map<Long, SynonymSet> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(2000);

    @Override
    public SynonymSet save(SynonymSet synonymSet) {
        if (synonymSet.getId() == null) {
            synonymSet.setId(idGen.incrementAndGet());
        }
        storage.put(synonymSet.getId(), synonymSet);
        return synonymSet;
    }

    @Override
    public SynonymSet update(SynonymSet synonymSet) {
        storage.put(synonymSet.getId(), synonymSet);
        return synonymSet;
    }

    @Override
    public Optional<SynonymSet> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<SynonymSet> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(s -> name.equalsIgnoreCase(s.getName()))
                .findFirst();
    }

    @Override
    public Optional<SynonymSet> findByCanonical(String canonical) {
        if (canonical == null) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(s -> canonical.equalsIgnoreCase(s.getCanonical()))
                .findFirst();
    }

    @Override
    public List<SynonymSet> listAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean delete(Long id) {
        return storage.remove(id) != null;
    }
}
