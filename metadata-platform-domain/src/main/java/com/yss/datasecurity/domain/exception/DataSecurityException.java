package com.yss.datasecurity.domain.exception;

public class DataSecurityException extends RuntimeException {
    private final String code;

    public DataSecurityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public DataSecurityException(DataSecurityErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.code = errorCode.getCode();
    }

    public DataSecurityException(DataSecurityErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.getDefaultMessage());
        this.code = errorCode.getCode();
    }

    public String getCode() {
        return code;
    }

    public String getErrorCode() {
        return code;
    }
}
