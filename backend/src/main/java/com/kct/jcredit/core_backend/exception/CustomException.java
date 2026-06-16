package com.kct.jcredit.core_backend.exception;

import lombok.Getter;
import java.util.Map;

@Getter
public class CustomException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, String> fields;
    private final Map<String, Object> details;

    public CustomException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.fields = null;
        this.details = null;
    }

    public CustomException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.fields = null;
        this.details = null;
    }

    public CustomException(String errorCode, String message, Map<String, String> fields, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.fields = fields;
        this.details = details;
    }
}
