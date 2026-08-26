package com.yss.metadata.domain.collector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 采集执行结果值对象。
 *
 * <p>成功时携带采集产物资产清单（经 AssetGateway 幂等入库 + 版本快照）；
 * 失败携带失败原因（局部重采语义字段）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectorExecutionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否执行成功 */
    private boolean success;

    /** 失败原因 */
    private String failReason;

    /** 采集产物资产清单（成功时携带，可空表示本次无资产变化） */
    private List<CollectedAsset> assets;

    public static CollectorExecutionResult success() {
        return success(null);
    }

    public static CollectorExecutionResult success(List<CollectedAsset> assets) {
        return CollectorExecutionResult.builder().success(true).assets(assets).build();
    }

    public static CollectorExecutionResult failure(String failReason) {
        return CollectorExecutionResult.builder().success(false).failReason(failReason).build();
    }
}
