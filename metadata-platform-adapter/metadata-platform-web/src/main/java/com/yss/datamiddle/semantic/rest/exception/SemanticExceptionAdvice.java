package com.yss.datamiddle.semantic.rest.exception;

import com.yss.datamiddle.semantic.rest.convertor.TermWebConvertor;
import com.yss.datamiddle.semantic.term.exception.BusinessValidationException;
import com.yss.datamiddle.semantic.term.exception.PermissionDeniedException;
import com.yss.datamiddle.semantic.term.exception.ReferenceConflictException;
import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import com.yss.datamiddle.semantic.term.exception.TermNotFoundException;
import com.yss.datamiddle.semantic.term.exception.VersionConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 语义层统一异常映射（冻结契约 4xx 语义：404 / 409 / 422 / 403）。
 *
 * <p>覆盖 yss-component-exception GlobalExceptionAdvice 的 400 默认映射：本类按
 * 异常类型精确匹配（更具体），命中专属异常时按冻结契约状态码返回。</p>
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class SemanticExceptionAdvice {

    private static final String SEVERITY_ERROR = "error";

    private final TermWebConvertor termWebConvertor;

    @ExceptionHandler(TermNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(TermNotFoundException e) {
        return ErrorResponse.of("NOT_FOUND", e.getMessage(), SEVERITY_ERROR);
    }

    @ExceptionHandler(BusinessValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleBusinessValidation(BusinessValidationException e) {
        return ErrorResponse.of(e.getCode(), e.getMessage(), SEVERITY_ERROR)
                .addFieldError(new FieldErrorItem(e.getField(), e.getFieldCode(),
                        e.getFieldMessage(), SEVERITY_ERROR));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        ErrorResponse response = ErrorResponse.of("PARAM_VALIDATION_ERROR", "参数校验失败", SEVERITY_ERROR);
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            response.addFieldError(new FieldErrorItem(fieldError.getField(), "REQUIRED",
                    fieldError.getDefaultMessage(), SEVERITY_ERROR));
        }
        return response;
    }

    @ExceptionHandler(VersionConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleVersionConflict(VersionConflictException e) {
        ErrorResponse response = ErrorResponse.of("VERSION_CONFLICT", e.getMessage(), SEVERITY_ERROR);
        response.setData(termWebConvertor.toVO(e.getLatest()));
        return response;
    }

    @ExceptionHandler(StateConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleStateConflict(StateConflictException e) {
        return ErrorResponse.of("STATE_CONFLICT", e.getMessage(), SEVERITY_ERROR);
    }

    @ExceptionHandler(ReferenceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleReferenceConflict(ReferenceConflictException e) {
        return ErrorResponse.of("REFERENCE_CONFLICT", e.getMessage(), SEVERITY_ERROR);
    }

    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handlePermissionDenied(PermissionDeniedException e) {
        return ErrorResponse.of("PERMISSION_DENIED", e.getMessage(), SEVERITY_ERROR);
    }
}
