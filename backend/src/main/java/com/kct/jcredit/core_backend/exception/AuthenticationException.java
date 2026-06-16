package com.kct.jcredit.core_backend.exception;

import com.kct.jcredit.core_backend.constant.ResponseConstants;

/**
 * Thrown when authentication fails (invalid credentials, expired token, etc.).
 */
public class AuthenticationException extends CustomException {

    public AuthenticationException(String message) {
        super(ResponseConstants.UNAUTHORIZED, message);
    }

    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
