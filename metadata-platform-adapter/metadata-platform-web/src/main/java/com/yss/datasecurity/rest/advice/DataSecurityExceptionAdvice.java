package com.yss.datasecurity.rest.advice;

import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.exception.SecurityGradeReferenceConflictException;
import com.yss.datasecurity.rest.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataSecurityExceptionAdvice {

    @ExceptionHandler(SecurityGradeReferenceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResult handleConflictException(SecurityGradeReferenceConflictException ex) {
        log.warn("业务冲突异常: {}", ex.getMessage());
        return ErrorResult.of(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(DataSecurityException.class)
    public org.springframework.http.ResponseEntity<ErrorResult> handleDataSecurityException(DataSecurityException ex) {
        log.warn("数据安全业务异常: [{}] {}", ex.getCode(), ex.getMessage());
        HttpStatus status = "KEY_IN_USE".equals(ex.getCode()) || "GRADE_REFERENCE_CONFLICT".equals(ex.getCode())
            ? HttpStatus.CONFLICT
            : HttpStatus.BAD_REQUEST;
        return org.springframework.http.ResponseEntity.status(status).body(ErrorResult.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResult handleValidationException(Exception ex) {
        ErrorResult result = ErrorResult.of("PARAM_VALIDATION_ERROR", "参数校验失败");
        if (ex instanceof MethodArgumentNotValidException) {
            for (FieldError fe : ((MethodArgumentNotValidException) ex).getBindingResult().getFieldErrors()) {
                result.addFieldError(fe.getField(), fe.getCode(), fe.getDefaultMessage(), "error");
            }
        } else if (ex instanceof BindException) {
            for (FieldError fe : ((BindException) ex).getFieldErrors()) {
                result.addFieldError(fe.getField(), fe.getCode(), fe.getDefaultMessage(), "error");
            }
        }
        return result;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResult handleGenericException(Exception ex) {
        log.error("系统未知异常", ex);
        return ErrorResult.of("INTERNAL_SERVER_ERROR", "系统内部错误: " + ex.getMessage());
    }
}
