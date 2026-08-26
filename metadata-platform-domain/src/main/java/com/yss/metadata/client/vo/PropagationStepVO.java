package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 故障传播链单步 VO
 *
 * @author ai
 * @since 2026-08-15
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropagationStepVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fromAssetId;
    private String fromAssetName;
    private String toAssetId;
    private String toAssetName;
    private String propagationType;
}
