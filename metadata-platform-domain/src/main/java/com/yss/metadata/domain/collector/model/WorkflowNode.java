package com.yss.metadata.domain.collector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流节点实体。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 节点 ID */
    private String id;

    /** 节点名称 */
    private String name;

    /** 节点类型 */
    private WorkflowNodeType type;

    /** 节点状态 */
    private CollectorInstanceStatus status;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 耗时 (毫秒) */
    private Long durationMs;

    /** 日志流列表 */
    @Builder.Default
    private List<String> logs = new ArrayList<>();

    /** 异常堆栈信息 */
    private String exceptionInfo;

    /** 性能诊断指标 (如阶段耗时、吞吐量、内存监控等) */
    @Builder.Default
    private Map<String, Object> performanceMetrics = new HashMap<>();

    /** 节点执行参数 */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    /** 智能排障与诊断建议 */
    @Builder.Default
    private Map<String, Object> diagnosisAdvice = new HashMap<>();

    /** 运行代码 (如 SQL / Flink 脚本) */
    private String executedCode;

    /**
     * 重跑节点
     */
    public void rerun() {
        this.status = CollectorInstanceStatus.RUNNING;
        this.startTime = LocalDateTime.now();
        this.endTime = null;
        this.durationMs = null;
        this.exceptionInfo = null;
        if (this.logs == null) {
            this.logs = new ArrayList<>();
        }
        this.logs.add("[INFO] " + LocalDateTime.now() + " 节点重跑触发...");
    }
}
