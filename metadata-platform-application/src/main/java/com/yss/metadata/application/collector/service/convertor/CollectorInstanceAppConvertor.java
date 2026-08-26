package com.yss.metadata.application.collector.service.convertor;

import com.yss.metadata.application.config.MapStructAppConfig;
import com.yss.metadata.client.vo.CollectorInstanceVO;
import com.yss.metadata.client.vo.MetadataDiffSummaryVO;
import com.yss.metadata.client.vo.WorkflowNodeVO;
import com.yss.metadata.domain.collector.model.CollectorInstance;
import com.yss.metadata.domain.collector.model.CollectorInstanceStatus;
import com.yss.metadata.domain.collector.model.ExecutionMode;
import com.yss.metadata.domain.collector.model.MetadataDiffSummary;
import com.yss.metadata.domain.collector.model.WorkflowNode;
import com.yss.metadata.domain.collector.model.WorkflowNodeType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 采集实例应用层对象转换器。
 */
@Mapper(config = MapStructAppConfig.class)
public interface CollectorInstanceAppConvertor {

    @Mapping(target = "status", expression = "java(toStatusCode(instance.getStatus()))")
    @Mapping(target = "statusDescription", expression = "java(toStatusDesc(instance.getStatus()))")
    @Mapping(target = "executionMode", expression = "java(toModeCode(instance.getExecutionMode()))")
    @Mapping(target = "executionModeDescription", expression = "java(toModeDesc(instance.getExecutionMode()))")
    CollectorInstanceVO toVO(CollectorInstance instance);

    List<CollectorInstanceVO> toVOList(List<CollectorInstance> instances);

    @Mapping(target = "type", expression = "java(toNodeTypeCode(node.getType()))")
    @Mapping(target = "typeDescription", expression = "java(toNodeTypeDesc(node.getType()))")
    @Mapping(target = "status", expression = "java(toStatusCode(node.getStatus()))")
    @Mapping(target = "statusDescription", expression = "java(toStatusDesc(node.getStatus()))")
    WorkflowNodeVO toNodeVO(WorkflowNode node);

    List<WorkflowNodeVO> toNodeVOList(List<WorkflowNode> nodes);

    MetadataDiffSummaryVO toDiffVO(MetadataDiffSummary summary);

    default String toStatusCode(CollectorInstanceStatus status) {
        return status == null ? null : status.getCode();
    }

    default String toStatusDesc(CollectorInstanceStatus status) {
        return status == null ? null : status.getDescription();
    }

    default String toModeCode(ExecutionMode mode) {
        return mode == null ? null : mode.getCode();
    }

    default String toModeDesc(ExecutionMode mode) {
        return mode == null ? null : mode.getDescription();
    }

    default String toNodeTypeCode(WorkflowNodeType type) {
        return type == null ? null : type.getCode();
    }

    default String toNodeTypeDesc(WorkflowNodeType type) {
        return type == null ? null : type.getDescription();
    }
}
