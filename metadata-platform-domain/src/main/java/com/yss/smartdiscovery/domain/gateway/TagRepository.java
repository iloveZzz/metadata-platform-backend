package com.yss.smartdiscovery.domain.gateway;

import com.yss.smartdiscovery.domain.tag.SmartTagDefinition;
import com.yss.smartdiscovery.domain.tag.TagCategory;

import java.util.List;
import java.util.Optional;

public interface TagRepository {
    List<TagCategory> listCategories();
    List<SmartTagDefinition> listTags(String categoryCode);
    Optional<SmartTagDefinition> findTagById(String id);
    Optional<SmartTagDefinition> findTagByCode(String code);
    SmartTagDefinition saveTag(SmartTagDefinition tag);
    void deleteTag(String id);
}
