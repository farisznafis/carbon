package com.kct.jcredit.core_backend.util;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.kct.jcredit.core_backend.constant.ResponseConstants;

@Component
public class ResponseUtil {
    
    public static HttpStatus getHttpStatus(String status) {
        switch (status) {
            case ResponseConstants.OK:
                return HttpStatus.OK;
            case ResponseConstants.NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case ResponseConstants.BAD_REQUEST:
                return HttpStatus.BAD_REQUEST;
            case ResponseConstants.VALIDATION_ERROR:
            case ResponseConstants.FILE_TOO_LARGE:
            case ResponseConstants.UNSUPPORTED_FILE_TYPE:
                return HttpStatus.BAD_REQUEST;
            case ResponseConstants.CONFLICT:
            case ResponseConstants.PROJECT_NUMBER_ALREADY_USED:
            case ResponseConstants.METHOD_NUMBER_ALREADY_USED:
                return HttpStatus.CONFLICT;
            case ResponseConstants.PAYMENT_REQUIRED:
                return HttpStatus.PAYMENT_REQUIRED;
            case ResponseConstants.UNAUTHORIZED:
            case ResponseConstants.INVALID_CREDENTIALS:
                return HttpStatus.UNAUTHORIZED;
            case ResponseConstants.FORBIDDEN:
                return HttpStatus.FORBIDDEN;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
