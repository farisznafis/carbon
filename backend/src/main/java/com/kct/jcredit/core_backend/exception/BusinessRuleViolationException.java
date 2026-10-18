package com.kct.jcredit.core_backend.exception;

import com.kct.jcredit.core_backend.constant.ResponseConstants;

/**
 * Thrown when a business rule is violated (e.g., duplicate registration, invalid state transition).
 */
public class BusinessRuleViolationException extends CustomException {

    public BusinessRuleViolationException(String message) {
        super(ResponseConstants.BAD_REQUEST, message);
    }

    public BusinessRuleViolationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
