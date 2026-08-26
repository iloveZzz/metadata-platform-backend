package com.yss.smartdiscovery.infrastructure.persistence;

import com.yss.smartdiscovery.domain.gateway.TagRepository;
import com.yss.smartdiscovery.domain.rule.TagRule;
import com.yss.smartdiscovery.domain.tag.SmartTagDefinition;
import com.yss.smartdiscovery.domain.tag.TagCategory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryTagRepository implements TagRepository {

    private final Map<String, SmartTagDefinition> tagMap = new ConcurrentHashMap<>();
    private final List<TagCategory> categoryList = new ArrayList<>();

    public InMemoryTagRepository() {
        initDefaultData();
    }

    private void initDefaultData() {
        categoryList.add(TagCategory.builder().id("CAT_DOMAIN").categoryName("业务主题分类").categoryCode("DOMAIN").sortOrder(1).build());
        categoryList.add(TagCategory.builder().id("CAT_SEC").categoryName("安全合规分级").categoryCode("SECURITY").sortOrder(2).build());
        categoryList.add(TagCategory.builder().id("CAT_LIFE").categoryName("数据生命周期").categoryCode("LIFECYCLE").sortOrder(3).build());

        SmartTagDefinition tag1 = SmartTagDefinition.builder()
                .id("TAG-01")
                .tagName("L4 核心敏感数据")
                .tagCode("SEC_L4")
                .categoryCode("SECURITY")
                .categoryName("安全合规分级")
                .colorToken("red")
                .description("国家法律法规明确规定的个人隐私与金融核心数据")
                .isEnabled(true)
                .tagRule(TagRule.builder()
                        .regexPattern("^(cust_id|id_card|identity_no|mobile_phone|bank_card_no|passwd)")
                        .boundTermNames(Arrays.asList("客户身份证", "银行卡号", "手机号", "用户密码"))
                        .fewShotPrompt("判断字段是否属于身份证、银行卡等个人敏感隐私。")
                        .build())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        SmartTagDefinition tag2 = SmartTagDefinition.builder()
                .id("TAG-02")
                .tagName("零售金融交易域")
                .tagCode("BIZ_FINANCE_TRADE")
                .categoryCode("DOMAIN")
                .categoryName("业务主题分类")
                .colorToken("blue")
                .description("零售金融线订单与清算交易流水")
                .isEnabled(true)
                .tagRule(TagRule.builder()
                        .regexPattern("^(trade_|order_|trans_|settle_)")
                        .boundTermNames(Arrays.asList("实际成交额", "订单明细", "清算流水"))
                        .fewShotPrompt("识别属于零售金融或支付结算业务线的交易事实。")
                        .build())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tagMap.put(tag1.getId(), tag1);
        tagMap.put(tag2.getId(), tag2);
    }

    @Override
    public List<TagCategory> listCategories() {
        return new ArrayList<>(categoryList);
    }

    @Override
    public List<SmartTagDefinition> listTags(String categoryCode) {
        if (categoryCode == null || categoryCode.trim().isEmpty() || "ALL".equalsIgnoreCase(categoryCode)) {
            return new ArrayList<>(tagMap.values());
        }
        return tagMap.values().stream()
                .filter(t -> categoryCode.equalsIgnoreCase(t.getCategoryCode()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SmartTagDefinition> findTagById(String id) {
        return Optional.ofNullable(tagMap.get(id));
    }

    @Override
    public Optional<SmartTagDefinition> findTagByCode(String code) {
        return tagMap.values().stream()
                .filter(t -> t.getTagCode().equalsIgnoreCase(code))
                .findFirst();
    }

    @Override
    public SmartTagDefinition saveTag(SmartTagDefinition tag) {
        tagMap.put(tag.getId(), tag);
        return tag;
    }

    @Override
    public void deleteTag(String id) {
        tagMap.remove(id);
    }
}
