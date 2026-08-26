package com.yss.smartdiscovery.domain.tag;

import com.yss.smartdiscovery.domain.rule.TagRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartTagDefinition {
    private String id;
    private String tagName;
    private String tagCode;
    private String categoryCode;
    private String categoryName;
    private String colorToken;
    private String description;
    private Boolean isEnabled;
    private TagRule tagRule;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        if (tagCode == null || tagCode.trim().isEmpty()) {
            throw new IllegalArgumentException("标签编码不能为空");
        }
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("所属分类编码不能为空");
        }
    }
}
