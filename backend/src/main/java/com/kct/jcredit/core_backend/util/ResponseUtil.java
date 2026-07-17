package com.kct.jcredit.core_backend.util;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.kct.jcredit.core_backend.constant.ResponseConstants;

@Component
public class ResponseUtil {
    
    public static HttpStatus getHttpStatus(String status) {
        switch (status) {
            case ResponseConstants.OK:
            case ResponseConstants.OTP_REQUIRED:
                return HttpStatus.OK;
            case ResponseConstants.NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case ResponseConstants.BAD_REQUEST:
                return HttpStatus.BAD_REQUEST;
            case ResponseConstants.VALIDATION_ERROR:
            case ResponseConstants.SAME_PASSWORD:
            case ResponseConstants.INVALID_CURRENT_PASSWORD:
            case ResponseConstants.FILE_TOO_LARGE:
            case ResponseConstants.UNSUPPORTED_FILE_TYPE:
                return HttpStatus.BAD_REQUEST;
            case ResponseConstants.CONFLICT:
            case ResponseConstants.USED_EMAIL:
            case ResponseConstants.USED_USERNAME:
            case ResponseConstants.EMAIL_ALREADY_USED:
            case ResponseConstants.CORP_NUMBER_ALREADY_USED:
            case ResponseConstants.PROJECT_NUMBER_ALREADY_USED:
            case ResponseConstants.METHOD_NUMBER_ALREADY_USED:
                return HttpStatus.CONFLICT;
            case ResponseConstants.UNAUTHORIZED:
            case ResponseConstants.INVALID_CREDENTIALS:
                return HttpStatus.UNAUTHORIZED;
            case ResponseConstants.FORBIDDEN:
                return HttpStatus.FORBIDDEN;
            case ResponseConstants.ACCOUNT_LOCKED:
                return HttpStatus.LOCKED;
            case ResponseConstants.SERVICE_UNAVAILABLE:
                return HttpStatus.SERVICE_UNAVAILABLE;
            case ResponseConstants.TOO_MANY_REQUESTS:
                return HttpStatus.TOO_MANY_REQUESTS;
            case ResponseConstants.INVALID_OR_EXPIRED_TOKEN:
                return HttpStatus.UNAUTHORIZED;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
