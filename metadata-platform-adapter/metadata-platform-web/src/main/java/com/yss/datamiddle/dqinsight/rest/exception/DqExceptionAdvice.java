package com.yss.datamiddle.dqinsight.rest.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.exception.AssetNotFoundException;
import com.yss.datamiddle.dqinsight.domain.exception.BatchDuplicateException;
import com.yss.datamiddle.dqinsight.domain.exception.BatchTooLargeException;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelBusyException;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelInUseException;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelNameConflictException;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelNotFoundException;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelValidationException;
import com.yss.datamiddle.dqinsight.domain.exception.DqForbiddenException;
import com.yss.datamiddle.dqinsight.domain.exception.HealthScoreNotFoundException;
import com.yss.datamiddle.dqinsight.domain.exception.IngestValidationException;
import com.yss.datamiddle.dqinsight.domain.exception.LinkageAlreadyLinkedException;
import com.yss.datamiddle.dqinsight.domain.exception.LinkageNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * DQ 统一异常处理（403 / 409 / 413 / 422 走统一错误体 code / message / severity / fieldErrors）。
 *
 * <p>本 Advice 置高优先级只处理 DQ 业务异常；其余异常由平台全局异常处理兜底。</p>
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DqExceptionAdvice {

    @ExceptionHandler(IngestValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponseVO handleIngestValidation(IngestValidationException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage(), e.getFieldErrors());
    }

    @ExceptionHandler(BatchDuplicateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseVO handleBatchDuplicate(BatchDuplicateException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(BatchTooLargeException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ErrorResponseVO handleBatchTooLarge(BatchTooLargeException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(DqForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponseVO handleForbidden(DqForbiddenException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(HealthScoreNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseVO handleHealthScoreNotFound(HealthScoreNotFoundException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ChannelNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseVO handleChannelNotFound(ChannelNotFoundException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(LinkageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseVO handleLinkageNotFound(LinkageNotFoundException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ChannelNameConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseVO handleChannelNameConflict(ChannelNameConflictException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ChannelBusyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseVO handleChannelBusy(ChannelBusyException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ChannelInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseVO handleChannelInUse(ChannelInUseException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(LinkageAlreadyLinkedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseVO handleLinkageAlreadyLinked(LinkageAlreadyLinkedException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(AssetNotFoundException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponseVO handleAssetNotFound(AssetNotFoundException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage());
    }

    @ExceptionHandler(ChannelValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponseVO handleChannelValidation(ChannelValidationException e) {
        return ErrorResponseVO.of(e.getErrCode(), e.getMessage(), e.getFieldErrors());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseVO handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突（幂等兜底）: {}", e.getMessage());
        return ErrorResponseVO.of(DqErrorCodes.BATCH_DUPLICATE, "批次已存在（幂等冲突）");
    }
}
