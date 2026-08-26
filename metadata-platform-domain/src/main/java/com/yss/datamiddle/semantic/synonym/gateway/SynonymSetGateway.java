package com.yss.datamiddle.semantic.synonym.gateway;

import com.yss.datamiddle.semantic.synonym.model.SynonymSet;

import java.util.List;
import java.util.Optional;

public interface SynonymSetGateway {
    SynonymSet save(SynonymSet synonymSet);
    SynonymSet update(SynonymSet synonymSet);
    Optional<SynonymSet> findById(Long id);
    Optional<SynonymSet> findByName(String name);
    Optional<SynonymSet> findByCanonical(String canonical);
    List<SynonymSet> listAll();
    boolean delete(Long id);
}
