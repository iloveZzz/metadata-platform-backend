package com.yss.datasecurity.domain.exception;

public class DataSecurityException extends RuntimeException {
    private final String code;

    public DataSecurityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getErrorCode() {
        return code;
    }
}
