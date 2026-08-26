package com.yss.metadata.rest.advice;

import com.yss.metadata.domain.asset.exception.AssetClaimConflictException;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.exception.AssetStateConflictException;
import com.yss.metadata.domain.collector.exception.CollectorInstanceNotFoundException;
import com.yss.metadata.domain.collector.exception.CollectorInstanceStateConflictException;
import com.yss.metadata.domain.collector.exception.CollectorTaskConflictException;
import com.yss.metadata.domain.collector.exception.CollectorTaskNotFoundException;
import com.yss.metadata.domain.collector.exception.CollectorTaskStateConflictException;
import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.exception.ConnectTestException;
import com.yss.metadata.domain.connector.exception.ConnectorNameConflictException;
import com.yss.metadata.domain.connector.exception.ConnectorNotFoundException;
import com.yss.metadata.domain.connector.exception.ConnectorReferencedException;
import com.yss.metadata.domain.governance.exception.ClassificationNotFoundException;
import com.yss.metadata.domain.governance.exception.ClassificationStateConflictException;
import com.yss.metadata.domain.governance.exception.ClassRuleNotFoundException;
import com.yss.metadata.domain.lineage.exception.LineageConflictException;
import com.yss.metadata.domain.lineage.exception.LineageCycleException;
import com.yss.metadata.domain.rbac.exception.ForbiddenException;
import com.yss.metadata.domain.rbac.exception.RoleNameConflictException;
import com.yss.metadata.domain.rbac.exception.RoleReferencedException;
import com.yss.metadata.rest.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 元数据平台全局异常处理（Web 层）。
 *
 * <p>将领域异常映射为冻结 OpenAPI Error 结构（code/message/severity/fieldErrors）：
 * 连接器不存在 404、名称冲突 409、连接测试失败与参数校验 422。
 * 优先级高于组件默认异常处理（@Order 最高）。</p>
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MetadataGlobalExceptionHandler {

    @ExceptionHandler(ConnectorNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResult handleConnectorNotFound(ConnectorNotFoundException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ConnectorNameConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleConnectorNameConflict(ConnectorNameConflictException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ConnectorReferencedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleConnectorReferenced(ConnectorReferencedException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(CollectorInstanceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResult handleCollectorInstanceNotFound(CollectorInstanceNotFoundException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(CollectorInstanceStateConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleCollectorInstanceStateConflict(CollectorInstanceStateConflictException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(CollectorTaskNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResult handleCollectorTaskNotFound(CollectorTaskNotFoundException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(CollectorTaskStateConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleCollectorTaskStateConflict(CollectorTaskStateConflictException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(CollectorTaskConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleCollectorTaskConflict(CollectorTaskConflictException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(AssetNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResult handleAssetNotFound(AssetNotFoundException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(AssetClaimConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleAssetClaimConflict(AssetClaimConflictException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(AssetStateConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleAssetStateConflict(AssetStateConflictException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(LineageCycleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleLineageCycle(LineageCycleException e) {
        // 环冲突（CYCLE）：定位冲突边（fieldErrors 携带）
        ErrorResult error = ErrorResult.of(e.getErrCode(), e.getMessage());
        error.addFieldError("fromAssetId", "lineage.cycle.edge",
                "冲突边：" + e.getConflictEdge().describe(), "error");
        return error;
    }

    @ExceptionHandler(LineageConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleLineageConflict(LineageConflictException e) {
        // 图版本冲突（CONFLICT）：恢复路径=重读图谱获取最新 graphVersionToken
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ClassRuleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResult handleClassRuleNotFound(ClassRuleNotFoundException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ClassificationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResult handleClassificationNotFound(ClassificationNotFoundException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ClassificationStateConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleClassificationStateConflict(ClassificationStateConflictException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(RoleNameConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleRoleNameConflict(RoleNameConflictException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(RoleReferencedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleRoleReferenced(RoleReferencedException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResult handleForbidden(ForbiddenException e) {
        return ErrorResult.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResult handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数不合法: {}", e.getMessage());
        return ErrorResult.of("asset.param.invalid", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResult handleIllegalState(IllegalStateException e) {
        log.error("服务调用或业务状态异常: {}", e.getMessage());
        return ErrorResult.of("remote.service.error", e.getMessage());
    }

    @ExceptionHandler(ConnectTestException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResult handleConnectTest(ConnectTestException e) {
        ErrorResult error = ErrorResult.of(e.getErrCode(), e.getMessage());
        if (e.getErrorType() == ConnectErrorType.CREDENTIAL) {
            error.addFieldError("password", "err.credential.invalid", e.getMessage(), "error");
        }
        return error;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResult handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        ErrorResult error = ErrorResult.of("param.invalid", "请求参数校验失败");
        e.getBindingResult().getFieldErrors().forEach(fieldError ->
                error.addFieldError(fieldError.getField(), fieldError.getCode(),
                        fieldError.getDefaultMessage(), "error"));
        return error;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResult handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ErrorResult.of("param.invalid", "请求体格式不正确或枚举值非法");
    }
}
