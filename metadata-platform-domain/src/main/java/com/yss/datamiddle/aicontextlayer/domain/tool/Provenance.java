package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资产 / 分类溯源元数据（SEC-08 / 断言 7）。
 *
 * <p>由服务端依据主平台实际响应生成。assetId 与 classificationId 互斥。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Provenance implements Serializable {
    private static final long serialVersionUID = 1L;

    private String assetId;
    private String classificationId;
    private String source;
    private LocalDateTime updatedAt;
    private LocalDateTime fetchedAt;
}
